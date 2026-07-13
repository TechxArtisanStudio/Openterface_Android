package com.openterface.AOS.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.openterface.AOS.R;
import com.openterface.AOS.adapter.ShortcutProfileAdapter;
import com.openterface.AOS.manager.ShortcutProfileManager;
import com.openterface.AOS.model.ShortcutProfile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * Fragment for managing shortcut profiles.
 * Supports CRUD operations, JSON import/export.
 */
public class ShortcutManagerFragment extends Fragment implements ShortcutProfileAdapter.OnProfileActionListener {

    private static final int REQUEST_IMPORT_PROFILE = 1001;
    private static final int REQUEST_EXPORT_PROFILE = 1002;

    private RecyclerView recyclerView;
    private ShortcutProfileAdapter adapter;
    private ShortcutProfileManager profileManager;
    private TextView tvEmpty;
    private View btnAddProfile;
    private View btnImport;
    private ShortcutProfile pendingExportProfile;  // Store profile being exported

    public static ShortcutManagerFragment newInstance() {
        return new ShortcutManagerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shortcut_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileManager = ShortcutProfileManager.getInstance(requireContext());

        recyclerView = view.findViewById(R.id.recycler_profiles);
        tvEmpty = view.findViewById(R.id.tv_empty);
        btnAddProfile = view.findViewById(R.id.btn_add_profile);
        btnImport = view.findViewById(R.id.btn_import);

        setupRecyclerView();
        setupButtons();
        loadProfiles();
    }

    private void setupRecyclerView() {
        adapter = new ShortcutProfileAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        if (btnAddProfile != null) {
            btnAddProfile.setOnClickListener(v -> showCreateProfileDialog());
        }

        if (btnImport != null) {
            btnImport.setOnClickListener(v -> importProfile());
        }
    }

