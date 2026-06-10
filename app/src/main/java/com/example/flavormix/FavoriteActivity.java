
package com.example.flavormix;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

// INI JUGA SUDAH DIPERBAIKI:
import com.example.flavormix.adapter.RecipeAdapter;
import com.example.flavormix.database.RecipeDbHelper;
import com.example.flavormix.databinding.ActivityFavoriteBinding;
import com.example.flavormix.model.Recipe;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteActivity extends AppCompatActivity {

    private ActivityFavoriteBinding binding;
    private RecipeAdapter adapter;
    private RecipeDbHelper dbHelper;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoriteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Resep Favorit");
        }

        dbHelper = new RecipeDbHelper(this);
        setupRecyclerView();
        loadFavorites();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void setupRecyclerView() {
        adapter = new RecipeAdapter(
                recipe -> {
                    Intent intent = new Intent(this, RecipeDetailActivity.class);
                    intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID,    recipe.getId());
                    intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_NAME,  recipe.getName());
                    intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_THUMB, recipe.getThumbnailUrl());
                    startActivity(intent);
                },
                (recipe, isFavorite) -> executor.execute(() -> {
                    if (isFavorite) {
                        dbHelper.addFavorite(recipe);
                    } else {
                        dbHelper.removeFavorite(recipe.getId());
                        mainHandler.post(this::loadFavorites);
                    }
                })
        );
        binding.rvFavorites.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvFavorites.setAdapter(adapter);
    }

    private void loadFavorites() {
        executor.execute(() -> {
            List<Recipe> favorites = dbHelper.getAllFavorites();
            mainHandler.post(() -> {
                if (favorites.isEmpty()) {
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                    binding.rvFavorites.setVisibility(View.GONE);
                } else {
                    binding.layoutEmpty.setVisibility(View.GONE);
                    binding.rvFavorites.setVisibility(View.VISIBLE);
                    adapter.updateData(favorites);
                }
            });
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}