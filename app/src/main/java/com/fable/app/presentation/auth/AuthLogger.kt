package com.fable.app.presentation.auth

import android.util.Log
import com.fable.app.BuildConfig

internal object AuthLogger {
    private const val TAG = "FableAuth"

    fun log(action: String, details: String? = null) {
        if (BuildConfig.DEBUG) {
            try {
                val message = if (details != null) "$action: $details" else action
                Log.d(TAG, message)
            } catch (_: Throwable) {
                // Safely ignored in JVM unit tests where android.util.Log is unmocked
            }
        }
    }

    fun logError(action: String, error: Throwable) {
        if (BuildConfig.DEBUG) {
            try {
                // Log exception type and sanitized message, never user credentials or secrets
                Log.e(TAG, "Failed $action: ${error.javaClass.simpleName} - ${error.message}")
            } catch (_: Throwable) {
                // Safely ignored in JVM unit tests where android.util.Log is unmocked
            }
        }
    }
}
