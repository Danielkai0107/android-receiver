package com.safenet.receiver.di

import com.safenet.receiver.data.remote.api.CloudFunctionApi
import com.safenet.receiver.data.remote.api.ServiceUuidApi
import com.safenet.receiver.data.remote.api.UploadApi
import com.safenet.receiver.data.remote.api.FallbackUploadApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WhitelistRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UploadRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PrimaryUploadRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FallbackUploadRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ServiceUuidRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
    
    @WhitelistRetrofit
    @Provides
    @Singleton
    fun provideWhitelistRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://getdevicewhitelist-kmzfyt3t5a-uc.a.run.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @UploadRetrofit
    @Provides
    @Singleton
    fun provideUploadRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://receivebeacondata-kmzfyt3t5a-uc.a.run.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @PrimaryUploadRetrofit
    @Provides
    @Singleton
    fun providePrimaryUploadRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://us-central1-safe-net-tw.cloudfunctions.net/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @FallbackUploadRetrofit
    @Provides
    @Singleton
    fun provideFallbackUploadRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://receivebeacondata-kmzfyt3t5a-uc.a.run.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @WhitelistRetrofit
    @Provides
    @Singleton
    fun provideWhitelistApi(@WhitelistRetrofit retrofit: Retrofit): CloudFunctionApi {
        return retrofit.create(CloudFunctionApi::class.java)
    }
    
    @UploadRetrofit
    @Provides
    @Singleton
    fun provideUploadApi(@UploadRetrofit retrofit: Retrofit): CloudFunctionApi {
        return retrofit.create(CloudFunctionApi::class.java)
    }
    
    @PrimaryUploadRetrofit
    @Provides
    @Singleton
    fun providePrimaryUploadApi(@PrimaryUploadRetrofit retrofit: Retrofit): UploadApi {
        return retrofit.create(UploadApi::class.java)
    }
    
    @FallbackUploadRetrofit
    @Provides
    @Singleton
    fun provideFallbackUploadApi(@FallbackUploadRetrofit retrofit: Retrofit): FallbackUploadApi {
        return retrofit.create(FallbackUploadApi::class.java)
    }
    
    @ServiceUuidRetrofit
    @Provides
    @Singleton
    fun provideServiceUuidRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            // 移除結尾的斜線，避免與 @GET("/") 產生雙斜線導致 404
            .baseUrl("https://us-central1-safe-net-tw.cloudfunctions.net/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideServiceUuidApi(@ServiceUuidRetrofit retrofit: Retrofit): ServiceUuidApi {
        return retrofit.create(ServiceUuidApi::class.java)
    }
}
