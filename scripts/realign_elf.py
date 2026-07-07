#!/usr/bin/env python3
"""
realign_elf.py — Realign ELF PT_LOAD segments to 16 KB page boundaries.

Fixes program headers AND section headers so the file remains loadable.

Usage:
    python3 realign_elf.py <path-to-.so> [--page-size 16384]
"""

import argparse
import struct
import sys
from pathlib import Path

PAGE_SIZE = 16384  # 16 KB

def align_up(value, alignment):
    return (value + alignment - 1) & ~(alignment - 1)

def main():
    parser = argparse.ArgumentParser(description="Realign ELF LOAD segments")
    parser.add_argument("input_file", help="Path to ELF .so file")
    parser.add_argument("--page-size", type=int, default=PAGE_SIZE, help="Target page size")
    args = parser.parse_args()

    path = Path(args.input_file)
    page_size = args.page_size

    if not path.exists():
        print(f"Error: {path} not found", file=sys.stderr)
        sys.exit(1)

    with open(path, 'rb') as f:
        data = bytearray(f.read())

    if data[:4] != b'\x7fELF':
        print(f"Error: not an ELF file", file=sys.stderr)
        sys.exit(1)

    is_64 = (data[4] == 2)
    file_size_orig = len(data)

    # --- Parse ELF header ---
    if is_64:
        e_phoff = struct.unpack_from('<Q', data, 32)[0]
        e_shoff = struct.unpack_from('<Q', data, 40)[0]
        e_phentsize = struct.unpack_from('<H', data, 54)[0]
        e_phnum = struct.unpack_from('<H', data, 56)[0]
        e_shentsize = struct.unpack_from('<H', data, 58)[0]
        e_shnum = struct.unpack_from('<H', data, 60)[0]
        ph_fmt = '<IIQQQQQQ'
    else:
        e_phoff = struct.unpack_from('<I', data, 28)[0]
        e_shoff = struct.unpack_from('<I', data, 32)[0]
        e_phentsize = struct.unpack_from('<H', data, 42)[0]
        e_phnum = struct.unpack_from('<H', data, 44)[0]
        e_shentsize = struct.unpack_from('<H', data, 46)[0]
        e_shnum = struct.unpack_from('<H', data, 48)[0]
        ph_fmt = '<IIIIIIII'

    print(f"ELF {is_64}-bit, phoff=0x{e_phoff:x} ({e_phnum}), shoff=0x{e_shoff:x} ({e_shnum}), size=0x{file_size_orig:x}")

    # --- Read program headers ---
    phdrs = []
    for i in range(e_phnum):
        off = e_phoff + i * e_phentsize
        vals = struct.unpack_from(ph_fmt, data, off)
        if is_64:
            p_type, p_flags, p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_align = vals
        else:
            p_type, p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_flags, p_align = vals
        phdrs.append({
            'idx': i, 'off': off,
            'p_type': p_type, 'p_flags': p_flags,
            'p_offset': p_offset, 'p_vaddr': p_vaddr, 'p_paddr': p_paddr,
            'p_filesz': p_filesz, 'p_memsz': p_memsz, 'p_align': p_align,
        })

    load_phdrs = sorted([s for s in phdrs if s['p_type'] == 1], key=lambda s: s['p_offset'])

    if not load_phdrs:
        print("No PT_LOAD segments")
        return

    already_aligned = all(
        (s['p_offset'] % page_size) == 0 and (s['p_align'] >= page_size)
        for s in load_phdrs
    )
    if already_aligned:
        print("All PT_LOAD already aligned")
        return

    # --- Build a layout map: for each old offset, compute the new offset ---
    #
    # File layout regions (in order):
    #   [pre_seg1] [seg1 data] [gap1] [seg2 data] [gap2] ... [segN data] [trailer]
    #
    # When copying to new file:
    #   - pre_seg1 stays at offset 0
    #   - segK data is copied to a 16KB-aligned position
    #   - gapK and trailer are copied as-is after the previous segment

    # Build list of (old_start, old_end, region_kind, seg_idx)
    regions = []
    prev_end = 0
    for seg in load_phdrs:
        if seg['p_offset'] > prev_end:
            regions.append((prev_end, seg['p_offset'], 'gap', -1))
        regions.append((seg['p_offset'], seg['p_offset'] + seg['p_filesz'], 'load', seg['idx']))
        prev_end = seg['p_offset'] + seg['p_filesz']
    if prev_end < file_size_orig:
        regions.append((prev_end, file_size_orig, 'trailer', -1))

    # Compute new file positions
    new_positions = []  # (old_start, old_end, new_start, kind, seg_idx)
    for old_start, old_end, kind, seg_idx in regions:
        if kind == 'load':
            new_start = align_up(len(new_positions) and new_positions[-1][2] + (new_positions[-1][1] - new_positions[-1][0]) or old_start, page_size)
            # Actually: new_start = align_up(current_new_file_pos, page_size)
            # current_new_file_pos = sum of (new_start_i + size_i) for all previous regions
            current_pos = 0
            for prev in new_positions:
                current_pos = prev[2] + (prev[1] - prev[0])
            new_start = align_up(current_pos, page_size)
        else:
            # Non-LOAD: copy right after previous region
            current_pos = 0
            for prev in new_positions:
                current_pos = prev[2] + (prev[1] - prev[0])
            new_start = current_pos

        new_positions.append((old_start, old_end, new_start, kind, seg_idx))

    # Build a function: given old offset, return new offset
    def old_to_new(old_off):
        for rs, re, ns, kind, _ in new_positions:
            if rs <= old_off < re:
                return ns + (old_off - rs)
        return old_off  # beyond all regions (shouldn't happen)

    total_padding = 0
    for rs, re, ns, kind, _ in new_positions:
        if rs == 0:
            total_padding += ns - rs  # 0
        else:
            total_padding += (ns - rs) - sum(1 for r in new_positions if r[0] < rs and r[3] == 'load') * 0
            # Actually: delta for this region = ns - rs
            pass

    # Compute total padding from file size difference
    # Simpler: sum up all padding bytes inserted
    total_pad_bytes = 0
    for i, (rs, re, ns, kind, _) in enumerate(new_positions):
        if kind == 'load':
            # padding before this segment
            prev_end_new = new_positions[i-1][2] + (new_positions[i-1][1] - new_positions[i-1][0]) if i > 0 else 0
            pad = ns - prev_end_new
            if pad > 0:
                total_pad_bytes += pad

    # Build new file data
    new_data = bytearray()
    for old_start, old_end, new_start, kind, seg_idx in new_positions:
        size = old_end - old_start
        # Pad if needed to reach new_start
        current_pos = len(new_data)
        if current_pos < new_start:
            new_data.extend(b'\x00' * (new_start - current_pos))
        new_data.extend(data[old_start:old_end])

    file_size_new = len(new_data)
    print(f"File: 0x{file_size_orig:x} -> 0x{file_size_new:x} (+{file_size_new - file_size_orig})")

    # --- Update section header table offset ---
    new_shoff = old_to_new(e_shoff)
    if is_64:
        struct.pack_into('<Q', new_data, 40, new_shoff)
    else:
        struct.pack_into('<I', new_data, 32, new_shoff)

    # --- Update section header sh_offset fields ---
    for i in range(e_shnum):
        shdr_off_new = new_shoff + i * e_shentsize
        if shdr_off_new + e_shentsize > file_size_new:
            break
        if is_64:
            sh_offset_old = struct.unpack_from('<Q', new_data, shdr_off_new + 24)[0]
        else:
            sh_offset_old = struct.unpack_from('<I', new_data, shdr_off_new + 16)[0]

        if sh_offset_old == 0:
            continue

        new_sh_offset = old_to_new(sh_offset_old)
        if new_sh_offset != sh_offset_old:
            if is_64:
                struct.pack_into('<Q', new_data, shdr_off_new + 24, new_sh_offset)
            else:
                struct.pack_into('<I', new_data, shdr_off_new + 16, new_sh_offset)

    # --- Update program headers ---
    for seg in phdrs:
        if seg['p_type'] == 1:
            new_offset = old_to_new(seg['p_offset'])
            delta = new_offset - seg['p_offset']
            new_vaddr = seg['p_vaddr'] + delta

            phdr_off = seg['off']
            if is_64:
                struct.pack_into('<Q', new_data, phdr_off + 8, new_offset)   # p_offset
                struct.pack_into('<Q', new_data, phdr_off + 16, new_vaddr)  # p_vaddr
                struct.pack_into('<Q', new_data, phdr_off + 24, new_vaddr)  # p_paddr
                struct.pack_into('<Q', new_data, phdr_off + 48, page_size)  # p_align
            else:
                struct.pack_into('<I', new_data, phdr_off + 4, new_offset)
                struct.pack_into('<I', new_data, phdr_off + 8, new_vaddr)
                struct.pack_into('<I', new_data, phdr_off + 12, new_vaddr)
                struct.pack_into('<I', new_data, phdr_off + 28, page_size)

            print(f"  LOAD #{seg['idx']}: 0x{seg['p_offset']:x}->0x{new_offset:x}, "
                  f"vaddr 0x{seg['p_vaddr']:x}->0x{new_vaddr:x}, align->0x{page_size:x}")

    # Write
    with open(path, 'wb') as f:
        f.write(new_data)

    print(f"Done: {path}")

if __name__ == '__main__':
    main()