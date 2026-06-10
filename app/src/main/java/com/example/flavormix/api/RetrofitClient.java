package com.example.flavormix.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    // API Key dari RapidAPI (Tasty API by ApiDojo)
    private static final String RAPID_API_KEY = "4018becc44msh82a6692bd017855p1bd2bfjsnd38434e4d62c";
    private static final String BASE_URL      = "https://tasty.p.rapidapi.com/";
    private static final String RAPID_API_HOST = "tasty.p.rapidapi.com";

    private static RetrofitClient instance;
    private final TastyApiService apiService;

    private RetrofitClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("x-rapidapi-key", RAPID_API_KEY)
                            .header("x-rapidapi-host", RAPID_API_HOST)
                            .build();
                    return chain.proceed(request);
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(TastyApiService.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public TastyApiService getApiService() {
        return apiService;
    }
}