package com.example.flavormix.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
// INI BAGIAN YANG DIPERBAIKI:
import com.example.flavormix.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME    = "flavormix.db";
    private static final int    DATABASE_VERSION  = 1;


    private static final String TABLE_CACHE     = "cache_recipes";
    private static final String COL_ID          = "id";
    private static final String COL_JSON        = "json_data";
    private static final String COL_CATEGORY    = "category";
    private static final String COL_TIMESTAMP   = "timestamp";


    private static final String TABLE_FAVORITES = "favorites";
    private static final String COL_FAV_ID      = "id";
    private static final String COL_FAV_JSON    = "json_data";
    private static final String COL_FAV_TS      = "timestamp";

    private final Gson gson = new Gson();

    public RecipeDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CACHE + " (" +
                COL_ID        + " INTEGER PRIMARY KEY, " +
                COL_JSON      + " TEXT NOT NULL, " +
                COL_CATEGORY  + " TEXT, " +
                COL_TIMESTAMP + " INTEGER)");

        db.execSQL("CREATE TABLE " + TABLE_FAVORITES + " (" +
                COL_FAV_ID  + " INTEGER PRIMARY KEY, " +
                COL_FAV_JSON + " TEXT NOT NULL, " +
                COL_FAV_TS  + " INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CACHE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        onCreate(db);
    }


    public void cacheRecipes(List<Recipe> recipes, String category) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_CACHE, COL_CATEGORY + " = ?", new String[]{category});
            long timestamp = System.currentTimeMillis();
            for (Recipe recipe : recipes) {
                ContentValues cv = new ContentValues();
                cv.put(COL_ID,        recipe.getId());
                cv.put(COL_JSON,      gson.toJson(recipe));
                cv.put(COL_CATEGORY,  category);
                cv.put(COL_TIMESTAMP, timestamp);
                db.insertWithOnConflict(TABLE_CACHE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Recipe> getCachedRecipes(String category) {
        List<Recipe> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_CACHE,
                new String[]{COL_JSON},
                COL_CATEGORY + " = ?",
                new String[]{category},
                null, null, COL_TIMESTAMP + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    String json = cursor.getString(cursor.getColumnIndexOrThrow(COL_JSON));
                    Recipe r = gson.fromJson(json, Recipe.class);
                    list.add(r);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
        return list;
    }

    public boolean isCacheValid(String category) {
        long maxAge = 3600000L; // 1 jam
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_CACHE,
                new String[]{COL_TIMESTAMP},
                COL_CATEGORY + " = ?",
                new String[]{category},
                null, null, COL_TIMESTAMP + " DESC", "1");
        if (cursor != null && cursor.moveToFirst()) {
            long ts = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));
            cursor.close();
            return System.currentTimeMillis() - ts < maxAge;
        }
        if (cursor != null) cursor.close();
        return false;
    }


    public void addFavorite(Recipe recipe) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_FAV_ID,  recipe.getId());
        cv.put(COL_FAV_JSON, gson.toJson(recipe));
        cv.put(COL_FAV_TS,  System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_FAVORITES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void removeFavorite(int recipeId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_FAVORITES, COL_FAV_ID + " = ?", new String[]{String.valueOf(recipeId)});
    }

    public boolean isFavorite(int recipeId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITES,
                new String[]{COL_FAV_ID},
                COL_FAV_ID + " = ?",
                new String[]{String.valueOf(recipeId)},
                null, null, null);
        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();
        return exists;
    }

    public List<Recipe> getAllFavorites() {
        List<Recipe> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITES,
                new String[]{COL_FAV_JSON},
                null, null, null, null, COL_FAV_TS + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    String json = cursor.getString(cursor.getColumnIndexOrThrow(COL_FAV_JSON));
                    Recipe r = gson.fromJson(json, Recipe.class);
                    r.setFavorite(true);
                    list.add(r);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
        return list;
    }
}