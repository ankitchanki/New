package com.vibe.stashdrop.features.circletosearch

import android.content.Context
import android.widget.Toast

/** All entry points request Google. Failure never starts a StashDrop capture or search activity. */
class CircleToSearchOrchestrator(private val context: Context) {
    /** True means Android accepted the request, not proof that Google displayed Circle to Search. */
    suspend fun launch(): Boolean {
        if (GoogleCircleToSearch.launch(context)) return true
        val reason = when (GoogleCircleToSearch.readiness(context)) {
            GoogleCircleToSearch.Readiness.GOOGLE_MISSING -> "Install or enable the Google app first"
            GoogleCircleToSearch.Readiness.GOOGLE_NOT_ASSISTANT -> "Choose Google as your digital assistant in system settings, or connect Google in StashDrop Settings"
            GoogleCircleToSearch.Readiness.READY -> "Google search request was blocked. Connect Shizuku and enable Circle to Search in your phone's navigation settings. Device support is required."
        }
        Toast.makeText(context, reason, Toast.LENGTH_LONG).show()
        return false
    }
}
