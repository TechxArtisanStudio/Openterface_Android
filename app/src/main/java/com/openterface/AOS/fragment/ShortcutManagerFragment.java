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
                .setTitle("新建快捷键配置")
                .setView(dialogView)
                .setPositiveButton("创建", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "请输入配置名称", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    profileManager.createProfile(name, description.isEmpty() ? "自定义配置" : description);
                    loadProfiles();
                    Toast.makeText(getContext(), "配置已创建", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showEditProfileDialog(ShortcutProfile profile) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_profile, null);
        EditText etName = dialogView.findViewById(R.id.et_profile_name);
        EditText etDescription = dialogView.findViewById(R.id.et_profile_description);

        etName.setText(profile.name);
        etDescription.setText(profile.description);

        new AlertDialog.Builder(getContext())
                .setTitle("编辑配置")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "请输入配置名称", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    profile.name = name;
                    profile.description = description.isEmpty() ? "自定义配置" : description;
                    profileManager.updateProfile(profile);
                    loadProfiles();
                    Toast.makeText(getContext(), "配置已更新", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDeleteConfirmDialog(ShortcutProfile profile) {
        new AlertDialog.Builder(getContext())
                .setTitle("删除配置")
                .setMessage("确定要删除 \"" + profile.name + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    profileManager.deleteProfile(profile.id);
                    loadProfiles();
                    Toast.makeText(getContext(), "配置已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void importProfile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT_PROFILE);
    }

    private void exportProfile(ShortcutProfile profile) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
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
            // For now, just show a message - full export implementation would need the profile
            Toast.makeText(getContext(), "导出功能开发中", Toast.LENGTH_SHORT).show();
        }
    }

    private void importProfileFromUri(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(getContext(), "无法读取文件", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "配置已导入: " + imported.name, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "导入失败：文件格式错误", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "导入失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ShortcutProfileAdapter.OnProfileActionListener implementation

    @Override
    public void onProfileClick(ShortcutProfile profile) {
        // Set as active profile
        profileManager.setActiveProfile(profile.id);
        Toast.makeText(getContext(), "已切换到: " + profile.name, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProfileEdit(ShortcutProfile profile) {
        showEditProfileDialog(profile);
    }

    @Override
    public void onProfileDelete(ShortcutProfile profile) {
        if (profile.builtIn) {
            Toast.makeText(getContext(), "内置配置不能删除", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), "配置已复制", Toast.LENGTH_SHORT).show();
        }
    }
}
