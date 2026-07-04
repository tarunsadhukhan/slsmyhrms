package com.example.slsHrms.api

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Current tenant/database name; injected as the X-Tenant header on every
    // request so the backend connects to the right database. Updated from
    // ApiConfig in getApiService(); volatile so the interceptor (which may run
    // on OkHttp's background threads) always sees the latest value.
    @Volatile
    private var tenant: String = ""

    private val tenantInterceptor = Interceptor { chain ->
        val request = chain.request()
        val t = tenant
        val newRequest = if (t.isNotEmpty()) {
            request.newBuilder().header("X-Tenant", t).build()
        } else {
            request
        }
        chain.proceed(newRequest)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY  // Changed to BODY to see response content
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(tenantInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    // Configure Gson with lenient mode and proper null handling
    private val gson = GsonBuilder()
        .setLenient()  // Allow lenient JSON parsing
        .serializeNulls()  // Include null fields
        .create()

    private var currentBaseUrl: String? = null
    private var retrofit: Retrofit? = null

    fun getApiService(context: Context): ApiService {
        val savedUrl = ApiConfig.getBaseUrl(context)
        // Refresh the tenant on every call so a change in Settings takes effect
        // without rebuilding the OkHttp client.
        tenant = ApiConfig.getTenant(context)
        if (retrofit == null || currentBaseUrl != savedUrl) {
            currentBaseUrl = savedUrl
            retrofit = Retrofit.Builder()
                .baseUrl(savedUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))  // Use configured Gson
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }
}
