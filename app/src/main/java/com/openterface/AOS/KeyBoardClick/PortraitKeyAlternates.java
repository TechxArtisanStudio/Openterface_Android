package com.openterface.AOS.KeyBoardClick;

import java.util.HashMap;
import java.util.Map;

/**
 * 竖屏键盘按键替代字符映射
 * 参考 KeyCMD 的 BasicPhysicalKeyboardView 实现
 */
public class PortraitKeyAlternates {

    /**
     * 替代字符数据结构
     */
    public static class AlternateInfo {
        public String hint;          // 按键上显示的提示字符（如 "1!"）
        public String[] options;     // 长按弹窗的可选项（如 ["Q", "1", "!"]）
        public int[] hidCodes;       // 对应的 HID 码
        public boolean[] needShift;  // 是否需要 Shift 修饰

        public AlternateInfo(String hint, String[] options, int[] hidCodes, boolean[] needShift) {
            this.hint = hint;
            this.options = options;
            this.hidCodes = hidCodes;
            this.needShift = needShift;
        }
    }

    private static final Map<Integer, AlternateInfo> ALTERNATES_MAP = new HashMap<>();

    static {
        // Row 1: Q W E R T Y U I O P
        // 字母键不显示替代字符提示，但长按可选择大写
        initAlternates(0x14, "Q", new String[]{"Q", "q"}, new int[]{0x14, 0x14}, new boolean[]{true, false});
        initAlternates(0x1A, "W", new String[]{"W", "w"}, new int[]{0x1A, 0x1A}, new boolean[]{true, false});
        initAlternates(0x08, "E", new String[]{"E", "e"}, new int[]{0x08, 0x08}, new boolean[]{true, false});
        initAlternates(0x15, "R", new String[]{"R", "r"}, new int[]{0x15, 0x15}, new boolean[]{true, false});
        initAlternates(0x17, "T", new String[]{"T", "t"}, new int[]{0x17, 0x17}, new boolean[]{true, false});
        initAlternates(0x1C, "Y", new String[]{"Y", "y"}, new int[]{0x1C, 0x1C}, new boolean[]{true, false});
        initAlternates(0x18, "U", new String[]{"U", "u"}, new int[]{0x18, 0x18}, new boolean[]{true, false});
        initAlternates(0x0C, "I", new String[]{"I", "i"}, new int[]{0x0C, 0x0C}, new boolean[]{true, false});
        initAlternates(0x12, "O", new String[]{"O", "o"}, new int[]{0x12, 0x12}, new boolean[]{true, false});
        initAlternates(0x13, "P", new String[]{"P", "p"}, new int[]{0x13, 0x13}, new boolean[]{true, false});

        // Row 2: A S D F G H J K L
        initAlternates(0x04, "A", new String[]{"A", "a"}, new int[]{0x04, 0x04}, new boolean[]{true, false});
        initAlternates(0x16, "S", new String[]{"S", "s"}, new int[]{0x16, 0x16}, new boolean[]{true, false});
        initAlternates(0x07, "D", new String[]{"D", "d"}, new int[]{0x07, 0x07}, new boolean[]{true, false});
        initAlternates(0x09, "F", new String[]{"F", "f"}, new int[]{0x09, 0x09}, new boolean[]{true, false});
        initAlternates(0x0A, "G", new String[]{"G", "g"}, new int[]{0x0A, 0x0A}, new boolean[]{true, false});
        initAlternates(0x0B, "H", new String[]{"H", "h"}, new int[]{0x0B, 0x0B}, new boolean[]{true, false});
        initAlternates(0x0D, "J", new String[]{"J", "j"}, new int[]{0x0D, 0x0D}, new boolean[]{true, false});
        initAlternates(0x0E, "K", new String[]{"K", "k"}, new int[]{0x0E, 0x0E}, new boolean[]{true, false});
        initAlternates(0x0F, "L", new String[]{"L", "l"}, new int[]{0x0F, 0x0F}, new boolean[]{true, false});

        // Row 3: Z X C V B N M
        initAlternates(0x1D, "Z", new String[]{"Z", "z"}, new int[]{0x1D, 0x1D}, new boolean[]{true, false});
        initAlternates(0x1B, "X", new String[]{"X", "x"}, new int[]{0x1B, 0x1B}, new boolean[]{true, false});
        initAlternates(0x06, "C", new String[]{"C", "c"}, new int[]{0x06, 0x06}, new boolean[]{true, false});
        initAlternates(0x19, "V", new String[]{"V", "v"}, new int[]{0x19, 0x19}, new boolean[]{true, false});
        initAlternates(0x05, "B", new String[]{"B", "b"}, new int[]{0x05, 0x05}, new boolean[]{true, false});
        initAlternates(0x11, "N", new String[]{"N", "n"}, new int[]{0x11, 0x11}, new boolean[]{true, false});
        initAlternates(0x10, "M", new String[]{"M", "m"}, new int[]{0x10, 0x10}, new boolean[]{true, false});

        // 符号键：显示 Shift 版本的提示
        // [ 键：显示 {
        initAlternates(0x2F, "{", new String[]{"[", "{"}, new int[]{0x2F, 0x2F}, new boolean[]{false, true});
        // ] 键：显示 }
        initAlternates(0x30, "}", new String[]{"]", "}"}, new int[]{0x30, 0x30}, new boolean[]{false, true});
        // \ 键：显示 |
        initAlternates(0x31, "|", new String[]{"\\", "|"}, new int[]{0x31, 0x31}, new boolean[]{false, true});
        // ; 键：显示 :
        initAlternates(0x33, ":", new String[]{";", ":"}, new int[]{0x33, 0x33}, new boolean[]{false, true});
        // ' 键：显示 "
        initAlternates(0x34, "\"", new String[]{"'", "\""}, new int[]{0x34, 0x34}, new boolean[]{false, true});
        // , 键：显示 <
        initAlternates(0x36, "<", new String[]{",", "<"}, new int[]{0x36, 0x36}, new boolean[]{false, true});
        // . 键：显示 >
        initAlternates(0x37, ">", new String[]{".", ">"}, new int[]{0x37, 0x37}, new boolean[]{false, true});
        // / 键：显示 ?
        initAlternates(0x38, "?", new String[]{"/", "?"}, new int[]{0x38, 0x38}, new boolean[]{false, true});
    }

    private static void initAlternates(int hidCode, String hint, String[] options, int[] codes, boolean[] needShift) {
        ALTERNATES_MAP.put(hidCode, new AlternateInfo(hint, options, codes, needShift));
    }

    /**
     * 获取按键的替代字符信息
     */
    public static AlternateInfo getAlternates(int hidCode) {
        return ALTERNATES_MAP.get(hidCode);
    }

    /**
     * 检查按键是否有替代字符
     */
    public static boolean hasAlternates(int hidCode) {
        return ALTERNATES_MAP.containsKey(hidCode);
    }
}
