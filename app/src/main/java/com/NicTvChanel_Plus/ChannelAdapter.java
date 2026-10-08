package com.NicTvChanel_Plus;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CATEGORY = 0;
    private static final int TYPE_CHANNEL = 1;
    private final List<Object> items;
    private final OnChannelClickListener listener;

    public interface OnChannelClickListener {
        void onChannelClick(Channel channel);
    }

    public ChannelAdapter(List<Object> items, OnChannelClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof String) ? TYPE_CATEGORY : TYPE_CHANNEL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CATEGORY) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new CategoryViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ChannelViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CategoryViewHolder) {
            String category = (String) items.get(position);
            TextView txt = ((CategoryViewHolder) holder).textView;
            txt.setText(category.toUpperCase());
            txt.setTextColor(Color.parseColor("#FFD700")); // Dorado para categorías
            txt.setTextSize(35f); // 3 veces más grande
            txt.setPadding(20, 40, 20, 10);
        } else {
            Channel channel = (Channel) items.get(position);
            TextView txt = ((ChannelViewHolder) holder).textView;
            txt.setText(channel.getName());
            txt.setTextColor(Color.WHITE);
            txt.setTextSize(30f); // Letra grande para TV
            txt.setFocusable(true);
            txt.setFocusableInTouchMode(true);

            // Efecto visual al seleccionar con el control remoto
            txt.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) v.setBackgroundColor(Color.parseColor("#4B0082"));
                else v.setBackgroundColor(Color.TRANSPARENT);
            });

            holder.itemView.setOnClickListener(v -> listener.onChannelClick(channel));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        CategoryViewHolder(View v) { super(v); textView = v.findViewById(android.R.id.text1); }
    }

    static class ChannelViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ChannelViewHolder(View v) { super(v); textView = v.findViewById(android.R.id.text1); }
    }
}