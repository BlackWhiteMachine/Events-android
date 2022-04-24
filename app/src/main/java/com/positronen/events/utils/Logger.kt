package com.positronen.events.utils

import android.util.Log
import com.positronen.events.BuildConfig

object Logger {

    private const val TAG = "Events"

    fun debug(message: String, tag: String? = null) {
        if (BuildConfig.DEBUG) {
            message.chunked(4000).forEach {
                Log.d(tag ?: TAG, it)
            }
        }
    }

    fun exception(throwable: Throwable, tag: String? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, throwable.message.toString())
        } else {
            // To analytics
        }
    }
}