package com.vibe.stashdrop.core.shizuku

import android.os.*
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class PrivilegedSystemBridge {
    fun launchNative(): Result<Unit> = runCatching {
        val contextual = if (Build.VERSION.SDK_INT >= 34) runCatching {
            val binder = SystemServiceHelper.getSystemService("contextual_search") ?: error("No contextual search service")
            val stub = Class.forName("android.app.contextualsearch.IContextualSearchManager\$Stub")
            val service = stub.getMethod("asInterface", IBinder::class.java).invoke(null, ShizukuBinderWrapper(binder))
            Class.forName("android.app.contextualsearch.IContextualSearchManager").getMethod("startContextualSearch", Int::class.javaPrimitiveType).invoke(service, 1)
        } else Result.failure(IllegalStateException("Contextual search requires a newer OS"))
        if (contextual.isSuccess) return@runCatching
        val binder = SystemServiceHelper.getSystemService("voiceinteraction") ?: error("Voice interaction service missing")
        val stub = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub")
        val service = stub.getMethod("asInterface", IBinder::class.java).invoke(null, ShizukuBinderWrapper(binder))
        val bundle = Bundle().apply { putInt("omni.entry_point", 1); putLong("invocation_time_ms", SystemClock.elapsedRealtime()) }
        val method = Class.forName("com.android.internal.app.IVoiceInteractionManagerService").methods.first { it.name == "showSessionFromSession" && it.parameterTypes.size == 4 }
        val accepted = method.invoke(service, null, bundle, 7, "com.android.systemui") as? Boolean
        check(accepted == true) { "Google voice session did not accept the request" }
    }
}
