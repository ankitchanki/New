package com.vibe.stashdrop.core.shizuku

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Makes StashDrop the default digital assistant (long-press home opens Circle to Search) and restores the previous one. */
class AssistantController(private val context: Context, private val manager: ShizukuConnectionManager) {
    private val backup = context.getSharedPreferences("assistant-backup", Context.MODE_PRIVATE)
    private val user get() = Process.myUid() / 100_000 // Android user id
    val supported get() = Build.VERSION.SDK_INT >= 29
    /** StashDrop receives assistant presses: it holds the role, or presses are routed to its assist screen. */
    fun isDefault(): Boolean = (supported && runCatching {
        context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT)
    }.getOrDefault(false)) || runCatching { android.provider.Settings.Secure.getString(context.contentResolver, "assistant") }.getOrNull() == ShellUserService.ASSIST_ACTIVITY
    /** Keeps StashDrop on assistant presses while switching Google's voice service back on for Circle to Search. */
    suspend fun linkGoogle(): String? = withContext(Dispatchers.IO) {
        val service = manager.service ?: return@withContext null
        val google = com.vibe.stashdrop.features.circletosearch.GoogleCircleToSearch.googleService(context) ?: return@withContext "Error: Google app is not installed"
        service.routeAssistantToStashDrop(google.flattenToString(), user)
    }
    /** Null when Shizuku is unavailable and the caller should open the system default-apps screen instead. */
    suspend fun makeDefault(): String? = withContext(Dispatchers.IO) {
        val service = manager.service ?: return@withContext null
        val previous = service.getAssistantRole(user)
        if (!previous.startsWith("Error:") && previous != context.packageName && !backup.contains("holder"))
            backup.edit().putString("holder", previous).commit()
        service.setAssistantRole(context.packageName, user).let { if (it.startsWith("Error:")) it else "StashDrop is now your digital assistant" }
    }
    suspend fun restore(): String? = withContext(Dispatchers.IO) {
        val service = manager.service ?: return@withContext null
        val previous = backup.getString("holder", null) ?: GOOGLE
        val result = service.setAssistantRole(previous, user)
        if (!result.startsWith("Error:")) backup.edit().remove("holder").commit()
        result
    }
    suspend fun grantSetup(): String? = withContext(Dispatchers.IO) { manager.service?.grantSetup(user) }
    companion object { const val GOOGLE = "com.google.android.googlequicksearchbox" }
}
