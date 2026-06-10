package com.example.flavormix.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Recipe {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("thumbnail_url")
    private String thumbnailUrl;

    @SerializedName("description")
    private String description;

    @SerializedName("total_time_minutes")
    private Integer totalTimeMinutes;

    @SerializedName("num_servings")
    private Integer numServings;

    @SerializedName("yields")
    private String yields;

    @SerializedName("user_ratings")
    private UserRatings userRatings;

    @SerializedName("sections")
    private List<Section> sections;

    @SerializedName("instructions")
    private List<Instruction> instructions;

    @SerializedName("nutrition")
    private Nutrition nutrition;

    @SerializedName("tags")
    private List<Tag> tags;

    private boolean isFavorite;


    public int getId() { return id; }
    public String getName() { return name != null ? name : ""; }
    public String getThumbnailUrl() { return thumbnailUrl != null ? thumbnailUrl : ""; }
    public String getDescription() { return description; }
    public Integer getTotalTimeMinutes() { return totalTimeMinutes; }


    public int getTime() {
        return (totalTimeMinutes != null) ? totalTimeMinutes : 0;
    }

    public Integer getNumServings() { return numServings; }
    public String getYields() { return yields; }
    public UserRatings getUserRatings() { return userRatings; }
    public List<Section> getSections() { return sections; }
    public List<Instruction> getInstructions() { return instructions; }
    public Nutrition getNutrition() { return nutrition; }
    public List<Tag> getTags() { return tags; }
    public boolean isFavorite() { return isFavorite; }


    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }



    public static class UserRatings {
        @SerializedName("score")
        private Double score;
        @SerializedName("count_positive")
        private Integer countPositive;

        public Double getScore() { return score; }
        public Integer getCountPositive() { return countPositive; }
    }

    public static class Section {
        @SerializedName("name")
        private String name;
        @SerializedName("components")
        private List<Component> components;

        public String getName() { return name; }
        public List<Component> getComponents() { return components; }
    }

    public static class Component {
        @SerializedName("raw_text")
        private String rawText;
        @SerializedName("ingredient")
        private Ingredient ingredient;

        public String getRawText() { return rawText; }
        public Ingredient getIngredient() { return ingredient; }
    }

    public static class Ingredient {
        @SerializedName("name")
        private String name;
        public String getName() { return name; }
    }

    public static class Instruction {
        @SerializedName("position")
        private Integer position;
        @SerializedName("display_text")
        private String displayText;

        public Integer getPosition() { return position; }
        public String getDisplayText() { return displayText; }
    }

    public static class Nutrition {
        @SerializedName("calories")
        private Integer calories;
        @SerializedName("protein")
        private Integer protein;
        @SerializedName("fat")
        private Integer fat;
        @SerializedName("carbohydrates")
        private Integer carbohydrates;

        public Integer getCalories() { return calories; }
        public Integer getProtein() { return protein; }
        public Integer getFat() { return fat; }
        public Integer getCarbohydrates() { return carbohydrates; }
    }

    public static class Tag {
        @SerializedName("display_name")
        private String displayName;
        @SerializedName("name")
        private String name;

        public String getDisplayName() { return displayName != null ? displayName : (name != null ? name : ""); }
    }
}