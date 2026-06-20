package com.example.slotsmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slotsmart.R;
import com.example.slotsmart.model.EntityItem;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class EntityAdapter extends RecyclerView.Adapter<EntityAdapter.ViewHolder> {

    public interface OnActionListener {
        void onEdit(EntityItem item);
        void onDelete(EntityItem item);
    }

    private List<EntityItem> allItems = new ArrayList<>();
    private List<EntityItem> filtered = new ArrayList<>();
    private OnActionListener listener;

    public EntityAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<EntityItem> items) {
        allItems = new ArrayList<>(items);
        filtered = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        String q = query.toLowerCase().trim();
        filtered.clear();
        if (q.isEmpty()) {
            filtered.addAll(allItems);
        } else {
            for (EntityItem item : allItems) {
                if (item.title.toLowerCase().contains(q)
                        || (item.subtitle != null && item.subtitle.toLowerCase().contains(q))) {
                    filtered.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entity, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        EntityItem item = filtered.get(pos);
        h.tvTitle.setText(item.title);
        h.tvSubtitle.setText(item.subtitle != null ? item.subtitle : "");
        if (item.badge != null && !item.badge.isEmpty()) {
            h.tvBadge.setVisibility(View.VISIBLE);
            h.tvBadge.setText(item.badge);
        } else {
            h.tvBadge.setVisibility(View.GONE);
        }
        h.btnEdit.setOnClickListener(v -> listener.onEdit(item));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return filtered.size();
    }

    public int getTotalCount() {
        return allItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvBadge;
        MaterialButton btnEdit, btnDelete;

        ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvSubtitle = v.findViewById(R.id.tvSubtitle);
            tvBadge = v.findViewById(R.id.tvBadge);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
