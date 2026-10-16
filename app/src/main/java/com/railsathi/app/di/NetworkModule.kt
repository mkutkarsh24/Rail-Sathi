package com.railsathi.app.di

import android.content.Context
import com.google.gson.Gson
import com.railsathi.app.BuildConfig
import com.railsathi.app.data.remote.api.ApiMode
import com.railsathi.app.data.remote.api.MockRailApi
import com.railsathi.app.data.remote.api.RailApi
import com.railsathi.app.data.remote.api.RetryInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApiMode(): ApiMode = ApiMode.from(BuildConfig.RAIL_API_BASE_URL)

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    /**
     * No logging interceptor on purpose: request URLs/bodies can contain train details and the
     * PNR, and nothing sensitive should reach logcat.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .cache(Cache(File(context.cacheDir, "http"), 5L * 1024 * 1024))
            .addInterceptor(RetryInterceptor(maxRetries = 2))
            .build()

    @Provides
    @Singleton
    fun provideRailApi(mode: ApiMode, client: dagger.Lazy<OkHttpClient>, gson: Gson): RailApi =
        if (mode.isMock) {
            MockRailApi()
        } else {
            Retrofit.Builder()
                .baseUrl(mode.baseUrl)
                .client(client.get())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(RailApi::class.java)
        }
}
