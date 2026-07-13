package com.openterface.AOS.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.openterface.AOS.R;
import com.openterface.AOS.model.ShortcutProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying shortcut profiles.
 */
public class ShortcutProfileAdapter extends RecyclerView.Adapter<ShortcutProfileAdapter.ViewHolder> {

    private List<ShortcutProfile> profiles = new ArrayList<>();
    private OnProfileActionListener listener;

    public interface OnProfileActionListener {
        void onProfileClick(ShortcutProfile profile);
        void onProfileEdit(ShortcutProfile profile);
        void onProfileDelete(ShortcutProfile profile);
        void onProfileExport(ShortcutProfile profile);
        void onProfileDuplicate(ShortcutProfile profile);
    }

    public ShortcutProfileAdapter(OnProfileActionListener listener) {
        this.listener = listener;
    }

    public void setProfiles(List<ShortcutProfile> profiles) {
        this.profiles = profiles != null ? profiles : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shortcut_profile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShortcutProfile profile = profiles.get(position);

        holder.tvName.setText(profile.name);
        holder.tvDescription.setText(profile.description);
        holder.tvShortcutCount.setText(String.format(holder.itemView.getContext().getString(R.string.shortcut_count), profile.getShortcutCount()));

        // Show/hide edit and delete buttons based on builtIn flag
        if (profile.builtIn) {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProfileClick(profile);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProfileEdit(profile);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProfileDelete(profile);
            }
        });

        holder.btnExport.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProfileExport(profile);
            }
        });

        holder.btnDuplicate.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProfileDuplicate(profile);
            }
        });
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvDescription;
        TextView tvShortcutCount;
        ImageButton btnEdit;
        ImageButton btnDelete;
        ImageButton btnExport;
        ImageButton btnDuplicate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_profile_name);
            tvDescription = itemView.findViewById(R.id.tv_profile_description);
            tvShortcutCount = itemView.findViewById(R.id.tv_shortcut_count);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnExport = itemView.findViewById(R.id.btn_export);
            btnDuplicate = itemView.findViewById(R.id.btn_duplicate);
        }
    }
}