    private void loadProfiles() {
        List<ShortcutProfile> profiles = profileManager.getAllProfiles();
        adapter.setProfiles(profiles);

        if (profiles.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showCreateProfileDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_profile, null);
        EditText etName = dialogView.findViewById(R.id.et_profile_name);
        EditText etDescription = dialogView.findViewById(R.id.et_profile_description);

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.shortcut_new_config)
                .setView(dialogView)
                .setPositiveButton(R.string.shortcut_create, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), R.string.shortcut_enter_name, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    profileManager.createProfile(name, description.isEmpty() ? getString(R.string.shortcut_custom_config) : description);
                    loadProfiles();
                    Toast.makeText(getContext(), R.string.shortcut_config_created, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.shortcut_cancel, null)
                .show();
    }

    private void showEditProfileDialog(ShortcutProfile profile) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_profile, null);
        EditText etName = dialogView.findViewById(R.id.et_profile_name);
        EditText etDescription = dialogView.findViewById(R.id.et_profile_description);

        etName.setText(profile.name);
        etDescription.setText(profile.description);

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.shortcut_edit_config)
                .setView(dialogView)
                .setPositiveButton(R.string.shortcut_save, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), R.string.shortcut_enter_name, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    profile.name = name;
                    profile.description = description.isEmpty() ? getString(R.string.shortcut_custom_config) : description;
                    profileManager.updateProfile(profile);
                    loadProfiles();
                    Toast.makeText(getContext(), R.string.shortcut_config_updated, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.shortcut_cancel, null)
                .show();
    }

    private void showDeleteConfirmDialog(ShortcutProfile profile) {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.shortcut_delete_config)
                .setMessage(getString(R.string.shortcut_delete_confirm, profile.name))
                .setPositiveButton(R.string.shortcut_delete, (dialog, which) -> {
                    profileManager.deleteProfile(profile.id);
                    loadProfiles();
                    Toast.makeText(getContext(), R.string.shortcut_config_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.shortcut_cancel, null)
                .show();
    }

    private void importProfile() {
        // Show dialog with two options: paste JSON or file picker
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.shortcut_import_config)
            .setItems(new String[]{getString(R.string.shortcut_paste_json_text), getString(R.string.shortcut_browse_file)}, (dialog, which) -> {
                if (which == 0) {
                    showPasteJsonDialog();
                } else {
                    openFilePicker();
                }
            })
            .setNegativeButton(R.string.shortcut_cancel, null)
            .show();
    }

    private void showPasteJsonDialog() {
        android.widget.EditText editText = new android.widget.EditText(requireContext());
        editText.setMinLines(8);
        editText.setGravity(android.view.Gravity.TOP);
        editText.setHint(R.string.shortcut_paste_json_hint);

        new android.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.shortcut_paste_json)
            .setView(editText)
            .setPositiveButton(R.string.shortcut_import, (dialog, which) -> {
                String json = editText.getText().toString().trim();
                if (json.isEmpty()) {
                    Toast.makeText(getContext(), R.string.shortcut_json_empty, Toast.LENGTH_SHORT).show();
                    return;
                }

                ShortcutProfile imported = profileManager.importProfile(json);
                if (imported != null) {
                    loadProfiles();
                    Toast.makeText(getContext(), getString(R.string.shortcut_import_success, imported.name), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), R.string.shortcut_import_failed_format, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.shortcut_cancel, null)
            .show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT_PROFILE);
    }

    private void exportProfile(ShortcutProfile profile) {
        pendingExportProfile = profile;  // Store for onActivityResult
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, profile.name + ".json");
        startActivityForResult(intent, REQUEST_EXPORT_PROFILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != getActivity().RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null) {
            return;
        }

        if (requestCode == REQUEST_IMPORT_PROFILE) {
            importProfileFromUri(uri);
        } else if (requestCode == REQUEST_EXPORT_PROFILE) {
            exportProfileToUri(uri, pendingExportProfile);
            pendingExportProfile = null;
        }
    }

    private void exportProfileToUri(Uri uri, ShortcutProfile profile) {
        if (profile == null) {
            Toast.makeText(getContext(), R.string.shortcut_export_failed_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String json = profileManager.exportProfile(profile.id);
            if (json == null) {
                Toast.makeText(getContext(), R.string.shortcut_export_failed_serialize, Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
            if (outputStream == null) {
                Toast.makeText(getContext(), R.string.shortcut_export_failed_write, Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            writer.write(json);
            writer.close();

            Toast.makeText(getContext(), getString(R.string.shortcut_config_exported, profile.name), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.shortcut_export_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void importProfileFromUri(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(getContext(), R.string.shortcut_cannot_read_file, Toast.LENGTH_SHORT).show();
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();

            String json = jsonBuilder.toString();
            ShortcutProfile imported = profileManager.importProfile(json);

            if (imported != null) {
                loadProfiles();
                Toast.makeText(getContext(), getString(R.string.shortcut_config_imported, imported.name), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), R.string.shortcut_import_failed_file_format, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.shortcut_import_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    // ShortcutProfileAdapter.OnProfileActionListener implementation

    @Override
    public void onProfileClick(ShortcutProfile profile) {
        // Set as active profile
        profileManager.setActiveProfile(profile.id);
        Toast.makeText(getContext(), getString(R.string.shortcut_switched_to, profile.name), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProfileEdit(ShortcutProfile profile) {
        showEditProfileDialog(profile);
    }

    @Override
    public void onProfileDelete(ShortcutProfile profile) {
        if (profile.builtIn) {
            Toast.makeText(getContext(), R.string.shortcut_builtin_cannot_delete, Toast.LENGTH_SHORT).show();
            return;
        }
        showDeleteConfirmDialog(profile);
    }

    @Override
    public void onProfileExport(ShortcutProfile profile) {
        exportProfile(profile);
    }

    @Override
    public void onProfileDuplicate(ShortcutProfile profile) {
        ShortcutProfile duplicate = profileManager.duplicateProfile(profile.id);
        if (duplicate != null) {
            loadProfiles();
            Toast.makeText(getContext(), R.string.shortcut_config_copied, Toast.LENGTH_SHORT).show();
        }
    }
}
