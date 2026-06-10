// INI SUDAH DIPERBAIKI (Hapus .utils):
package com.example.flavormix;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import com.example.flavormix.api.RetrofitClient;
import com.example.flavormix.database.RecipeDbHelper;
import com.example.flavormix.databinding.ActivityRecipeDetailBinding;
import com.example.flavormix.model.Recipe;
import com.example.flavormix.utils.NetworkUtils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID    = "extra_recipe_id";
    public static final String EXTRA_RECIPE_NAME  = "extra_recipe_name";
    public static final String EXTRA_RECIPE_THUMB = "extra_recipe_thumb";

    private ActivityRecipeDetailBinding binding;
    private RecipeDbHelper dbHelper;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private int     recipeId;
    private boolean isFavorite = false;
    private Recipe  currentRecipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecipeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recipeId = getIntent().getIntExtra(EXTRA_RECIPE_ID, 0);
        String recipeName  = getIntent().getStringExtra(EXTRA_RECIPE_NAME);
        String recipeThumb = getIntent().getStringExtra(EXTRA_RECIPE_THUMB);

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(recipeName);

        Glide.with(this)
                .load(recipeThumb)
                .placeholder(R.drawable.placeholder_recipe)
                .centerCrop()
                .into(binding.imgHero);

        dbHelper = new RecipeDbHelper(this);

        executor.execute(() -> {
            isFavorite = dbHelper.isFavorite(recipeId);
            mainHandler.post(this::updateFabIcon);
        });

        setupFab();
        setupRetryButton();
        loadDetail();
    }

    private void setupFab() {
        binding.fabFavorite.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            updateFabIcon();
            executor.execute(() -> {
                if (isFavorite && currentRecipe != null) {
                    currentRecipe.setFavorite(true);
                    dbHelper.addFavorite(currentRecipe);
                } else {
                    dbHelper.removeFavorite(recipeId);
                }
                String msg = isFavorite ? "Ditambahkan ke favorit" : "Dihapus dari favorit";
                mainHandler.post(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
            });
        });
    }

    private void setupRetryButton() {
        binding.btnRetry.setOnClickListener(v -> loadDetail());
    }

    private void updateFabIcon() {
        binding.fabFavorite.setImageResource(
                isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border
        );
    }

    private void loadDetail() {
        showLoading(true);
        hideError();

        if (!NetworkUtils.isNetworkAvailable(this)) {
            showLoading(false);
            showError("Tidak ada koneksi internet");
            return;
        }

        RetrofitClient.getInstance().getApiService()
                .getRecipeDetail(recipeId)
                .enqueue(new Callback<Recipe>() {
                    @Override
                    public void onResponse(@NonNull Call<Recipe> call,
                                           @NonNull Response<Recipe> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            currentRecipe = response.body();
                            displayDetail(currentRecipe);
                        } else {
                            showError("Gagal memuat detail (kode: " + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Recipe> call, @NonNull Throwable t) {
                        showLoading(false);
                        showError("Koneksi gagal: " + t.getMessage());
                    }
                });
    }

    private void displayDetail(Recipe recipe) {
        Glide.with(this)
                .load(recipe.getThumbnailUrl())
                .placeholder(R.drawable.placeholder_recipe)
                .centerCrop()
                .into(binding.imgHero);

        binding.tvTitle.setText(recipe.getName());


        String desc = recipe.getDescription();
        if (desc != null && !desc.isEmpty()) {
            binding.tvDescription.setText(desc);
            binding.tvDescription.setVisibility(View.VISIBLE);
        } else {
            binding.tvDescription.setVisibility(View.GONE);
        }


        Integer time = recipe.getTotalTimeMinutes();
        if (time != null && time > 0) {
            binding.tvTime.setText(time >= 60
                    ? (time / 60) + "j " + (time % 60) + "m"
                    : time + "m");
        } else {
            binding.tvTime.setText("N/A");
        }


        binding.tvServings.setText(recipe.getYields() != null
                ? recipe.getYields()
                : (recipe.getNumServings() != null ? recipe.getNumServings() + " porsi" : "N/A"));


        if (recipe.getUserRatings() != null && recipe.getUserRatings().getScore() != null) {
            double stars = recipe.getUserRatings().getScore() * 5;
            binding.tvRating.setText(String.format(Locale.US, "%.1f", stars));
        } else {
            binding.tvRating.setText("N/A");
        }


        Recipe.Nutrition nutrition = recipe.getNutrition();
        if (nutrition != null) {
            binding.layoutNutrition.setVisibility(View.VISIBLE);
            binding.tvCalories.setText(String.valueOf(nutrition.getCalories() != null ? nutrition.getCalories() : 0));
            binding.tvProtein.setText((nutrition.getProtein() != null ? nutrition.getProtein() : 0) + "g");
            binding.tvFat.setText((nutrition.getFat() != null ? nutrition.getFat() : 0) + "g");
            binding.tvCarbs.setText((nutrition.getCarbohydrates() != null ? nutrition.getCarbohydrates() : 0) + "g");
        } else {
            binding.layoutNutrition.setVisibility(View.GONE);
        }


        List<Recipe.Section> sections = recipe.getSections();
        if (sections != null && !sections.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Recipe.Section section : sections) {
                if (section.getName() != null && !section.getName().isEmpty()) {
                    sb.append("📌 ").append(section.getName()).append("\n");
                }
                if (section.getComponents() != null) {
                    for (Recipe.Component comp : section.getComponents()) {
                        String text = comp.getRawText();
                        if (text == null && comp.getIngredient() != null) text = comp.getIngredient().getName();
                        if (text != null && !text.isEmpty()) sb.append("• ").append(text).append("\n");
                    }
                }
                sb.append("\n");
            }
            binding.tvIngredients.setText(sb.toString().trim());
        }


        List<Recipe.Instruction> instructions = recipe.getInstructions();
        if (instructions != null && !instructions.isEmpty()) {
            instructions.sort((a, b) -> {
                int pa = a.getPosition() != null ? a.getPosition() : 0;
                int pb = b.getPosition() != null ? b.getPosition() : 0;
                return pa - pb;
            });
            StringBuilder sb = new StringBuilder();
            for (Recipe.Instruction step : instructions) {
                int pos = step.getPosition() != null ? step.getPosition() : 0;
                String txt = step.getDisplayText();
                if (txt != null && !txt.isEmpty()) {
                    sb.append(pos).append(". ").append(txt).append("\n\n");
                }
            }
            binding.tvInstructions.setText(sb.toString().trim());
        }



        binding.scrollContent.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) binding.scrollContent.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        binding.layoutError.setVisibility(View.VISIBLE);
        binding.tvError.setText(msg);
    }

    private void hideError() {
        binding.layoutError.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}