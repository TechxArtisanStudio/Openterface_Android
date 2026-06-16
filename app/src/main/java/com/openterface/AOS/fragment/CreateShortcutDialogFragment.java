package com.openterface.AOS.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.openterface.AOS.R;
import com.openterface.AOS.manager.ShortcutProfileManager;
import com.openterface.AOS.model.ShortcutProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for creating or editing a shortcut.
 * Provides modifier checkboxes (Ctrl/Shift/Alt/Win), key selection with category tabs,
 * and live preview.
 */
public class CreateShortcutDialogFragment extends DialogFragment {

    private static final String ARG_PROFILE_ID = "profile_id";
    private static final String ARG_SHORTCUT_ID = "shortcut_id";
    private static final String ARG_SHORTCUT_NAME = "shortcut_name";
    private static final String ARG_SHORTCUT_LABEL = "shortcut_label";
    private static final String ARG_SHORTCUT_MODIFIERS = "shortcut_modifiers";
    private static final String ARG_SHORTCUT_KEYCODE = "shortcut_keycode";

    // Key categories
    private static final int CATEGORY_LETTERS = 0;
    private static final int CATEGORY_NUMBERS = 1;
    private static final int CATEGORY_FUNCTION = 2;
    private static final int CATEGORY_SPECIAL = 3;
    private static final int CATEGORY_SYMBOLS = 4;
    private static final int CATEGORY_NUMPAD = 5;

    private ShortcutProfile profile;
    private ShortcutProfile.Shortcut editingShortcut;

    private TextView etName;
    private CheckBox cbCtrl, cbShift, cbAlt, cbWin;
    private ChipGroup chipGroupCategory;
    private TabLayout tabKeysCategory;
    private FlexboxLayout flexboxKeys;
    private TextView tvPreview;

    private List<KeyItem> currentKeys = new ArrayList<>();
    private KeyItem selectedKey;
    private int selectedCategoryIndex = -1;

    public interface OnShortcutCreatedListener {
        void onShortcutCreated(ShortcutProfile.Shortcut shortcut);
    }

