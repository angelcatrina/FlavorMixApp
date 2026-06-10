package com.example.flavormix.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

// INI BAGIAN YANG DIPERBAIKI:
import com.example.flavormix.R;
import com.example.flavormix.model.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Recipe recipe);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Recipe recipe, boolean isFavorite);
    }

    private final List<Recipe> recipes = new ArrayList<>();
    private final OnItemClickListener itemClickListener;
    private final OnFavoriteClickListener favoriteClickListener;

    public RecipeAdapter(OnItemClickListener itemClickListener,
                         OnFavoriteClickListener favoriteClickListener) {
        this.itemClickListener    = itemClickListener;
        this.favoriteClickListener = favoriteClickListener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        holder.tvName.setText(recipe.getName());

        Integer time = recipe.getTotalTimeMinutes();
        if (time != null && time > 0) {
            if (time >= 60) {
                int h = time / 60;
                int m = time % 60;
                holder.tvTime.setText(m == 0 ? h + "j" : h + "j " + m + "m");
            } else {
                holder.tvTime.setText(time + "m");
            }
        } else {
            holder.tvTime.setText("N/A");
        }

        if (recipe.getUserRatings() != null && recipe.getUserRatings().getScore() != null) {
            double stars = recipe.getUserRatings().getScore() * 5;
            holder.tvRating.setText(String.format(Locale.US, "%.1f", stars));
        } else {
            holder.tvRating.setText("N/A");
        }

        Glide.with(holder.itemView.getContext())
                .load(recipe.getThumbnailUrl())
                .placeholder(R.drawable.placeholder_recipe)
                .error(R.drawable.placeholder_recipe)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.imgThumbnail);


        updateFavIcon(holder.btnFavorite, recipe.isFavorite());

        holder.btnFavorite.setOnClickListener(v -> {
            boolean newState = !recipe.isFavorite();
            recipe.setFavorite(newState);
            updateFavIcon(holder.btnFavorite, newState);
            favoriteClickListener.onFavoriteClick(recipe, newState);
        });

        holder.itemView.setOnClickListener(v -> itemClickListener.onItemClick(recipe));
    }

    private void updateFavIcon(ImageButton btn, boolean isFav) {
        btn.setImageResource(isFav ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public void updateData(List<Recipe> newRecipes) {
        recipes.clear();
        recipes.addAll(newRecipes);
        notifyDataSetChanged();
    }

    public void addData(List<Recipe> newRecipes) {
        int start = recipes.size();
        recipes.addAll(newRecipes);
        notifyItemRangeInserted(start, newRecipes.size());
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView   imgThumbnail;
        TextView    tvName, tvTime, tvRating;
        ImageButton btnFavorite;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            tvName       = itemView.findViewById(R.id.tvRecipeName);
            tvTime       = itemView.findViewById(R.id.tvTime);
            tvRating     = itemView.findViewById(R.id.tvRating);
            btnFavorite  = itemView.findViewById(R.id.btnFavorite);
        }
    }
}