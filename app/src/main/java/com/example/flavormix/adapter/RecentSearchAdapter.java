package com.example.flavormix.adapter;

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class RecentSearchAdapter extends RecyclerView.Adapter<RecentSearchAdapter.ViewHolder> {

    private List<String> historyList = new ArrayList<>();
    private final OnSearchItemClickListener listener;

    public interface OnSearchItemClickListener {
        void onSearchClick(String query);
    }

    public RecentSearchAdapter(List<String> historyList, OnSearchItemClickListener listener) {
        this.historyList = (historyList != null) ? historyList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        Chip chip = new Chip(context);
        chip.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        chip.setTextSize(12f);
        chip.setChipMinHeight(80f);
        chip.setChipStartPadding(8f);
        chip.setChipEndPadding(8f);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setClickable(true);
        chip.setFocusable(true);

        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
        );
        lp.setMarginEnd(8);
        chip.setLayoutParams(lp);

        return new ViewHolder(chip);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String query = historyList.get(position);
        holder.chip.setText(query);
        holder.chip.setOnClickListener(v -> {
            if (listener != null) listener.onSearchClick(query);
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void updateData(List<String> newList) {
        this.historyList = (newList != null) ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        Chip chip;

        public ViewHolder(@NonNull Chip chip) {
            super(chip);
            this.chip = chip;
        }
    }
}