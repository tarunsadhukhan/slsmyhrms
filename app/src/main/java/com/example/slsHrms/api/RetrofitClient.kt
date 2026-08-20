package com.example.slsHrms.api

import android.content.Context
import com.example.slsHrms.sync.OfflineInterceptor
import com.google.gson.GsonBuilder
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
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

    /**
     * Flask sends no cache headers, so nothing would ever be stored. Rewriting
     * the *response* to be cacheable is what lets a master GET be replayed from
     * disk when the mill loses its network — this is the whole master-cache
     * mechanism, so it is load-bearing, not an optimisation.
     * `max-age=0` keeps online behaviour identical: every online GET still
     * revalidates against the server.
     */
    private val cacheableResponses = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        if (chain.request().method == "GET" && response.isSuccessful) {
            response.newBuilder()
                .removeHeader("Pragma")
                .header("Cache-Control", "public, max-age=0")
                .build()
        } else {
            response
        }
    }

    private const val CACHE_BYTES = 20L * 1024 * 1024

    @Volatile private var okHttpClient: OkHttpClient? = null

    private fun client(context: Context): OkHttpClient =
        okHttpClient ?: synchronized(this) {
            okHttpClient ?: OkHttpClient.Builder()
                .cache(Cache(File(context.applicationContext.cacheDir, "http_cache"), CACHE_BYTES))
                .addInterceptor(tenantInterceptor)
                // Queues writes / serves reads from cache when there is no network.
                .addInterceptor(OfflineInterceptor(context.applicationContext))
                .addInterceptor(loggingInterceptor)
                .addNetworkInterceptor(cacheableResponses)
                // Connect is short on purpose. The backend is on the mill LAN:
                // it either completes the TCP handshake in well under a second
                // or it is not there. A 60s connect meant every screen froze for
                // a full minute before the offline fallback (cache for reads,
                // outbox for writes) could kick in — which defeats the point of
                // offline mode. Read/write stay generous: they cover a slow
                // server and face-image uploads, which are real waits.
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()
                .also { okHttpClient = it }
        }

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
                .client(client(context))
                .addConverterFactory(GsonConverterFactory.create(gson))  // Use configured Gson
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }

    /** Raw client for the outbox replay, which builds its own URLs. */
    fun rawClient(context: Context): OkHttpClient {
        tenant = ApiConfig.getTenant(context)
        return client(context)
    }

    fun baseUrl(context: Context): String = ApiConfig.getBaseUrl(context)

    /** Drop cached master responses — used on logout and on branch switch. */
    fun clearCache(context: Context) {
        runCatching { client(context).cache?.evictAll() }
    }
}
