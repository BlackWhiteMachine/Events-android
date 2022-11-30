package com.positronen.events.di.core

import androidx.lifecycle.ViewModelProvider
import com.positronen.events.presentation.base.ViewModelFactory
import dagger.Binds
import dagger.Module

@Module
interface ViewModelFactoryModule {

    @Binds
    fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}