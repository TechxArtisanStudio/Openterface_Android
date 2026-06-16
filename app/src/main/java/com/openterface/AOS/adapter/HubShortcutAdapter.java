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
import com.openterface.AOS.model.ShortcutProfile.Shortcut;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for shortcut items in the Hub detail panel.
 * Supports two presentation modes: card (grid) and row (list).
 */
public class HubShortcutAdapter extends RecyclerView.Adapter<HubShortcutAdapter.ViewHolder> {

    public static final int MODE_CARD = 0;  // Grid layout (favorites)
    public static final int MODE_ROW = 1;   // List layout (browse)

    private List<Shortcut> shortcuts = new ArrayList<>();
    private List<String> favoriteIds = new ArrayList<>();
    private int mode;
    private OnShortcutActionListener listener;

    public interface OnShortcutActionListener {
        void onShortcutClick(Shortcut shortcut);
        void onShortcutLongClick(Shortcut shortcut);
        void onToggleFavorite(Shortcut shortcut);
        void onOverflowClick(Shortcut shortcut, View anchor);
    }

    public HubShortcutAdapter(int mode, OnShortcutActionListener listener) {
        this.mode = mode;
        this.listener = listener;
    }

    public void setShortcuts(List<Shortcut> shortcuts) {
        this.shortcuts = shortcuts != null ? shortcuts : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setFavoriteIds(List<String> ids) {
        this.favoriteIds = ids != null ? ids : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setMode(int mode) {
        this.mode = mode;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return mode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = (viewType == MODE_CARD)
                ? R.layout.item_hub_shortcut_card
                : R.layout.item_hub_shortcut_row;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Shortcut shortcut = shortcuts.get(position);

        holder.tvName.setText(shortcut.name != null ? shortcut.name : "");
        holder.tvLabel.setText(shortcut.label != null ? shortcut.label : "");

        if (holder.viewType == MODE_ROW) {
            // Favorite star
            boolean isFav = favoriteIds.contains(shortcut.id);
            if (holder.btnFavorite != null) {
                holder.btnFavorite.setImageResource(
                        isFav ? R.drawable.ic_star_filled_24 : R.drawable.ic_star_border_24);
                holder.btnFavorite.setOnClickListener(v -> {
                    if (listener != null) listener.onToggleFavorite(shortcut);
                });
            }
            // Overflow
            if (holder.btnOverflow != null) {
                holder.btnOverflow.setOnClickListener(v -> {
                    if (listener != null) listener.onOverflowClick(shortcut, v);
                });
            }
        }

        // Click → execute shortcut
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onShortcutClick(shortcut);
        });

        // Long click
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onShortcutLongClick(shortcut);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return shortcuts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        int viewType;
        ImageView ivIcon;
        TextView tvName;
        TextView tvLabel;
        ImageButton btnFavorite;
        ImageButton btnOverflow;

        ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            ivIcon = itemView.findViewById(R.id.iv_shortcut_icon);
            tvName = itemView.findViewById(R.id.tv_shortcut_name);
            tvLabel = itemView.findViewById(R.id.tv_shortcut_label);
            if (viewType == MODE_ROW) {
                btnFavorite = itemView.findViewById(R.id.btn_favorite);
                btnOverflow = itemView.findViewById(R.id.btn_overflow);
            }
        }
    }
}
