package com.vibe.stashdrop.features.stashshelf

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("settings")
data class GestureSettings(val swipe: Boolean = true, val corners: Boolean = false, val hold: Boolean = false, val doubleTap: Boolean = false)
data class StashSettings(val autoStash: Boolean = false, val gesturesEnabled: Boolean = false, val gestures: GestureSettings = GestureSettings(), val bounce: Float = 1f,
    val haptics: Boolean = true)
class StashPreferences(private val context: Context) {
    val settings = context.store.data.map { p ->
        StashSettings(p[booleanPreferencesKey("auto")] ?: false, p[booleanPreferencesKey("bar")] ?: false,
            GestureSettings(p[booleanPreferencesKey("swipe")] ?: true, p[booleanPreferencesKey("corners")] ?: false,
                p[booleanPreferencesKey("hold")] ?: false, p[booleanPreferencesKey("double")] ?: false), p[floatPreferencesKey("bounce")] ?: 1f,
            p[booleanPreferencesKey("haptics")] ?: true)
    }
    suspend fun set(key: String, value: Boolean) { context.store.edit { it[booleanPreferencesKey(key)] = value } }
    suspend fun bounce(value: Float) { context.store.edit { it[floatPreferencesKey("bounce")] = value.coerceIn(.5f, 1.5f) } }
}
