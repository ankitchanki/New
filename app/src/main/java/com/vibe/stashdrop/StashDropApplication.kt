package com.vibe.stashdrop

import android.app.Application
import com.vibe.stashdrop.core.hiddenapi.HiddenApiBypassInitializer
import com.vibe.stashdrop.core.shizuku.ShizukuConnectionManager
import com.vibe.stashdrop.features.stashshelf.*

class StashDropApplication : Application() {
    val database by lazy { StashDatabase.create(this) }
    val repository by lazy { StashRepository(this, database.stash()) }
    val preferences by lazy { StashPreferences(this) }
    val shizuku by lazy { ShizukuConnectionManager(this) }
    override fun onCreate() { super.onCreate(); HiddenApiBypassInitializer.initialize(); shizuku.start() }
}
val android.content.Context.stashApp: StashDropApplication get() = applicationContext as StashDropApplication
