package com.positronen.events.di

import com.positronen.events.BuildConfig
import com.positronen.events.data.repository.MainRepositoryImpl
import com.positronen.events.data.service.MainService
import com.positronen.events.domain.MainRepository
import com.positronen.events.data.location.LocationDataSource
import com.positronen.events.data.location.LocationDataSourceImpl
import com.positronen.events.domain.interactor.MainInteractorImpl
import com.positronen.events.domain.interactor.MainInteractor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(ViewModelComponent::class)
class MainProvidesModule {

    @Provides
    fun provideRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    if (BuildConfig.DEBUG) {
                        setLevel(HttpLoggingInterceptor.Level.BODY);
                    }
                }
            )
            .build()

        return Retrofit.Builder()
            .baseUrl("https://open-api.myhelsinki.fi")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    @Provides
    fun provideMainService(retrofit: Retrofit): MainService {
        return retrofit.create(MainService::class.java)
    }
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class MainModule {

    @Binds
    abstract fun bindMainInteractor(impl: MainInteractorImpl): MainInteractor

    @Binds
    abstract fun bindMainRepository(impl: MainRepositoryImpl): MainRepository

    @Binds
    abstract fun bindsLocationDataSource(impl: LocationDataSourceImpl): LocationDataSource
}