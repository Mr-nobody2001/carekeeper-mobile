package com.example.carekeeper.network;

import androidx.annotation.NonNull;

import com.example.carekeeper.service.SharedPreferencesService;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

public class ApiClient {

    private static final String BASE_URL = "http://172.27.219.132:9001/api/";
    private static Retrofit retrofit;

    /**
     * Retorna uma instância do Retrofit sem autenticação.
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message ->
                    android.util.Log.d("Retrofit", message)
            );
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * Retorna uma instância do Retrofit com JWT incluído no cabeçalho Authorization.
     */
    public static Retrofit getClientWithAuth(SharedPreferencesService prefs) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message ->
                android.util.Log.d("Retrofit", message)
        );
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(new Interceptor() {
                    @NonNull
                    @Override
                    public Response intercept(@NonNull Chain chain) throws IOException {
                        String token = prefs.getJwtToken();
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();

                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }

                        Request request = builder.build();
                        return chain.proceed(request);
                    }
                })
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
