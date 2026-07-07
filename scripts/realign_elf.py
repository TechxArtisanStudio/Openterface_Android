#!/usr/bin/env python3
"""
realign_elf.py - Realign ELF PT_LOAD segments to 16KB page boundaries.

Strategy:
1. Insert padding AFTER each non-final LOAD segment, shifting subsequent data forward.
2. Update program headers, section headers, .dynamic entries.
3. UPDATE RELA relocation entries' addends that point into shifted LOAD segments.
4. UPDATE symbol table entries (st_value) that point into shifted LOAD segments.
5. Sync PT_DYNAMIC.p_offset with .dynamic section's actual sh_offset.
"""

import argparse
import struct
import sys

# Tags in .dynamic that are virtual addresses
DT_ADDRESS_TAGS = frozenset({
    3, 5, 6, 7, 12, 13, 17, 23, 25, 26,
    0x6ffffff0, 0x6ffffffc, 0x6ffffffe,
})

# ELF64 relocation types that have an addend as a virtual address
R_AARCH64_RELATIVE = 0x403
R_AARCH64_IRELATIVE = 0x407


def get_delta_for_vaddr(vaddr, loads):
    """Compute the vaddr delta for a given vaddr based on which LOAD it's in."""
    for l in loads:
        if l['vaddr'] <= vaddr < l['vaddr'] + l['memsz']:
            return l.get('delta', 0)
    return 0


