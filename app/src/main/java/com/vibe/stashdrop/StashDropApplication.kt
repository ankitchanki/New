package com.vibe.stashdrop

import android.app.Application
import com.vibe.stashdrop.core.haptics.TactileHapticEngine
import com.vibe.stashdrop.core.hiddenapi.HiddenApiBypassInitializer
import com.vibe.stashdrop.core.shizuku.ShizukuConnectionManager
import com.vibe.stashdrop.features.stashshelf.*
import kotlinx.coroutines.*

class StashDropApplication : Application() {
    val database by lazy { StashDatabase.create(this) }
    val repository by lazy { StashRepository(this, database.stash()) }
    val preferences by lazy { StashPreferences(this) }
    val shizuku by lazy { ShizukuConnectionManager(this) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override fun onCreate() {
        super.onCreate(); HiddenApiBypassInitializer.initialize(); shizuku.start()
        scope.launch { preferences.settings.collect { TactileHapticEngine.enabled = it.haptics } }
    }
}
val android.content.Context.stashApp: StashDropApplication get() = applicationContext as StashDropApplication