    public static CreateShortcutDialogFragment newInstance(String profileId,
            @Nullable ShortcutProfile.Shortcut existingShortcut) {
        CreateShortcutDialogFragment fragment = new CreateShortcutDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROFILE_ID, profileId);
        if (existingShortcut != null) {
            args.putString(ARG_SHORTCUT_ID, existingShortcut.id);
            args.putString(ARG_SHORTCUT_NAME, existingShortcut.name);
            args.putString(ARG_SHORTCUT_LABEL, existingShortcut.label);
            args.putInt(ARG_SHORTCUT_MODIFIERS, existingShortcut.modifiers);
            args.putInt(ARG_SHORTCUT_KEYCODE, existingShortcut.keyCode);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String profileId = getArguments().getString(ARG_PROFILE_ID);
        profile = ShortcutProfileManager.getInstance(requireContext()).getProfileById(profileId);

        if (getArguments().containsKey(ARG_SHORTCUT_ID)) {
            editingShortcut = new ShortcutProfile.Shortcut();
            editingShortcut.id = getArguments().getString(ARG_SHORTCUT_ID);
            editingShortcut.name = getArguments().getString(ARG_SHORTCUT_NAME);
            editingShortcut.label = getArguments().getString(ARG_SHORTCUT_LABEL);
            editingShortcut.modifiers = getArguments().getInt(ARG_SHORTCUT_MODIFIERS);
            editingShortcut.keyCode = getArguments().getInt(ARG_SHORTCUT_KEYCODE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_shortcut, null);

        etName = view.findViewById(R.id.et_shortcut_name);
        cbCtrl = view.findViewById(R.id.cb_ctrl);
        cbShift = view.findViewById(R.id.cb_shift);
        cbAlt = view.findViewById(R.id.cb_alt);
        cbWin = view.findViewById(R.id.cb_win);
        chipGroupCategory = view.findViewById(R.id.chip_group_category);
        tabKeysCategory = view.findViewById(R.id.tab_keys_category);
        flexboxKeys = view.findViewById(R.id.flexbox_keys);
        tvPreview = view.findViewById(R.id.tv_preview);

        setupCategoryChips();
        setupCategoryTabs();

        cbCtrl.setOnCheckedChangeListener((btn, checked) -> updatePreview());
        cbShift.setOnCheckedChangeListener((btn, checked) -> updatePreview());
        cbAlt.setOnCheckedChangeListener((btn, checked) -> updatePreview());
        cbWin.setOnCheckedChangeListener((btn, checked) -> updatePreview());

        if (editingShortcut != null) {
            etName.setText(editingShortcut.name);
            cbCtrl.setChecked((editingShortcut.modifiers & ShortcutProfile.MOD_CTRL) != 0);
            cbShift.setChecked((editingShortcut.modifiers & ShortcutProfile.MOD_SHIFT) != 0);
            cbAlt.setChecked((editingShortcut.modifiers & ShortcutProfile.MOD_ALT) != 0);
            cbWin.setChecked((editingShortcut.modifiers & ShortcutProfile.MOD_WIN) != 0);

            selectedKey = new KeyItem(editingShortcut.label, editingShortcut.keyCode);

            int category = getCategoryForKeyCode(editingShortcut.keyCode);
            if (category >= 0 && tabKeysCategory.getTabCount() > category) {
                tabKeysCategory.getTabAt(category).select();
            }

            autoSelectCategoryChip();
            updatePreview();
        }

        return new AlertDialog.Builder(requireContext())
                .setTitle(editingShortcut != null ? "编辑快捷键" : "新建快捷键")
                .setView(view)
                .setPositiveButton("保存", (dialog, which) -> saveShortcut())
                .setNegativeButton("取消", null)
                .create();
    }

    private void setupCategoryChips() {
        if (profile == null || profile.categories == null) return;

        chipGroupCategory.removeAllViews();

        for (int i = 0; i < profile.categories.size(); i++) {
            ShortcutProfile.ShortcutCategory category = profile.categories.get(i);
            Chip chip = new Chip(requireContext());
            chip.setText(category.name);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setTag(i);
            chip.setOnClickListener(v -> {
                selectedCategoryIndex = (int) chip.getTag();
                updateChipSelection();
            });
            chipGroupCategory.addView(chip);
        }

        if (editingShortcut == null && chipGroupCategory.getChildCount() > 0) {
            selectedCategoryIndex = 0;
            Chip firstChip = (Chip) chipGroupCategory.getChildAt(0);
            firstChip.setChecked(true);
        }
    }

    private void updateChipSelection() {
        for (int i = 0; i < chipGroupCategory.getChildCount(); i++) {
            View child = chipGroupCategory.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.getTag() instanceof Integer) {
                    chip.setChecked((int) chip.getTag() == selectedCategoryIndex);
                }
            }
        }
    }

    private void autoSelectCategoryChip() {
        if (editingShortcut == null || profile == null || profile.categories == null) return;

        for (int catIdx = 0; catIdx < profile.categories.size(); catIdx++) {
            ShortcutProfile.ShortcutCategory cat = profile.categories.get(catIdx);
            if (cat.shortcuts != null) {
                for (ShortcutProfile.Shortcut s : cat.shortcuts) {
                    if (s != null && editingShortcut.id != null && editingShortcut.id.equals(s.id)) {
                        selectedCategoryIndex = catIdx;
                        updateChipSelection();
                        return;
                    }
                }
            }
        }
    }

    private void setupCategoryTabs() {
        String[] categories = {"字母", "数字", "功能键", "特殊", "符号", "小键盘"};

        for (String cat : categories) {
            TabLayout.Tab tab = tabKeysCategory.newTab();
            tab.setText(cat);
            tabKeysCategory.addTab(tab);
        }

        tabKeysCategory.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadKeysForCategory(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadKeysForCategory(CATEGORY_LETTERS);
    }

    private void loadKeysForCategory(int category) {
        currentKeys.clear();

        switch (category) {
            case CATEGORY_LETTERS:
                for (char c = 'A'; c <= 'Z'; c++) {
                    currentKeys.add(new KeyItem(String.valueOf(c), c - 'A' + 4));
                }
                break;

            case CATEGORY_NUMBERS:
                for (char c = '0'; c <= '9'; c++) {
                    currentKeys.add(new KeyItem(String.valueOf(c), c - '0' + 30));
                }
                break;

            case CATEGORY_FUNCTION:
                for (int i = 1; i <= 12; i++) {
                    currentKeys.add(new KeyItem("F" + i, 57 + i));
                }
                for (int i = 13; i <= 20; i++) {
                    currentKeys.add(new KeyItem("F" + i, 103 + (i - 13)));
                }
                break;

            case CATEGORY_SPECIAL:
                currentKeys.add(new KeyItem("Space", 44));
                currentKeys.add(new KeyItem("Enter", 40));
                currentKeys.add(new KeyItem("Esc", 41));
                currentKeys.add(new KeyItem("Tab", 43));
                currentKeys.add(new KeyItem("Backspace", 42));
                currentKeys.add(new KeyItem("Delete", 76));
                currentKeys.add(new KeyItem("Insert", 73));
                currentKeys.add(new KeyItem("Home", 74));
                currentKeys.add(new KeyItem("End", 77));
                currentKeys.add(new KeyItem("PgUp", 75));
                currentKeys.add(new KeyItem("PgDn", 78));
                currentKeys.add(new KeyItem("↑", 82));
                currentKeys.add(new KeyItem("↓", 81));
                currentKeys.add(new KeyItem("←", 80));
                currentKeys.add(new KeyItem("→", 79));
                currentKeys.add(new KeyItem("Caps Lock", 57));
                currentKeys.add(new KeyItem("Print", 70));
                currentKeys.add(new KeyItem("Scroll Lock", 71));
                currentKeys.add(new KeyItem("Pause", 72));
                currentKeys.add(new KeyItem("Num Lock", 83));
                break;

            case CATEGORY_SYMBOLS:
                currentKeys.add(new KeyItem("-", 45));
                currentKeys.add(new KeyItem("=", 46));
                currentKeys.add(new KeyItem("[", 47));
                currentKeys.add(new KeyItem("]", 48));
                currentKeys.add(new KeyItem("\\", 49));
                currentKeys.add(new KeyItem(";", 51));
                currentKeys.add(new KeyItem("'", 52));
                currentKeys.add(new KeyItem("`", 53));
                currentKeys.add(new KeyItem(",", 54));
                currentKeys.add(new KeyItem(".", 55));
                currentKeys.add(new KeyItem("/", 56));
                break;

            case CATEGORY_NUMPAD:
                currentKeys.add(new KeyItem("/", 84));
                currentKeys.add(new KeyItem("*", 85));
                currentKeys.add(new KeyItem("-", 86));
                currentKeys.add(new KeyItem("+", 87));
                currentKeys.add(new KeyItem("Enter", 88));
                currentKeys.add(new KeyItem("1", 89));
                currentKeys.add(new KeyItem("2", 90));
                currentKeys.add(new KeyItem("3", 91));
                currentKeys.add(new KeyItem("4", 92));
                currentKeys.add(new KeyItem("5", 93));
                currentKeys.add(new KeyItem("6", 94));
                currentKeys.add(new KeyItem("7", 95));
                currentKeys.add(new KeyItem("8", 96));
                currentKeys.add(new KeyItem("9", 97));
                currentKeys.add(new KeyItem("0", 98));
                currentKeys.add(new KeyItem(".", 99));
                break;
        }

        populateFlexbox();
    }

    private void populateFlexbox() {
        flexboxKeys.removeAllViews();

        for (KeyItem key : currentKeys) {
            TextView button = createKeyButton(key);
            flexboxKeys.addView(button);
        }
    }

    private TextView createKeyButton(KeyItem key) {
        TextView button = new TextView(requireContext());

        String displayLabel = getShortLabel(key.label);
        button.setText(displayLabel);
        button.setTextColor(0xFF212121);
        button.setTextSize(14);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setPadding(24, 12, 24, 12);

        // Set minimum width based on text length
        float density = getResources().getDisplayMetrics().density;
        int minWidth;
        if (displayLabel.length() <= 2) {
            minWidth = (int) (48 * density);
        } else if (displayLabel.length() <= 4) {
            minWidth = (int) (64 * density);
        } else {
            minWidth = (int) (80 * density);
        }
        button.setMinWidth(minWidth);
        button.setMinHeight((int) (40 * density));

        // Set background
        if (key.equals(selectedKey)) {
            button.setBackgroundResource(R.color.shortcut_selected);
        } else {
            button.setBackgroundResource(R.drawable.shortcut_button_background);
        }

        // Set layout params for FlexboxLayout
        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            FlexboxLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 8, 8);
        button.setLayoutParams(params);

        button.setOnClickListener(v -> onKeySelected(key));

        return button;
    }

    private String getShortLabel(String label) {
        switch (label) {
            case "Backspace":  return "Bksp";
            case "Caps Lock":  return "Caps";
            case "Print":      return "PrtSc";
            case "Scroll Lock": return "ScrLk";
            case "Num Lock":   return "NumLk";
            case "Page Up":    return "PgUp";
            case "Page Down":  return "PgDn";
            case "Insert":     return "Ins";
            case "Delete":     return "Del";
            case "Escape":     return "Esc";
            default:
                return label;
        }
    }

    private int getCategoryForKeyCode(int keyCode) {
        if (keyCode >= 4 && keyCode <= 29) return CATEGORY_LETTERS;
        if (keyCode >= 30 && keyCode <= 39) return CATEGORY_NUMBERS;
        if (keyCode >= 58 && keyCode <= 69) return CATEGORY_FUNCTION;
        if (keyCode == 40 || keyCode == 41 || keyCode == 42 || keyCode == 43 ||
            keyCode == 44 || keyCode == 57 || keyCode == 70 || keyCode == 72 ||
            keyCode == 73 || keyCode == 74 || keyCode == 75 || keyCode == 76 ||
            keyCode == 77 || keyCode == 78 || keyCode == 79 || keyCode == 80 ||
            keyCode == 81 || keyCode == 82 || keyCode == 83) {
            return CATEGORY_SPECIAL;
        }
        if ((keyCode >= 45 && keyCode <= 49) || (keyCode >= 51 && keyCode <= 56)) {
            return CATEGORY_SYMBOLS;
        }
        if (keyCode >= 84 && keyCode <= 99) return CATEGORY_NUMPAD;
        return -1;
    }

    private void onKeySelected(KeyItem key) {
        selectedKey = key;
        populateFlexbox();
        updatePreview();
    }

    private void updatePreview() {
        StringBuilder preview = new StringBuilder();

        if (cbCtrl.isChecked()) preview.append("Ctrl+");
        if (cbShift.isChecked()) preview.append("Shift+");
        if (cbAlt.isChecked()) preview.append("Alt+");
        if (cbWin.isChecked()) preview.append("Win+");

        if (selectedKey != null) {
            preview.append(selectedKey.label);
        }

        tvPreview.setText(preview.length() > 0 ? preview.toString() : "（未选择）");
    }

    private void saveShortcut() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("请输入名称");
            return;
        }

        if (selectedKey == null) {
            Toast.makeText(requireContext(), "请选择按键", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategoryIndex < 0 || profile == null || profile.categories == null
                || selectedCategoryIndex >= profile.categories.size()) {
            Toast.makeText(requireContext(), "请选择目标类别", Toast.LENGTH_SHORT).show();
            return;
        }

        int modifiers = 0;
        if (cbCtrl.isChecked()) modifiers |= ShortcutProfile.MOD_CTRL;
        if (cbShift.isChecked()) modifiers |= ShortcutProfile.MOD_SHIFT;
        if (cbAlt.isChecked()) modifiers |= ShortcutProfile.MOD_ALT;
        if (cbWin.isChecked()) modifiers |= ShortcutProfile.MOD_WIN;

        String label = tvPreview.getText().toString();
        ShortcutProfile.ShortcutCategory targetCategory = profile.categories.get(selectedCategoryIndex);

        if (editingShortcut != null) {
            editingShortcut.name = name;
            editingShortcut.label = label;
            editingShortcut.modifiers = modifiers;
            editingShortcut.keyCode = selectedKey.keyCode;

            int oldCategoryIndex = findCategoryForShortcut(editingShortcut.id);
            if (oldCategoryIndex >= 0 && oldCategoryIndex != selectedCategoryIndex) {
                ShortcutProfile.ShortcutCategory oldCategory = profile.categories.get(oldCategoryIndex);
                oldCategory.shortcuts.removeIf(s -> s != null && editingShortcut.id.equals(s.id));

                if (targetCategory.shortcuts == null) {
                    targetCategory.shortcuts = new ArrayList<>();
                }
                targetCategory.shortcuts.add(editingShortcut);
            }

            ShortcutProfileManager.getInstance(requireContext()).updateProfile(profile);

            if (getTargetFragment() instanceof OnShortcutCreatedListener) {
                ((OnShortcutCreatedListener) getTargetFragment()).onShortcutCreated(editingShortcut);
            }
        } else {
            ShortcutProfile.Shortcut newShortcut = new ShortcutProfile.Shortcut();
            newShortcut.id = "shortcut_" + System.currentTimeMillis();
            newShortcut.name = name;
            newShortcut.label = label;
            newShortcut.modifiers = modifiers;
            newShortcut.keyCode = selectedKey.keyCode;

            if (targetCategory.shortcuts == null) {
                targetCategory.shortcuts = new ArrayList<>();
            }
            targetCategory.shortcuts.add(newShortcut);

            ShortcutProfileManager.getInstance(requireContext()).updateProfile(profile);

            if (getTargetFragment() instanceof OnShortcutCreatedListener) {
                ((OnShortcutCreatedListener) getTargetFragment()).onShortcutCreated(newShortcut);
            }
        }

        dismiss();
    }

    private int findCategoryForShortcut(String shortcutId) {
        if (shortcutId == null || profile == null || profile.categories == null) return -1;

        for (int i = 0; i < profile.categories.size(); i++) {
            ShortcutProfile.ShortcutCategory cat = profile.categories.get(i);
            if (cat.shortcuts != null) {
                for (ShortcutProfile.Shortcut s : cat.shortcuts) {
                    if (s != null && shortcutId.equals(s.id)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public static class KeyItem implements java.io.Serializable {
        public final String label;
        public final int keyCode;

        public KeyItem(String label, int keyCode) {
            this.label = label;
            this.keyCode = keyCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof KeyItem) {
                return keyCode == ((KeyItem) obj).keyCode;
            }
            return false;
        }
    }
}