def main():
    parser = argparse.ArgumentParser(description="Realign ELF PT_LOAD segments")
    parser.add_argument("input_file", help="Path to ELF .so file")
    parser.add_argument("--page-size", type=int, default=16384, help="Target page size")
    args = parser.parse_args()

    path = args.input_file
    page_size = args.page_size

    with open(path, 'rb') as f:
        data = bytearray(f.read())

    if data[:4] != b'\x7fELF':
        print(f"Not an ELF file: {path}")
        return

    is64 = data[4] == 2
    if not is64:
        print(f"Only 64-bit ELF supported for now")
        return

    # Parse ELF header
    e_phoff = struct.unpack_from('<Q', data, 32)[0]
    e_shoff = struct.unpack_from('<Q', data, 40)[0]
    e_phentsize = struct.unpack_from('<H', data, 54)[0]
    e_phnum = struct.unpack_from('<H', data, 56)[0]
    e_shentsize = struct.unpack_from('<H', data, 58)[0]
    e_shnum = struct.unpack_from('<H', data, 60)[0]
    e_shstrndx = struct.unpack_from('<H', data, 62)[0]

    # Parse program headers
    phdrs = []
    for i in range(e_phnum):
        off = e_phoff + i * e_phentsize
        vals = struct.unpack_from('<IIQQQQQQ', data, off)
        p = {'type': vals[0], 'flags': vals[1], 'offset': vals[2],
             'vaddr': vals[3], 'paddr': vals[4], 'filesz': vals[5],
             'memsz': vals[6], 'align': vals[7], 'hdr_off': off}
        phdrs.append(p)

    loads = sorted([p for p in phdrs if p['type'] == 1], key=lambda p: p['offset'])

    if not loads:
        print("No PT_LOAD segments")
        return

    if all(l['offset'] % page_size == 0 and l['align'] >= page_size for l in loads):
        print("All PT_LOAD already aligned")
        return

    # Parse section headers
    shdrs = []
    for i in range(e_shnum):
        off = e_shoff + i * e_shentsize
        if off + e_shentsize > len(data):
            break
        sh_name, sh_type = struct.unpack_from('<II', data, off)
        sh_offset = struct.unpack_from('<Q', data, off + 24)[0]
        sh_size = struct.unpack_from('<Q', data, off + 32)[0]
        shdrs.append({'name_idx': sh_name, 'type': sh_type,
                      'offset': sh_offset, 'size': sh_size, 'idx': i})

    # Get section name string table
    shstrtab = b''
    if e_shstrndx < len(shdrs):
        strtab_off = shdrs[e_shstrndx]['offset']
        strtab_size = shdrs[e_shstrndx]['size']
        if strtab_off + strtab_size <= len(data):
            shstrtab = bytes(data[strtab_off:strtab_off + strtab_size])

    # Find .dynamic and symbol table sections
    dyn_section_info = None
    symtab_sections = []  # .dynsym, .symtab
    if shstrtab:
        for sh in shdrs:
            name_end = shstrtab.find(b'\x00', sh['name_idx'])
            if name_end >= 0:
                name = shstrtab[sh['name_idx']:name_end].decode('ascii', errors='replace')
                if name == '.dynamic':
                    dyn_section_info = {'offset': sh['offset'], 'size': sh['size']}
                elif name in ('.dynsym', '.symtab'):
                    symtab_sections.append({'name': name, 'offset': sh['offset'], 'size': sh['size']})

    # Build padding plan
    padding_points = []
    cumulative_pad = 0
    for i in range(len(loads) - 1):
        load_end = loads[i]['offset'] + loads[i]['filesz']
        next_load_offset = loads[i + 1]['offset']
        needed = (next_load_offset + cumulative_pad) % page_size
        pad = (page_size - needed) % page_size
        padding_points.append((load_end, pad))
        cumulative_pad += pad

    def get_new_offset(old_offset):
        shift = 0
        for pad_pos, pad_amt in padding_points:
            if old_offset >= pad_pos:
                shift += pad_amt
        return old_offset + shift

    def get_delta_for_offset(old_offset):
        shift = 0
        for pad_pos, pad_amt in padding_points:
            if old_offset >= pad_pos:
                shift += pad_amt
        return shift

    # Pre-compute LOAD deltas (for quick lookup by vaddr)
    for l in loads:
        l['delta'] = get_delta_for_offset(l['offset'])

    # Build new file: copy data with padding inserted
    new_data = bytearray()
    prev_end = 0
    for pad_pos, pad_amt in padding_points:
        new_data.extend(data[prev_end:pad_pos])
        if pad_amt > 0:
            new_data.extend(b'\x00' * pad_amt)
        prev_end = pad_pos
    new_data.extend(data[prev_end:])
    new_file_size = len(new_data)

    # --- UPDATE ALL 8-byte aligned values in shifted LOAD segments ---
    # These are internal data pointers (function pointers in init arrays, global
    # pointers, etc.) that were written at link-time.
    # MUST run BEFORE .dynamic update to avoid double-updating .dynamic entries.
    data_updates = 0
    for li, l in enumerate(loads):
        if li == 0:
            continue  # First LOAD doesn't shift
        if l['delta'] == 0:
            continue
        new_load_off = l['offset'] + l['delta']
        scan_size = l['filesz']
        for off_in_seg in range(0, scan_size - 7, 8):
            file_off = new_load_off + off_in_seg
            if file_off + 8 > new_file_size:
                break
            val = struct.unpack_from('<Q', new_data, file_off)[0]
            for other_l in loads:
                if other_l['delta'] != 0 and other_l['vaddr'] <= val < other_l['vaddr'] + other_l['memsz']:
                    new_val = val + other_l['delta']
                    struct.pack_into('<Q', new_data, file_off, new_val)
                    data_updates += 1
                    break

    # --- UPDATE .dynamic ENTRIES ---
    if dyn_section_info:
        dyn_new_offset = get_new_offset(dyn_section_info['offset'])
        dyn_entry_size = 16  # Elf64_Dyn
        num_entries = dyn_section_info['size'] // dyn_entry_size
        for entry_idx in range(num_entries):
            entry_off = dyn_new_offset + entry_idx * dyn_entry_size
            if entry_off + dyn_entry_size > new_file_size:
                break
            tag, val = struct.unpack_from('<qQ', new_data, entry_off)
            if tag == 0:
                break
            if tag in DT_ADDRESS_TAGS:
                delta = get_delta_for_vaddr(val, loads)
                if delta != 0:
                    struct.pack_into('<Q', new_data, entry_off + 8, val + delta)

    # --- UPDATE RELA entries' addends ---
    # Find DT_RELA and DT_JMPREL in .dynamic
    rela_info = None
    jmprel_info = None
    if dyn_section_info:
        dyn_new_offset = get_new_offset(dyn_section_info['offset'])
        dyn_entry_size = 16
        num_entries = dyn_section_info['size'] // dyn_entry_size
        rela_vaddr = jmprel_vaddr = rela_size = pltrel_size = None
        for entry_idx in range(num_entries):
            entry_off = dyn_new_offset + entry_idx * dyn_entry_size
            if entry_off + dyn_entry_size > new_file_size:
                break
            tag, val = struct.unpack_from('<qQ', new_data, entry_off)
            if tag == 0:
                break
            if tag == 7:   # DT_RELA
                rela_vaddr = val
            elif tag == 8: # DT_RELASZ
                rela_size = val
            elif tag == 23: # DT_JMPREL
                jmprel_vaddr = val
            elif tag == 2: # DT_PLTRELSZ
                pltrel_size = val

        # Find RELA file offset (in which LOAD is it?)
        if rela_vaddr is not None and rela_size is not None:
            rela_file_off = None
            for l in loads:
                if l['vaddr'] <= rela_vaddr < l['vaddr'] + l['memsz']:
                    rela_file_off = l['offset'] + (rela_vaddr - l['vaddr'])
                    break
            if rela_file_off is not None:
                rela_info = {'file_off': rela_file_off, 'size': rela_size}

        if jmprel_vaddr is not None and pltrel_size is not None:
            jmprel_file_off = None
            for l in loads:
                if l['vaddr'] <= jmprel_vaddr < l['vaddr'] + l['memsz']:
                    jmprel_file_off = l['offset'] + (jmprel_vaddr - l['vaddr'])
                    break
            if jmprel_file_off is not None:
                jmprel_info = {'file_off': jmprel_file_off, 'size': pltrel_size}

    # Process RELA entries
    # r_offset always needs updating (it's always where the linker writes)
    # r_addend only needs updating for types where it's a vaddr (RELATIVE, IRELATIVE)
    rela_entry_size = 24  # Elf64_Rela: r_offset(8) + r_info(8) + r_addend(8)
    rela_updates = 0

    for rela_data in [rela_info, jmprel_info]:
        if rela_data is None:
            continue
        num_entries = rela_data['size'] // rela_entry_size
        for i in range(num_entries):
            entry_off = rela_data['file_off'] + i * rela_entry_size
            if entry_off + rela_entry_size > new_file_size:
                break
            r_offset, r_info, r_addend = struct.unpack_from('<QQq', new_data, entry_off)
            r_type = r_info & 0xffffffff

            # Update r_offset for ALL relocation types
            offset_delta = get_delta_for_vaddr(r_offset, loads)
            if offset_delta != 0:
                struct.pack_into('<Q', new_data, entry_off, r_offset + offset_delta)
                rela_updates += 1

            # Update r_addend only for types where it's a virtual address
            if r_type in (R_AARCH64_RELATIVE, R_AARCH64_IRELATIVE):
                addend_delta = get_delta_for_vaddr(r_addend, loads)
                if addend_delta != 0:
                    struct.pack_into('<q', new_data, entry_off + 16, r_addend + addend_delta)
                    rela_updates += 1

    # --- UPDATE symbol table entries (st_value) ---
    # Elf64_Sym: st_name(4) + st_info(1) + st_other(1) + st_shndx(2) + st_value(8) + st_size(8)
    sym_entry_size = 24
    sym_updates = 0

    for sym_info in symtab_sections:
        sym_file_off = get_new_offset(sym_info['offset'])
        num_entries = sym_info['size'] // sym_entry_size
        for i in range(num_entries):
            entry_off = sym_file_off + i * sym_entry_size
            if entry_off + sym_entry_size > new_file_size:
                break
            st_name, st_info, st_other, st_shndx = struct.unpack_from('<IBBH', new_data, entry_off)
            st_value = struct.unpack_from('<Q', new_data, entry_off + 8)[0]
            st_size = struct.unpack_from('<Q', new_data, entry_off + 16)[0]

            delta = get_delta_for_vaddr(st_value, loads)
            if delta != 0:
                struct.pack_into('<Q', new_data, entry_off + 8, st_value + delta)
                sym_updates += 1

    # --- UPDATE ELF HEADER ---
    new_e_shoff = get_new_offset(e_shoff)
    struct.pack_into('<Q', new_data, 40, new_e_shoff)

    # --- UPDATE SECTION HEADERS ---
    for sh in shdrs:
        new_sh_off = get_new_offset(sh['offset'])
        sh_hdr_new_off = new_e_shoff + sh['idx'] * e_shentsize
        if sh_hdr_new_off + e_shentsize > new_file_size:
            continue
        struct.pack_into('<Q', new_data, sh_hdr_new_off + 24, new_sh_off)

    # --- UPDATE PROGRAM HEADERS ---
    for p in phdrs:
        new_offset = get_new_offset(p['offset'])
        delta = new_offset - p['offset']
        new_vaddr = p['vaddr'] + delta

        ph_off = p['hdr_off']
        struct.pack_into('<Q', new_data, ph_off + 8, new_offset)
        struct.pack_into('<Q', new_data, ph_off + 16, new_vaddr)
        struct.pack_into('<Q', new_data, ph_off + 24, new_vaddr)
        if p['type'] == 1:
            struct.pack_into('<Q', new_data, ph_off + 48, page_size)

    # --- SYNC PT_DYNAMIC TO ACTUAL .dynamic SECTION ---
    if dyn_section_info:
        actual_dyn_new_offset = get_new_offset(dyn_section_info['offset'])
        for p in phdrs:
            if p['type'] == 2:
                ph_off = p['hdr_off']
                pt_dyn_new_vaddr = p['vaddr'] + get_delta_for_offset(p['offset'])
                struct.pack_into('<Q', new_data, ph_off + 8, actual_dyn_new_offset)
                struct.pack_into('<Q', new_data, ph_off + 16, pt_dyn_new_vaddr)
                struct.pack_into('<Q', new_data, ph_off + 24, pt_dyn_new_vaddr)

    with open(path, 'wb') as f:
        f.write(new_data)

    total_pad = sum(pp[1] for pp in padding_points)
    print(f"Realigned: {path} (+{total_pad} bytes, RELA:{rela_updates}, SYM:{sym_updates}, DATA:{data_updates})")
    for l in loads:
        new_off = l['offset'] + l['delta']
        status = "OK" if new_off % page_size == 0 else f"FAIL(0x{new_off:x})"
        print(f"  LOAD: 0x{l['offset']:x}->0x{new_off:x} vaddr:0x{l['vaddr']:x}->0x{l['vaddr']+l['delta']:x} [{status}]")


if __name__ == '__main__':
    main()
