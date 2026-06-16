package com.openterface.AOS.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.openterface.AOS.R;
import com.openterface.AOS.manager.ThemeManager;

import java.util.Arrays;
import java.util.List;

public class ThemeColorAdapter extends RecyclerView.Adapter<ThemeColorAdapter.ViewHolder> {

    private final List<String> colorFamilies;
    private String selectedFamily;
    private OnColorSelectedListener listener;

    public interface OnColorSelectedListener {
        void onColorSelected(String family);
    }

    public ThemeColorAdapter(Context context, String currentFamily, OnColorSelectedListener listener) {
        this.colorFamilies = Arrays.asList(ThemeManager.getAllFamilies());
        this.selectedFamily = currentFamily;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_theme_color, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String family = colorFamilies.get(position);
        String displayName = ThemeManager.getFamilyDisplayName(family);
        int previewColor = ThemeManager.getFamilyPreviewColor(holder.itemView.getContext(), family);

        holder.tvColorName.setText(displayName);
        holder.viewColorPreview.setBackgroundColor(previewColor);

        // Show selected indicator
        if (family.equals(selectedFamily)) {
            holder.ivSelected.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundResource(R.drawable.bg_theme_color_selected);
        } else {
            holder.ivSelected.setVisibility(View.GONE);
            holder.itemView.setBackgroundResource(R.drawable.bg_theme_color_normal);
        }

        holder.itemView.setOnClickListener(v -> {
            selectedFamily = family;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onColorSelected(family);
            }
        });
    }

    @Override
    public int getItemCount() {
        return colorFamilies.size();
    }

    public String getSelectedFamily() {
        return selectedFamily;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View viewColorPreview;
        ImageView ivSelected;
        TextView tvColorName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColorPreview = itemView.findViewById(R.id.view_color_preview);
            ivSelected = itemView.findViewById(R.id.iv_selected);
            tvColorName = itemView.findViewById(R.id.tv_color_name);
        }
    }
}
