package com.positronen.events.di

import com.positronen.events.data.repository.MainRepositoryImpl
import com.positronen.events.data.service.MainService
import com.positronen.events.domain.MainRepository
import com.positronen.events.data.location.LocationDataSource
import com.positronen.events.data.location.LocationDataSourceImpl
import com.positronen.events.data.service.MainServiceImpl
import com.positronen.events.domain.interactor.MainInteractorImpl
import com.positronen.events.domain.interactor.MainInteractor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

@Module
@InstallIn(ViewModelComponent::class)
class MainProvidesModule {

    @Provides
    fun provideHttpClient(): HttpClient =
        HttpClient(CIO) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.ALL
            }

            install(DefaultRequest)

            install(ContentNegotiation) {
                json(Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }

            install(HttpTimeout) {
                connectTimeoutMillis = 15000
                requestTimeoutMillis = 30000
            }

            defaultRequest {
                url("https://open-api.myhelsinki.fi")
                header("Content-Type", "application/json; charset=UTF-8")
            }
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
    abstract fun bindMainService(impl: MainServiceImpl): MainService

    @Binds
    abstract fun bindsLocationDataSource(impl: LocationDataSourceImpl): LocationDataSource
}