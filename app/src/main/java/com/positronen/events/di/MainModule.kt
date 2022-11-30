package com.positronen.events.di

import androidx.lifecycle.ViewModel
import com.positronen.events.data.repository.MainRepositoryImpl
import com.positronen.events.data.service.MainService
import com.positronen.events.domain.MainRepository
import com.positronen.events.data.location.LocationDataSource
import com.positronen.events.data.location.LocationDataSourceImpl
import com.positronen.events.data.service.MainServiceImpl
import com.positronen.events.domain.interactor.MainInteractorImpl
import com.positronen.events.domain.interactor.MainInteractor
import com.positronen.events.presentation.base.ViewModelKey
import com.positronen.events.presentation.detail.DetailViewModel
import com.positronen.events.presentation.main.MainViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class MainModule {

    @Binds
    abstract fun bindMainInteractor(impl: MainInteractorImpl): MainInteractor

    @Binds
    abstract fun bindMainRepository(impl: MainRepositoryImpl): MainRepository

    @Binds
    abstract fun bindMainService(impl: MainServiceImpl): MainService

    @Binds
    abstract fun bindsLocationDataSource(impl: LocationDataSourceImpl): LocationDataSource

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun provideMainViewModel(mainViewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DetailViewModel::class)
    abstract fun provideDetailViewModel(mainViewModel: DetailViewModel): ViewModel
}