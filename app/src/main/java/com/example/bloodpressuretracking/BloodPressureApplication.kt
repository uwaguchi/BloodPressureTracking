package com.example.bloodpressuretracking

import android.app.Application
import android.util.Log
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for BloodPressureTracking app.
 * Hilt entry point for dependency injection.
 * Initializes AWS Amplify with Cognito authentication.
 */
@HiltAndroidApp
class BloodPressureApplication : Application() {

    companion object {
        private const val TAG = "BloodPressureApp"
    }

    override fun onCreate() {
        super.onCreate()
        initializeAmplify()
    }

    /**
     * Initialize AWS Amplify with Cognito Auth plugin.
     * Configuration is loaded from res/raw/amplifyconfiguration.json
     */
    private fun initializeAmplify() {
        try {
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(applicationContext)
            Log.i(TAG, "Amplify initialized successfully")
        } catch (error: AmplifyException) {
            Log.e(TAG, "Failed to initialize Amplify", error)
        }
    }
}
