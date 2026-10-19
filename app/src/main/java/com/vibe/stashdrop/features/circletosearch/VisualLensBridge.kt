package com.vibe.stashdrop.features.circletosearch

import android.app.Activity
import android.content.*
import android.net.Uri
import com.vibe.stashdrop.features.stashshelf.*

object VisualLensBridge {
    fun share(context: Context, repository: StashRepository, items: List<StashItemEntity>, whatsapp: Boolean = false) {
        require(items.isNotEmpty())
        val uris = ArrayList(items.mapNotNull(repository::uri))
        val text = items.mapNotNull { it.text }.joinToString("\n\n")
        val streamTypes = items.filter { it.fileName != null }.map { it.mimeType }.distinct()
        val majorTypes = streamTypes.map { it.substringBefore('/') }.distinct()
        val mime = when {
            streamTypes.isEmpty() -> "text/plain"
            streamTypes.size == 1 -> streamTypes.first()
            majorTypes.size == 1 -> "${majorTypes.first()}/*"
            else -> "*/*"
        }
        val intent = Intent(if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND).apply {
            type = mime
            if (text.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, text)
            if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
            else if (uris.isNotEmpty()) putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            clipData = repository.clip(items)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (whatsapp) setPackage("com.whatsapp")
        }
        if (whatsapp && intent.resolveActivity(context.packageManager) != null) context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        else context.startActivity(Intent.createChooser(intent.setPackage(null), "Share from StashDrop").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun lens(context: Context, uri: Uri) {
        // Lens has no stable public image deep-link; offer its ACTION_SEND receiver when installed.
        val intent = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .apply { clipData = ClipData.newRawUri("Circle cutout", uri) }.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setPackage("com.google.ar.lens")
        if (intent.resolveActivity(context.packageManager) == null) intent.setPackage("com.google.android.googlequicksearchbox")
        if (intent.resolveActivity(context.packageManager) == null) intent.setPackage(null)
        context.startActivity(Intent.createChooser(intent, "Search image with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
