package com.openterface.AOS.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.openterface.AOS.R;
import com.openterface.AOS.target.KeyBoardManager;
import com.openterface.AOS.adapter.HubProfileAdapter;
import com.openterface.AOS.adapter.HubShortcutAdapter;
import com.openterface.AOS.manager.ShortcutProfileManager;
import com.openterface.AOS.model.ShortcutProfile;
import com.openterface.AOS.model.ShortcutProfile.Shortcut;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shortcut Hub floating panel - dual-panel design (reference: KeyCMD)
 * Left: Profile list
 * Right: Shortcut details (category tabs + grid/list)
 */
public class ShortcutHubFloatingFragment extends AppCompatDialogFragment
        implements HubProfileAdapter.OnProfileActionListener,
                   HubShortcutAdapter.OnShortcutActionListener,
                   CreateShortcutDialogFragment.OnShortcutCreatedListener {

    private static final String TAG = "OP-SHORTCUT";
    private static final int TAB_FAVORITES_INDEX = 0;

    // Views
    private View panelProfileList;
    private View panelShortcutDetail;
    private RecyclerView recyclerProfiles;
    private RecyclerView recyclerMyShortcuts;
    private RecyclerView recyclerBrowseShortcuts;
    private TabLayout tabLayout;
    private TextView tvDetailProfileName;
    private TextView tvEmptyProfiles;
    private TextView tvEmptyShortcuts;
    private Button btnNewProfile;
    private Button btnImportProfile;
    private Button btnExportAll;
    private ImageButton btnBackToProfiles;
    private ImageButton btnAddShortcut;
    private ImageButton btnClose;
    private View overlay;
    private View card;

    // Adapters
    private HubProfileAdapter profileAdapter;
    private HubShortcutAdapter myShortcutsAdapter;
    private HubShortcutAdapter browseShortcutsAdapter;

    // Data
    private ShortcutProfileManager profileManager;
    private List<ShortcutProfile> profiles = new ArrayList<>();
    private ShortcutProfile selectedProfile;
    private ShortcutProfile activeProfile;
    private List<Shortcut> myShortcuts = new ArrayList<>();
    private List<String> favoriteIds = new ArrayList<>();
    private int currentTabPosition = 0;

    // Gesture detector for swipe to dismiss
    private GestureDetector gestureDetector;

    // File import/export launchers
    private ActivityResultLauncher<String> importFileLauncher;
    private ActivityResultLauncher<String> exportFileLauncher;
    private ShortcutProfile pendingExportProfile;
    private boolean isExportingAllProfiles = false;

    // Callback
    private OnDismissListener dismissListener;

    public interface OnDismissListener {
        void onHubDismissed();
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize file import launcher
        importFileLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    importFromFile(uri);
                }
            }
        );

        // Initialize file export launcher
        exportFileLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) {
                    if (isExportingAllProfiles) {
                        exportAllToFile(uri);
                    } else if (pendingExportProfile != null) {
                        exportToFile(uri, pendingExportProfile);
                    }
                    pendingExportProfile = null;
                    isExportingAllProfiles = false;
                }
            }
        );
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_shortcut_hub_floating, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileManager = ShortcutProfileManager.getInstance(requireContext());

        // Initialize views
        panelProfileList = view.findViewById(R.id.panel_profile_list);
        panelShortcutDetail = view.findViewById(R.id.panel_shortcut_detail);
        recyclerProfiles = view.findViewById(R.id.recycler_profiles);
        recyclerMyShortcuts = view.findViewById(R.id.recycler_my_shortcuts);
        recyclerBrowseShortcuts = view.findViewById(R.id.recycler_browse_shortcuts);
        tabLayout = view.findViewById(R.id.tab_layout_categories);
        tvDetailProfileName = view.findViewById(R.id.tv_detail_profile_name);
        tvEmptyProfiles = view.findViewById(R.id.tv_empty_profiles);
        tvEmptyShortcuts = view.findViewById(R.id.tv_empty_shortcuts);
        btnNewProfile = view.findViewById(R.id.btn_new_profile);
        btnImportProfile = view.findViewById(R.id.btn_import_profile);
        btnExportAll = view.findViewById(R.id.btn_export_all);
        btnBackToProfiles = view.findViewById(R.id.btn_back_to_profiles);
        btnAddShortcut = view.findViewById(R.id.btn_add_shortcut);
        btnClose = view.findViewById(R.id.btn_close_hub);
        overlay = view.findViewById(R.id.hub_dim_overlay);
        card = view.findViewById(R.id.hub_floating_card);

        // Setup gesture detector (swipe left to dismiss)
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e1.getX() - e2.getX() > 100 && Math.abs(velocityX) > 100) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        // Setup click listeners
        btnClose.setOnClickListener(v -> dismiss());
        overlay.setOnClickListener(v -> dismiss());
        btnNewProfile.setOnClickListener(v -> showCreateProfileDialog());
        btnImportProfile.setOnClickListener(v -> showImportDialog());
        btnExportAll.setOnClickListener(v -> exportAllProfiles());
        btnBackToProfiles.setOnClickListener(v -> showProfileListPanel());
        btnAddShortcut.setOnClickListener(v -> showCreateShortcutDialog(null));

        card.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        // Setup adapters
        profileAdapter = new HubProfileAdapter(this);
        recyclerProfiles.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerProfiles.setAdapter(profileAdapter);

        myShortcutsAdapter = new HubShortcutAdapter(HubShortcutAdapter.MODE_CARD, this);
        recyclerMyShortcuts.setLayoutManager(new GridLayoutManager(getContext(), 4));
        recyclerMyShortcuts.setAdapter(myShortcutsAdapter);

        browseShortcutsAdapter = new HubShortcutAdapter(HubShortcutAdapter.MODE_ROW, this);
        recyclerBrowseShortcuts.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerBrowseShortcuts.setAdapter(browseShortcutsAdapter);

        // Setup tab listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                refreshShortcutContent();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Load data
        loadData();
        showProfileListPanel();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                );
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null) {
            dismissListener.onHubDismissed();
        }
    }

    // === Data Loading ===

    private void loadData() {
        profiles = profileManager.getAllProfiles();
        activeProfile = profileManager.getActiveProfile();
        profileAdapter.setProfiles(profiles, activeProfile != null ? activeProfile.id : null);

        if (profiles.isEmpty()) {
            tvEmptyProfiles.setVisibility(View.VISIBLE);
            recyclerProfiles.setVisibility(View.GONE);
        } else {
            tvEmptyProfiles.setVisibility(View.GONE);
            recyclerProfiles.setVisibility(View.VISIBLE);
        }
    }

    // === Panel Switching ===

    private void showProfileListPanel() {
        panelProfileList.setVisibility(View.VISIBLE);
        panelShortcutDetail.setVisibility(View.GONE);
    }

    private void showDetailPanel(ShortcutProfile profile) {
        selectedProfile = profile;
        tvDetailProfileName.setText(profile.name);

        panelProfileList.setVisibility(View.GONE);
        panelShortcutDetail.setVisibility(View.VISIBLE);

        // Load favorites
        myShortcuts = profileManager.getMyShortcuts(profile.id);
        favoriteIds = new ArrayList<>();
        for (Shortcut s : myShortcuts) {
            if (s != null && s.id != null) {
                favoriteIds.add(s.id);
            }
        }

        // Build tabs
        buildCategoryTabs(profile);

        // Default to favorites tab
        currentTabPosition = 0;
        if (tabLayout.getTabCount() > 0) {
            TabLayout.Tab tab = tabLayout.getTabAt(0);
            if (tab != null) tab.select();
        }
        refreshShortcutContent();
    }

    private void buildCategoryTabs(ShortcutProfile profile) {
        tabLayout.removeAllTabs();

        // First tab: Favorites
        TabLayout.Tab favTab = tabLayout.newTab();
        favTab.setText(R.string.shortcut_hub_favorites);
        tabLayout.addTab(favTab);

        // Category tabs
        if (profile.categories != null) {
            for (ShortcutProfile.ShortcutCategory category : profile.categories) {
                TabLayout.Tab tab = tabLayout.newTab();
                tab.setText(category.name);
                tabLayout.addTab(tab);
            }
        }

        // Setup long-press on category tabs (not favorites) for rename/delete
        ViewGroup tabsViewGroup = (ViewGroup) tabLayout.getChildAt(0);
        for (int i = 0; i < tabsViewGroup.getChildCount(); i++) {
            final int tabIndex = i;
            // Skip the favorites tab (index 0)
            if (tabIndex == 0) continue;
            tabsViewGroup.getChildAt(i).setOnLongClickListener(v -> {
                int categoryIndex = tabIndex - 1;
                if (selectedProfile != null && selectedProfile.categories != null
                        && categoryIndex >= 0 && categoryIndex < selectedProfile.categories.size()) {
                    showCategoryManageMenu(selectedProfile.categories.get(categoryIndex), v);
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Show a popup menu for managing a category (rename / delete).
     */
    private void showCategoryManageMenu(ShortcutProfile.ShortcutCategory category, View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(getContext(), anchor);
        popup.getMenu().add(0, 1, 0, R.string.shortcut_hub_rename_category);
        popup.getMenu().add(0, 2, 1, R.string.shortcut_hub_delete_category);

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    showRenameCategoryDialog(category);
                    return true;
                case 2:
                    showDeleteCategoryDialog(category);
                    return true;
            }
            return false;
        });

        popup.show();
    }

    /**
     * Show dialog to rename a category.
     */
    private void showRenameCategoryDialog(ShortcutProfile.ShortcutCategory category) {
        EditText input = new EditText(requireContext());
        input.setText(category.name);
        input.setSelectAllOnFocus(true);
        input.setMaxLines(1);

        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setPadding(48, 24, 48, 24);
        container.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.shortcut_hub_rename_category)
                .setView(container)
                .setPositiveButton(R.string.shortcut_dialog_save, (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        category.name = newName;
                        profileManager.updateProfile(selectedProfile);
                        // Rebuild tabs to reflect the new name
                        buildCategoryTabs(selectedProfile);
                        // Try to keep the same tab selected
                        if (currentTabPosition < tabLayout.getTabCount()) {
                            TabLayout.Tab tab = tabLayout.getTabAt(currentTabPosition);
                            if (tab != null) tab.select();
                        }
                        refreshShortcutContent();
                        Toast.makeText(getContext(), R.string.shortcut_hub_renamed, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), R.string.shortcut_hub_name_cannot_be_empty, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.shortcut_dialog_cancel, null)
                .show();
    }

    /**
     * Show dialog to confirm deletion of a category (and its shortcuts).
     */
    private void showDeleteCategoryDialog(ShortcutProfile.ShortcutCategory category) {
        int shortcutCount = (category.shortcuts != null) ? category.shortcuts.size() : 0;
        String message = getString(R.string.shortcut_hub_delete_category_message, category.name, shortcutCount);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.shortcut_hub_delete_category)
                .setMessage(message)
                .setPositiveButton(R.string.shortcut_hub_delete_label, (dialog, which) -> {
                    // Remove the category from the profile
                    selectedProfile.categories.remove(category);
                    profileManager.updateProfile(selectedProfile);

                    // Reset to favorites tab
                    currentTabPosition = TAB_FAVORITES_INDEX;
                    buildCategoryTabs(selectedProfile);
                    if (tabLayout.getTabCount() > 0) {
                        TabLayout.Tab tab = tabLayout.getTabAt(0);
                        if (tab != null) tab.select();
                    }
                    refreshShortcutContent();
                    Toast.makeText(getContext(), R.string.shortcut_hub_category_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.shortcut_dialog_cancel, null)
                .show();
    }

    private void refreshShortcutContent() {
        if (selectedProfile == null) return;

        if (currentTabPosition == TAB_FAVORITES_INDEX) {
            // Favorites tab
            recyclerMyShortcuts.setVisibility(View.VISIBLE);
            recyclerBrowseShortcuts.setVisibility(View.GONE);

            myShortcutsAdapter.setShortcuts(myShortcuts);

            if (myShortcuts.isEmpty()) {
                tvEmptyShortcuts.setVisibility(View.VISIBLE);
                recyclerMyShortcuts.setVisibility(View.GONE);
            } else {
                tvEmptyShortcuts.setVisibility(View.GONE);
                recyclerMyShortcuts.setVisibility(View.VISIBLE);
            }
        } else {
            // Category tab
            recyclerMyShortcuts.setVisibility(View.GONE);
            recyclerBrowseShortcuts.setVisibility(View.VISIBLE);
            tvEmptyShortcuts.setVisibility(View.GONE);

            int categoryIndex = currentTabPosition - 1;
            if (selectedProfile.categories != null && categoryIndex < selectedProfile.categories.size()) {
                ShortcutProfile.ShortcutCategory category = selectedProfile.categories.get(categoryIndex);
                browseShortcutsAdapter.setShortcuts(category.shortcuts);
                browseShortcutsAdapter.setFavoriteIds(favoriteIds);
            }
        }
    }

    // === HubProfileAdapter.OnProfileActionListener ===

    @Override
    public void onProfileSelected(ShortcutProfile profile) {
        showDetailPanel(profile);
    }

    @Override
    public void onProfileLongPress(ShortcutProfile profile, View anchor) {
        showProfileOverflowMenu(profile, anchor);
    }

    // === HubShortcutAdapter.OnShortcutActionListener ===

    @Override
    public void onShortcutClick(Shortcut shortcut) {
        executeShortcut(shortcut);
    }

    @Override
    public void onShortcutLongClick(Shortcut shortcut) {
        showCreateShortcutDialog(shortcut);
    }

    @Override
    public void onToggleFavorite(Shortcut shortcut) {
        if (shortcut == null || shortcut.id == null) return;

        if (favoriteIds.contains(shortcut.id)) {
            // Remove from favorites
            myShortcuts.removeIf(s -> s.id != null && s.id.equals(shortcut.id));
            favoriteIds.remove(shortcut.id);
            Toast.makeText(getContext(), R.string.shortcut_hub_unfavorited, Toast.LENGTH_SHORT).show();
        } else {
            // Add to favorites
            Shortcut clone = new Shortcut(shortcut.id, shortcut.name, shortcut.label,
                    shortcut.modifiers, shortcut.keyCode, shortcut.icon, shortcut.displayOrder);
            myShortcuts.add(clone);
            favoriteIds.add(shortcut.id);
            Toast.makeText(getContext(), R.string.shortcut_hub_favorited, Toast.LENGTH_SHORT).show();
        }

        // Save
        profileManager.updateMyShortcuts(selectedProfile.id, myShortcuts);

        // Refresh
        refreshShortcutContent();
    }

    @Override
    public void onOverflowClick(Shortcut shortcut, View anchor) {
        showShortcutOverflowMenu(shortcut, anchor);
    }

    // === HID Execution ===

    private void executeShortcut(Shortcut shortcut) {
        if (shortcut == null) return;

        String modifier = getModifierString(shortcut.modifiers);
        String key = getKeyString(shortcut.keyCode);

        if (key == null || key.isEmpty()) {
            Toast.makeText(getContext(), R.string.shortcut_hub_unknown_key, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Executing shortcut: " + modifier + " + " + key);

        if (modifier.isEmpty()) {
            KeyBoardManager.sendKeyBoardPressAndRelease("", key);
        } else {
            KeyBoardManager.sendKeyBoardShortCut(modifier, key);
        }

        String sentLabel = shortcut.label != null ? shortcut.label : key;
        Toast.makeText(getContext(), getString(R.string.shortcut_hub_sent_format, sentLabel),
                Toast.LENGTH_SHORT).show();
    }

    private String getModifierString(int modifiers) {
        if (modifiers == 0) return "";

        List<String> mods = new ArrayList<>();
        if ((modifiers & ShortcutProfile.MOD_CTRL) != 0) mods.add("Ctrl");
        if ((modifiers & ShortcutProfile.MOD_SHIFT) != 0) mods.add("Shift");
        if ((modifiers & ShortcutProfile.MOD_ALT) != 0) mods.add("Alt");
        if ((modifiers & ShortcutProfile.MOD_WIN) != 0) mods.add("Win");

        return String.join("+", mods);
    }

    /**
     * Map HID usage code to key name matching CH9329MSKBMap
     */
    private String getKeyString(int keyCode) {
        switch (keyCode) {
            // Letters (a-z)
            case 4: return "a"; case 5: return "b"; case 6: return "c"; case 7: return "d";
            case 8: return "e"; case 9: return "f"; case 10: return "g"; case 11: return "h";
            case 12: return "i"; case 13: return "j"; case 14: return "k"; case 15: return "l";
            case 16: return "m"; case 17: return "n"; case 18: return "o"; case 19: return "p";
            case 20: return "q"; case 21: return "r"; case 22: return "s"; case 23: return "t";
            case 24: return "u"; case 25: return "v"; case 26: return "w"; case 27: return "x";
            case 28: return "y"; case 29: return "z";
            // Numbers (1-0)
            case 30: return "1"; case 31: return "2"; case 32: return "3"; case 33: return "4";
            case 34: return "5"; case 35: return "6"; case 36: return "7"; case 37: return "8";
            case 38: return "9"; case 39: return "0";
            // Special keys
            case 40: return "ENTER"; case 41: return "Esc"; case 42: return "BACK";
            case 43: return "TAB"; case 44: return "SPACE";
            // Symbols
            case 45: return "-"; case 46: return "="; case 47: return "["; case 48: return "]";
            case 49: return "\\"; case 51: return ";"; case 52: return "'"; case 53: return "`";
            case 54: return ","; case 55: return "."; case 56: return "/";
            // Function keys
            case 58: return "F1"; case 59: return "F2"; case 60: return "F3"; case 61: return "F4";
            case 62: return "F5"; case 63: return "F6"; case 64: return "F7"; case 65: return "F8";
            case 66: return "F9"; case 67: return "F10"; case 68: return "F11"; case 69: return "F12";
            // Additional keys
            case 70: return "PrtSc"; case 71: return "ScrLk"; case 72: return "Pause"; case 73: return "Ins";
            // Navigation
            case 74: return "Home"; case 75: return "PgUp"; case 76: return "Delete";
            case 77: return "End"; case 78: return "PgDn";
            case 79: return "DPAD_RIGHT"; case 80: return "DPAD_LEFT";
            case 81: return "DPAD_DOWN"; case 82: return "DPAD_UP";
            // Caps Lock and Num Lock
            case 57: return "CAPS_LOCK";
            case 83: return "NumLk";
            // Numpad operators
            case 84: return "NUMPAD_DIVIDE"; case 85: return "NUMPAD_MULTIPLY";
            case 86: return "NUMPAD_SUBTRACT"; case 87: return "NUMPAD_ADD";
            case 88: return "ENTER"; // Numpad Enter
            // Numpad digits (must map to NUMPAD_x, not plain digits)
            case 89: return "NUMPAD_1"; case 90: return "NUMPAD_2";
            case 91: return "NUMPAD_3"; case 92: return "NUMPAD_4";
            case 93: return "NUMPAD_5"; case 94: return "NUMPAD_6";
            case 95: return "NUMPAD_7"; case 96: return "NUMPAD_8";
            case 97: return "NUMPAD_9"; case 98: return "NUMPAD_0";
            case 99: return "."; // Numpad decimal point
            default: return null;
        }
    }

    // === Dialogs ===

    private void showCreateProfileDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_profile, null);
        EditText etName = dialogView.findViewById(R.id.et_profile_name);
        EditText etDescription = dialogView.findViewById(R.id.et_profile_description);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.shortcut_hub_new_config)
                .setView(dialogView)
                .setPositiveButton(R.string.shortcut_create, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String desc = etDescription.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), R.string.shortcut_enter_name, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ShortcutProfile newProfile = profileManager.createProfile(name, desc);
                    loadData();
                    Toast.makeText(getContext(), getString(R.string.shortcut_hub_created_format, name), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.shortcut_dialog_cancel, null)
                .show();
    }

    private void showImportDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.shortcut_hub_import_config)
                .setItems(new String[]{
                        getString(R.string.shortcut_paste_json_text),
                        getString(R.string.shortcut_hub_import_from_file)
                }, (dialog, which) -> {
                    if (which == 0) {
                        showPasteJsonImportDialog();
                    } else {
                        importFileLauncher.launch("*/*");
                    }
                })
                .setNegativeButton(R.string.shortcut_dialog_cancel, null)
                .show();
    }

    private void showPasteJsonImportDialog() {
        android.widget.EditText editText = new android.widget.EditText(getContext());
        editText.setMinLines(5);
        editText.setHint(R.string.shortcut_paste_json_hint);
        editText.setGravity(android.view.Gravity.TOP);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.shortcut_hub_import_config)
                .setView(editText)
                .setPositiveButton(R.string.shortcut_import, (dialog, which) -> {
                    String json = editText.getText().toString().trim();
                    if (json.isEmpty()) {
                        Toast.makeText(getContext(), R.string.shortcut_json_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    handleImportJson(json);
                })
                .setNegativeButton(R.string.shortcut_dialog_cancel, null)
                .show();
    }

    /**
     * Import JSON: auto-detect single profile or array of profiles.
     */
    private void handleImportJson(String json) {
        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("[")) {
                List<ShortcutProfile> imported = profileManager.importProfiles(json);
                if (imported != null && !imported.isEmpty()) {
                    loadData();
                    Toast.makeText(getContext(),
                            getString(R.string.shortcut_hub_import_success_format, imported.size()), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), R.string.shortcut_hub_import_failed, Toast.LENGTH_SHORT).show();
                }
            } else {
                ShortcutProfile imported = profileManager.importProfile(json);
                if (imported != null) {
                    loadData();
                    Toast.makeText(getContext(), getString(R.string.shortcut_hub_import_success_name_format, imported.name), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), R.string.shortcut_hub_import_failed, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.shortcut_hub_json_format_error, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void importFromFile(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is == null) {
                Toast.makeText(getContext(), R.string.shortcut_cannot_read_file, Toast.LENGTH_SHORT).show();
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            handleImportJson(sb.toString());
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.shortcut_hub_read_failed_format, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void showProfileOverflowMenu(ShortcutProfile profile, View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(getContext(), anchor);
        popup.getMenu().add(0, 1, 0, R.string.shortcut_hub_activate);
        popup.getMenu().add(0, 2, 1, R.string.shortcut_hub_copy);
        popup.getMenu().add(0, 3, 2, R.string.shortcut_hub_export);
        if (!profile.builtIn) {
            popup.getMenu().add(0, 4, 3, R.string.shortcut_hub_delete_label);
        }

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    profileManager.setActiveProfile(profile.id);
                    loadData();
                    Toast.makeText(getContext(), getString(R.string.shortcut_hub_activated_format, profile.name), Toast.LENGTH_SHORT).show();
                    return true;
                case 2:
                    ShortcutProfile copy = profileManager.duplicateProfile(profile.id);
                    if (copy != null) {
                        loadData();
                        Toast.makeText(getContext(), R.string.shortcut_hub_copied, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                case 3:
                    showExportProfileDialog(profile);
                    return true;
                case 4:
                    confirmDeleteProfile(profile);
                    return true;
            }
            return false;
        });

        popup.show();
    }

    private void showShortcutOverflowMenu(Shortcut shortcut, View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(getContext(), anchor);
        popup.getMenu().add(0, 1, 0, R.string.shortcut_hub_edit);
        popup.getMenu().add(0, 2, 1, R.string.shortcut_hub_delete_label);

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    showCreateShortcutDialog(shortcut);
                    return true;
                case 2:
                    confirmDeleteShortcut(shortcut);
                    return true;
            }
            return false;
        });

        popup.show();
    }

    private void showExportProfileDialog(ShortcutProfile profile) {
        String json = profileManager.exportProfile(profile.id);

        android.widget.EditText editText = new android.widget.EditText(getContext());
        editText.setText(json);
        editText.setMinLines(8);
        editText.setFocusable(true);
        editText.setLongClickable(true);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.shortcut_hub_export_config)
                .setMessage(R.string.shortcut_hub_long_press_to_copy)
                .setView(editText)
                .setPositiveButton(R.string.shortcut_hub_export_to_file, (dialog, which) -> {
                    pendingExportProfile = profile;
                    String filename = profile.name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_") + ".json";
                    exportFileLauncher.launch(filename);
                })
                .setNegativeButton(R.string.shortcut_hub_close, null)
                .show();
    }

    /**
     * Export profile JSON to a file.
     */
    private void exportToFile(Uri uri, ShortcutProfile profile) {
        try {
            String json = profileManager.exportProfile(profile.id);
            if (json == null) {
                Toast.makeText(getContext(), R.string.shortcut_hub_export_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            OutputStream os = requireContext().getContentResolver().openOutputStream(uri);
            if (os == null) {
                Toast.makeText(getContext(), R.string.shortcut_hub_cannot_write_file, Toast.LENGTH_SHORT).show();
                return;
            }
            os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            os.close();
            Toast.makeText(getContext(), getString(R.string.shortcut_hub_exported_format, profile.name), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.shortcut_hub_export_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Export all profiles JSON to a file.
     */
    private void exportAllToFile(Uri uri) {
        try {
            String json = profileManager.exportAllProfiles();
            if (json == null) {
                Toast.makeText(getContext(), R.string.shortcut_hub_export_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            OutputStream os = requireContext().getContentResolver().openOutputStream(uri);
            if (os == null) {
                Toast.makeText(getContext(), R.string.shortcut_hub_cannot_write_file, Toast.LENGTH_SHORT).show();
                return;
            }
            os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            os.close();
            Toast.makeText(getContext(), R.string.shortcut_hub_exported_all, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.shortcut_hub_export_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Launch file export dialog for all profiles.
     */
    private void exportAllProfiles() {
        isExportingAllProfiles = true;
        pendingExportProfile = null;
        exportFileLauncher.launch("all_shortcut_profiles.json");
    }

    private void confirmDeleteProfile(ShortcutProfile profile) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.shortcut_hub_confirm_delete)
                .setMessage(getString(R.string.shortcut_hub_delete_profile_message, profile.name))
                .setPositiveButton(R.string.shortcut_hub_delete_label, (dialog, which) -> {
                    profileManager.deleteProfile(profile.id);
                    loadData();
                    Toast.makeText(getContext(), R.string.shortcut_hub_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.shortcut_dialog_cancel, null)
                .show();
    }

    private void confirmDeleteShortcut(Shortcut shortcut) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.shortcut_hub_confirm_delete)
                .setMessage(getString(R.string.shortcut_hub_delete_shortcut_message, shortcut.name))
                .setPositiveButton(R.string.shortcut_hub_delete_label, (dialog, which) -> {
                    // Find and remove the shortcut from all categories
                    boolean deleted = false;
                    for (ShortcutProfile.ShortcutCategory category : selectedProfile.categories) {
                        for (int i = 0; i < category.shortcuts.size(); i++) {
                            Shortcut s = category.shortcuts.get(i);
                            if (s.id.equals(shortcut.id)) {
                                category.shortcuts.remove(i);
                                deleted = true;
                                break;
                            }
                        }
                        if (deleted) break;
                    }

                    // If not found in categories, try removing from flat list
                    if (!deleted && selectedProfile.shortcuts != null) {
                        for (int i = 0; i < selectedProfile.shortcuts.size(); i++) {
                            Shortcut s = selectedProfile.shortcuts.get(i);
                            if (s.id.equals(shortcut.id)) {
                                selectedProfile.shortcuts.remove(i);
                                deleted = true;
                                break;
                            }
                        }
                    }

                    if (deleted) {
                        profileManager.updateProfile(selectedProfile);
                        showDetailPanel(selectedProfile);
                        Toast.makeText(getContext(), R.string.shortcut_hub_deleted, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), R.string.shortcut_hub_delete_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.shortcut_dialog_cancel, null)
                .show();
    }

    private void showCreateShortcutDialog(Shortcut existing) {
        CreateShortcutDialogFragment dialog = CreateShortcutDialogFragment.newInstance(
                selectedProfile != null ? selectedProfile.id : null,
                existing
        );
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "create_shortcut");
    }

    @Override
    public void onShortcutCreated(ShortcutProfile.Shortcut shortcut) {
        // Reload and refresh
        loadData();
        if (selectedProfile != null) {
            selectedProfile = profileManager.getProfileById(selectedProfile.id);
            if (selectedProfile != null) {
                showDetailPanel(selectedProfile);
            }
        }
        Toast.makeText(getContext(), R.string.shortcut_hub_saved, Toast.LENGTH_SHORT).show();
    }
}
