package com.vibe.stashdrop.features.dragdrop

import android.content.*
import android.content.pm.PackageManager
import android.view.*
import android.widget.*
import com.vibe.stashdrop.features.circletosearch.VisualLensBridge
import com.vibe.stashdrop.features.stashshelf.*

/** Only activities advertising support for this exact share intent. No usage-history permission. */
object CompatibleDropTargets {
    fun create(context: Context, repository: StashRepository, items: List<StashItemEntity>,
        onComplete: () -> Unit, onError: (String) -> Unit): View? {
        val intent=VisualLensBridge.intent(repository,items)
        val manager=context.packageManager
        val history=context.getSharedPreferences("share-targets",Context.MODE_PRIVATE)
        val matches=manager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.activityInfo.packageName!=context.packageName && it.activityInfo.exported &&
                (it.activityInfo.permission==null || context.checkSelfPermission(it.activityInfo.permission)==PackageManager.PERMISSION_GRANTED) }
            .distinctBy { it.activityInfo.packageName }
            .sortedWith(compareByDescending<android.content.pm.ResolveInfo> { history.getLong(it.activityInfo.packageName,0L) }
                .thenBy { if(it.activityInfo.packageName in listOf("com.whatsapp","org.telegram.messenger")) 0 else 1 }
                .thenBy { it.loadLabel(manager).toString() }).take(4)
        if(matches.isEmpty()) return null
        val density=context.resources.displayMetrics.density
        val row=LinearLayout(context).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER
            background=android.graphics.drawable.GradientDrawable().apply { setColor(0xFF20283A.toInt()); cornerRadius=20*density }
            setPadding((8*density).toInt(),(6*density).toInt(),(8*density).toInt(),(6*density).toInt())
            elevation=10*density
        }
        matches.forEach { match ->
            val column=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER }
            val icon=ImageView(context).apply { setImageDrawable(match.loadIcon(manager)); contentDescription="Share to ${match.loadLabel(manager)}" }
            column.addView(icon,LinearLayout.LayoutParams((36*density).toInt(),(36*density).toInt()))
            column.addView(TextView(context).apply { text=match.loadLabel(manager); textSize=10f; setTextColor(android.graphics.Color.WHITE); maxLines=1; gravity=Gravity.CENTER; ellipsize=android.text.TextUtils.TruncateAt.END })
            fun send(): Boolean = runCatching {
                context.startActivity(Intent(intent).setComponent(ComponentName(match.activityInfo.packageName,match.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                history.edit().putLong(match.activityInfo.packageName,System.currentTimeMillis()).apply()
                onComplete(); true
            }.getOrElse { onError(it.message ?: "This app could not receive the item"); false }
            column.setOnClickListener { send() }
            column.setOnDragListener { view,event ->
                when(event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> true
                    DragEvent.ACTION_DRAG_ENTERED -> { view.animate().scaleX(1.12f).scaleY(1.12f).alpha(1f).setDuration(100).start(); true }
                    DragEvent.ACTION_DRAG_EXITED,DragEvent.ACTION_DRAG_ENDED -> { view.animate().scaleX(1f).scaleY(1f).setDuration(100).start(); true }
                    DragEvent.ACTION_DROP -> send()
                    else -> true
                }
            }
            row.addView(column,LinearLayout.LayoutParams((64*density).toInt(),(62*density).toInt()))
        }
        return row
    }
}
