package com.example.flavormix.api;


import com.example.flavormix.model.Recipe;
import com.example.flavormix.model.RecipeListResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TastyApiService {

    @GET("recipes/list")
    Call<RecipeListResponse> getRecipes(
            @Query("from") int from,
            @Query("size") int size,
            @Query("tags") String tags
    );

    @GET("recipes/list")
    Call<RecipeListResponse> searchRecipes(
            @Query("from") int from,
            @Query("size") int size,
            @Query("q") String query
    );

    @GET("recipes/get-more-info")
    Call<Recipe> getRecipeDetail(
            @Query("id") int id
    );
}