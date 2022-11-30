package com.positronen.events.di.core

import android.content.Context
import com.positronen.events.EventsApplication
import com.positronen.events.di.MainModule
import com.positronen.events.presentation.detail.DetailInfoDialogFragment
import com.positronen.events.presentation.main.MainActivity
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Component(
    modules = [
        ViewModelFactoryModule::class,
        HttpClientModule::class,
        MainModule::class
    ]
)
@Singleton
interface CoreComponent: CoreComponentApi {

    @Component.Factory
    interface Factory {

        fun create(@BindsInstance app: Context): CoreComponent
    }

    fun inject(application: EventsApplication)

    fun inject(activity: MainActivity)

    fun inject(fragment: DetailInfoDialogFragment)
}