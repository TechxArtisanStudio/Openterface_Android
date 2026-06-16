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
 * Adapter for profile list in the Hub floating panel (Panel 1).
 */
public class HubProfileAdapter extends RecyclerView.Adapter<HubProfileAdapter.ViewHolder> {

    private List<ShortcutProfile> profiles = new ArrayList<>();
    private String activeProfileId;
    private OnProfileActionListener listener;

    public interface OnProfileActionListener {
        void onProfileSelected(ShortcutProfile profile);
        void onProfileLongPress(ShortcutProfile profile, View anchor);
    }

    public HubProfileAdapter(OnProfileActionListener listener) {
        this.listener = listener;
    }

    public void setProfiles(List<ShortcutProfile> profiles, String activeProfileId) {
        this.profiles = profiles != null ? profiles : new ArrayList<>();
        this.activeProfileId = activeProfileId;
        notifyDataSetChanged();
    }

    public void setActiveProfileId(String id) {
        this.activeProfileId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hub_profile_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShortcutProfile profile = profiles.get(position);
        boolean isActive = profile.id != null && profile.id.equals(activeProfileId);

        holder.tvName.setText(profile.name);
        holder.tvDescription.setText(profile.description != null ? profile.description : "");
        holder.tvCount.setText(profile.getShortcutCount() + " 个");

        // Active indicator
        holder.ivActive.setVisibility(isActive ? View.VISIBLE : View.GONE);
        holder.spacer.setVisibility(isActive ? View.GONE : View.VISIBLE);

        // Click → select / show detail
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProfileSelected(profile);
        });

        // Long press → overflow menu
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onProfileLongPress(profile, v);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivActive;
        View spacer;
        TextView tvName;
        TextView tvDescription;
        TextView tvCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivActive = itemView.findViewById(R.id.iv_active_indicator);
            spacer = itemView.findViewById(R.id.active_spacer);
            tvName = itemView.findViewById(R.id.tv_profile_name);
            tvDescription = itemView.findViewById(R.id.tv_profile_description);
            tvCount = itemView.findViewById(R.id.tv_shortcut_count);
        }
    }
}
