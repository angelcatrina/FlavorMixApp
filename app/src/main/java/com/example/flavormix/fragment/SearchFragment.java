package com.example.flavormix.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.flavormix.R;
import com.example.flavormix.RecipeDetailActivity;
import com.example.flavormix.adapter.RecipeAdapter;
import com.example.flavormix.adapter.RecentSearchAdapter;
import com.example.flavormix.api.RetrofitClient;
import com.example.flavormix.database.RecipeDbHelper;
import com.example.flavormix.databinding.FragmentSearchBinding;
import com.example.flavormix.model.Recipe;
import com.example.flavormix.model.RecipeListResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";

    private FragmentSearchBinding binding;
    private RecipeAdapter searchAdapter, recommendationAdapter;
    private RecentSearchAdapter historyAdapter;
    private RecipeDbHelper dbHelper;
    private List<Recipe> allSearchResults = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new RecipeDbHelper(requireContext());
        setupAdapters();
        setupFilters();
        showRecommendationMode();
        loadRecommendations();

        binding.btnBack.setOnClickListener(v -> goBackToRecommendation());

        binding.etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) switchToSearchMode();
        });

        binding.btnSearch.setOnClickListener(v -> {
            String query = binding.etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                saveSearchHistory(query);
                performSearch(query);
            }
        });


        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    allSearchResults.clear();
                    searchAdapter.updateData(new ArrayList<>());
                    hideEmptyState();
                    switchToSearchMode();
                }
            }
        });
    }


    private void goBackToRecommendation() {
        allSearchResults.clear();
        searchAdapter.updateData(new ArrayList<>());
        hideEmptyState();
        binding.etSearch.setText("");
        binding.etSearch.clearFocus();
        showRecommendationMode();
    }

    private void setupFilters() {
        binding.chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            if (allSearchResults.isEmpty()) return;

            int id = checkedIds.get(0);
            if (id == R.id.chipAll) {
                hideEmptyState();
                binding.tvSectionTitle.setText("Hasil Pencarian");
                searchAdapter.updateData(allSearchResults);
            } else if (id == R.id.chipFast) {
                applyFilterFast();
            } else if (id == R.id.chipHighRating) {
                applyFilterHighRating();
            }
        });
    }

    private void applyFilterFast() {
        List<Recipe> filtered = new ArrayList<>();
        for (Recipe r : allSearchResults) {
            int time = (r.getTotalTimeMinutes() != null) ? r.getTotalTimeMinutes() : 0;
            if (time > 0 && time < 20) {
                filtered.add(r);
            }
        }
        binding.tvSectionTitle.setText("Hasil Filter: < 20 Menit");
        if (filtered.isEmpty()) {
            showEmptyState("Tidak ada resep yang bisa dibuat\nkurang dari 20 menit");
        } else {
            hideEmptyState();
        }
        searchAdapter.updateData(filtered);
    }

    private void applyFilterHighRating() {
        for (Recipe r : allSearchResults) {
            double score = (r.getUserRatings() != null && r.getUserRatings().getScore() != null)
                    ? r.getUserRatings().getScore() : -1.0;
            Log.d(TAG, "Resep: " + r.getName() + " | Score: " + score);
        }

        List<Recipe> sorted = new ArrayList<>(allSearchResults);
        sorted.sort((r1, r2) -> {
            double s1 = (r1.getUserRatings() != null && r1.getUserRatings().getScore() != null)
                    ? r1.getUserRatings().getScore() : 0.0;
            double s2 = (r2.getUserRatings() != null && r2.getUserRatings().getScore() != null)
                    ? r2.getUserRatings().getScore() : 0.0;
            return Double.compare(s2, s1);
        });

        binding.tvSectionTitle.setText("Hasil Filter: Rating Tertinggi");
        if (sorted.isEmpty()) {
            showEmptyState("Tidak ada resep dengan rating\nyang ditemukan");
        } else {
            hideEmptyState();
        }
        searchAdapter.updateData(sorted);
    }

    private void showEmptyState(String message) {
        binding.tvEmptyMessage.setText(message);
        binding.tvEmptyState.setVisibility(View.VISIBLE);
        binding.rvSearchResults.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        binding.tvEmptyState.setVisibility(View.GONE);
        binding.rvSearchResults.setVisibility(View.VISIBLE);
    }

    private void performSearch(String query) {
        switchToSearchMode();
        hideEmptyState();
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.rvRecentSearch.setVisibility(View.GONE);

        RetrofitClient.getInstance().getApiService().searchRecipes(0, 20, query).enqueue(new Callback<RecipeListResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecipeListResponse> call, @NonNull Response<RecipeListResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allSearchResults = response.body().getResults();

                    int checkedId = binding.chipGroup.getCheckedChipId();
                    if (checkedId == R.id.chipFast) {
                        applyFilterFast();
                    } else if (checkedId == R.id.chipHighRating) {
                        applyFilterHighRating();
                    } else {
                        hideEmptyState();
                        binding.tvSectionTitle.setText("Hasil Pencarian");
                        searchAdapter.updateData(allSearchResults);
                    }

                    binding.rvRecommendations.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<RecipeListResponse> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Search failed: " + t.getMessage());
            }
        });
    }


    private void showRecommendationMode() {
        binding.btnBack.setVisibility(View.GONE);
        binding.tvEmptyState.setVisibility(View.GONE);
        binding.rvRecommendations.setVisibility(View.VISIBLE);
        binding.rvRecentSearch.setVisibility(View.GONE);
        binding.chipGroup.setVisibility(View.GONE);
        binding.rvSearchResults.setVisibility(View.GONE);
        binding.tvSectionTitle.setText("Rekomendasi untuk Anda");
    }

    private void switchToSearchMode() {
        binding.btnBack.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setVisibility(View.GONE);
        binding.rvRecommendations.setVisibility(View.GONE);
        binding.rvRecentSearch.setVisibility(View.VISIBLE);
        binding.chipGroup.setVisibility(View.VISIBLE);
        binding.rvSearchResults.setVisibility(allSearchResults.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvSectionTitle.setText("Riwayat Pencarian");
        loadRecentSearches();
    }

    private void setupAdapters() {
        RecipeAdapter.OnItemClickListener listener = recipe -> {
            Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
            startActivity(intent);
        };

        searchAdapter = new RecipeAdapter(listener, (r, isFav) -> executor.execute(() -> {
            if (isFav) dbHelper.addFavorite(r);
            else dbHelper.removeFavorite(r.getId());
        }));
        binding.rvSearchResults.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvSearchResults.setAdapter(searchAdapter);
        binding.rvSearchResults.setNestedScrollingEnabled(false);

        recommendationAdapter = new RecipeAdapter(listener, (r, isFav) -> {});
        binding.rvRecommendations.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvRecommendations.setAdapter(recommendationAdapter);
        binding.rvRecommendations.setNestedScrollingEnabled(false);

        historyAdapter = new RecentSearchAdapter(new ArrayList<>(), this::performSearch);
        binding.rvRecentSearch.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvRecentSearch.setAdapter(historyAdapter);
    }

    private void loadRecommendations() {
        RetrofitClient.getInstance().getApiService().searchRecipes(0, 20, "").enqueue(new Callback<RecipeListResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecipeListResponse> call, @NonNull Response<RecipeListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recommendationAdapter.updateData(response.body().getResults());
                }
            }

            @Override
            public void onFailure(@NonNull Call<RecipeListResponse> call, @NonNull Throwable t) {}
        });
    }

    private void saveSearchHistory(String query) {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("SearchPrefs", Context.MODE_PRIVATE);
        String history = prefs.getString("history", "");
        List<String> list = new ArrayList<>(Arrays.asList(history.split(",")));
        list.removeIf(String::isEmpty);
        if (!list.contains(query)) {
            list.add(0, query);
            if (list.size() > 5) list.remove(list.size() - 1);
            prefs.edit().putString("history", String.join(",", list)).apply();
            loadRecentSearches();
        }
    }

    private void loadRecentSearches() {
        String history = requireContext().getSharedPreferences("SearchPrefs", Context.MODE_PRIVATE)
                .getString("history", "");
        if (!history.isEmpty()) {
            historyAdapter.updateData(new ArrayList<>(Arrays.asList(history.split(","))));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}