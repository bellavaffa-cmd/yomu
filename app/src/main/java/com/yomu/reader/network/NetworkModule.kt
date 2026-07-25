package com.yomu.reader.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.yomu.reader.source.mangadex.MangaDexApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Shared OkHttp/Moshi/Retrofit wiring. Manual singletons (no DI framework) hung off
 * the Application, matching the rest of the app.
 */
object NetworkModule {

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // MangaDex requires a descriptive User-Agent.
                val req = chain.request().newBuilder()
                    .header("User-Agent", "Yomu/0.1 (Android manga reader)")
                    .build()
                chain.proceed(req)
            }
            .apply {
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
                addInterceptor(logging)
            }
            .build()
    }

    val mangaDexApi: MangaDexApi by lazy {
        Retrofit.Builder()
            .baseUrl(MangaDexApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MangaDexApi::class.java)
    }
}
