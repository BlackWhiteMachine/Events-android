package com.positronen.events

import android.app.Application
import com.positronen.events.di.core.CoreComponent
import com.positronen.events.di.core.DaggerCoreComponent

class EventsApplication : Application() {

    lateinit var component: CoreComponent

    override fun onCreate() {
        super.onCreate()

        component = DaggerCoreComponent
            .factory()
            .create(this)
    }
}