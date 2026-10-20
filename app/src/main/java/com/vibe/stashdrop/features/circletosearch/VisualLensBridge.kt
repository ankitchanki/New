package com.vibe.stashdrop.features.circletosearch

import android.content.*
import com.vibe.stashdrop.features.stashshelf.*

object VisualLensBridge {
    fun intent(repository: StashRepository, items: List<StashItemEntity>): Intent {
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
        return Intent(if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND).apply {
            type = mime
            if (text.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, text)
            if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
            else if (uris.isNotEmpty()) putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            clipData = repository.clip(items)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    fun share(context: Context, repository: StashRepository, items: List<StashItemEntity>, whatsapp: Boolean = false) {
        val intent=intent(repository,items)
        if(whatsapp) intent.setPackage("com.whatsapp")
        if (whatsapp && intent.resolveActivity(context.packageManager) != null) context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        else context.startActivity(Intent.createChooser(intent.setPackage(null), "Share from StashDrop").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
