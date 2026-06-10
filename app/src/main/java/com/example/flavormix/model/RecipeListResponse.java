package com.example.flavormix.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RecipeListResponse {

    @SerializedName("count")
    private Integer count;

    @SerializedName("results")
    private List<Recipe> results;

    public Integer getCount() { return count; }

    public List<Recipe> getResults() {
        return results != null ? results : new java.util.ArrayList<>();
    }
}
