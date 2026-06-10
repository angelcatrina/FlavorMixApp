package com.example.flavormix.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.flavormix.RecipeDetailActivity;
import com.example.flavormix.adapter.RecipeAdapter;
import com.example.flavormix.database.RecipeDbHelper;
import com.example.flavormix.databinding.FragmentFavoriteListBinding;
import com.example.flavormix.model.Recipe;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteListFragment extends Fragment {

    private FragmentFavoriteListBinding binding;
    private RecipeAdapter adapter;
    private RecipeDbHelper dbHelper;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoriteListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new RecipeDbHelper(requireContext());

        binding.btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().finish();
            }
        });

        setupRecyclerView();
        loadFavorites();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void setupRecyclerView() {
        adapter = new RecipeAdapter(
                recipe -> {
                    Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
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
        binding.rvFavorites.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvFavorites.setAdapter(adapter);
    }

    private void loadFavorites() {
        executor.execute(() -> {
            List<Recipe> favorites = dbHelper.getAllFavorites();
            mainHandler.post(() -> {
                if (!isAdded()) return;

                if (favorites.isEmpty()) {
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                    binding.rvFavorites.setVisibility(View.GONE);
                    binding.tvFavoriteCount.setText("0 resep");
                } else {
                    binding.layoutEmpty.setVisibility(View.GONE);
                    binding.rvFavorites.setVisibility(View.VISIBLE);
                    adapter.updateData(favorites);
                    binding.tvFavoriteCount.setText(favorites.size() + " resep");
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}