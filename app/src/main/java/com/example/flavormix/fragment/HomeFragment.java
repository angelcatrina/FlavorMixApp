package com.example.flavormix.fragment;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.flavormix.R;
import com.example.flavormix.RecipeDetailActivity;
import com.example.flavormix.adapter.RecipeAdapter;
import com.example.flavormix.api.RetrofitClient;
import com.example.flavormix.database.RecipeDbHelper;
import com.example.flavormix.databinding.FragmentHomeBinding;
import com.example.flavormix.model.Recipe;
import com.example.flavormix.model.RecipeListResponse;
import com.example.flavormix.utils.NetworkUtils;
import com.example.flavormix.utils.PreferencesHelper; // ← pakai PreferencesHelper yang sama dengan MainActivity

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {


    private FragmentHomeBinding binding;
    private RecipeAdapter adapter;
    private RecipeDbHelper dbHelper;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String selectedCategory = "";
    private static final int PAGE_SIZE = 20;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new RecipeDbHelper(requireContext());
        setupRecyclerView();
        setupCategoryChips();
        setupButtons();
        updateThemeIcon();
        loadRecipes(false);
    }

    private void setupRecyclerView() {
        adapter = createRecipeAdapter();
        binding.rvRecipes.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvRecipes.setAdapter(adapter);
    }

    private RecipeAdapter createRecipeAdapter() {
        return new RecipeAdapter(
                recipe -> {
                    Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
                    intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
                    startActivity(intent);
                },
                (recipe, isFavorite) -> executor.execute(() -> {
                    if (isFavorite) dbHelper.addFavorite(recipe);
                    else dbHelper.removeFavorite(recipe.getId());
                    mainHandler.post(() -> Toast.makeText(getContext(),
                            isFavorite ? "Ditambahkan ke favorit" : "Dihapus dari favorit",
                            Toast.LENGTH_SHORT).show());
                })
        );
    }

    private void setupCategoryChips() {
        binding.chipAll.setChecked(true);
        binding.chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int chipId = checkedIds.get(0);

            if      (chipId == R.id.chipAll)       selectedCategory = "";
            else if (chipId == R.id.chipBreakfast) selectedCategory = "breakfast";
            else if (chipId == R.id.chipLunch)     selectedCategory = "lunch";
            else if (chipId == R.id.chipDinner)    selectedCategory = "dinner";
            else if (chipId == R.id.chipDessert)   selectedCategory = "desserts";

            loadRecipes(false);
        });
    }

    private void setupButtons() {
        binding.btnRetry.setOnClickListener(v -> loadRecipes(true));
        binding.swipeRefresh.setOnRefreshListener(() -> loadRecipes(true));

        binding.btnCariResep.setOnClickListener(v -> {
            if (getActivity() != null) {

                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                        getActivity().findViewById(R.id.bottomNav);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.searchFragment);
                }
            }
        });

        binding.btnToggleTheme.setOnClickListener(v -> {
            boolean currentlyDark = PreferencesHelper.isDarkMode(requireContext());
            boolean setToDark = !currentlyDark;

            PreferencesHelper.setDarkMode(requireContext(), setToDark);

            AppCompatDelegate.setDefaultNightMode(
                    setToDark
                            ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO
            );
        });
    }


    private void updateThemeIcon() {
        boolean dark = isDarkModeActive();
        binding.btnToggleTheme.setImageResource(
                dark ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode
        );
    }

    private boolean isDarkModeActive() {
        int uiMode = requireContext().getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }


    private void loadRecipes(boolean forceRefresh) {
        showLoading(true);
        hideError();

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            loadFromCache();
            return;
        }

        executor.execute(() -> {
            boolean cacheValid = dbHelper.isCacheValid(selectedCategory) && !forceRefresh;
            if (cacheValid) {
                List<Recipe> cached = dbHelper.getCachedRecipes(selectedCategory);
                if (!cached.isEmpty()) {
                    markFavorites(cached);
                    mainHandler.post(() -> {
                        adapter.updateData(cached);
                        showLoading(false);
                        binding.swipeRefresh.setRefreshing(false);
                    });
                    return;
                }
            }
            mainHandler.post(this::fetchFromApi);
        });
    }

    private void fetchFromApi() {
        RetrofitClient.getInstance().getApiService()
                .getRecipes(0, PAGE_SIZE, selectedCategory)
                .enqueue(new Callback<RecipeListResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RecipeListResponse> call,
                                           @NonNull Response<RecipeListResponse> response) {
                        if (!isAdded()) return;
                        showLoading(false);
                        binding.swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Recipe> recipes = response.body().getResults();
                            if (recipes.isEmpty()) {
                                showEmpty();
                                return;
                            }
                            markFavorites(recipes);
                            adapter.updateData(recipes);
                            executor.execute(() -> dbHelper.cacheRecipes(recipes, selectedCategory));
                        } else {
                            showError("Gagal memuat resep (kode: " + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RecipeListResponse> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        showLoading(false);
                        binding.swipeRefresh.setRefreshing(false);
                        loadFromCache();
                    }
                });
    }

    private void loadFromCache() {
        executor.execute(() -> {
            List<Recipe> cached = dbHelper.getCachedRecipes(selectedCategory);
            mainHandler.post(() -> {
                showLoading(false);
                binding.swipeRefresh.setRefreshing(false);
                if (!cached.isEmpty()) {
                    markFavorites(cached);
                    adapter.updateData(cached);
                } else {
                    showError("Tidak ada koneksi & tidak ada data tersimpan");
                }
            });
        });
    }

    private void markFavorites(List<Recipe> recipes) {
        executor.execute(() -> {
            for (Recipe r : recipes) r.setFavorite(dbHelper.isFavorite(r.getId()));
        });
    }


    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.rvRecipes.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmpty() {
        binding.layoutEmptyState.setVisibility(View.VISIBLE);
        binding.rvRecipes.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        binding.layoutError.setVisibility(View.VISIBLE);
        binding.tvError.setText(msg);
        binding.rvRecipes.setVisibility(View.GONE);
    }

    private void hideError() {
        binding.layoutError.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}