package com.vibe.stashdrop.features.stashshelf

import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class StashRepository(private val context: Context, private val dao: StashDao) {
    private val directory = File(context.filesDir, "stash").apply { mkdirs() }
    val items = dao.observe()
    suspend fun addText(raw: String, fromClipboard: Boolean = false) = withContext(Dispatchers.IO) {
        val text = UrlPrivacySanitizer.cleanText(raw).take(100_000)
        require(text.isNotBlank()) { "Nothing to stash" }
        dao.insert(StashItemEntity(UUID.randomUUID().toString(), text, null, "text/plain"))
        if (fromClipboard) {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
            dao.record(ClipboardHistoryEntity(digest = digest)); dao.pruneHistory()
        }
    }
    suspend fun addBitmap(bitmap: Bitmap): StashItemEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val file = File(directory, "$id.png")
        try {
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            StashItemEntity(id, null, file.name, "image/png").also { dao.insert(it) }
        } catch (e: Exception) { file.delete(); throw e }
    }
    suspend fun importUri(uri: Uri): Unit = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?.takeIf { it.matches(Regex("[A-Za-z0-9]{1,10}")) }
        val file = File(directory, if(extension != null) "$id.$extension" else id)
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Cannot read attachment" }
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024); var total = 0L
                    while (true) {
                        val n = input.read(buffer); if (n < 0) break
                        total += n; require(total <= 50L * 1024 * 1024) { "Attachment exceeds 50 MB" }
                        output.write(buffer, 0, n)
                    }
                }
            }
            dao.insert(StashItemEntity(id, null, file.name, mime))
        } catch (e: Exception) { file.delete(); throw e }
    }
    fun uri(item: StashItemEntity): Uri? = item.fileName?.let { FileProvider.getUriForFile(context, "${context.packageName}.files", File(directory, it)) }
    fun file(item: StashItemEntity): File? = item.fileName?.let { File(directory, it) }
    suspend fun delete(item: StashItemEntity) = withContext(Dispatchers.IO) { dao.delete(item.id); file(item)?.delete(); Unit }
    suspend fun move(item: StashItemEntity, collection: String) { dao.move(item.id, collection.trim().ifEmpty { "Inbox" }.take(80)) }
    fun clip(items: List<StashItemEntity>): ClipData {
        require(items.isNotEmpty())
        val mimes = items.map { it.mimeType }.distinct().toTypedArray()
        fun entry(item: StashItemEntity) = uri(item)?.let { ClipData.Item(it) } ?: ClipData.Item(item.text.orEmpty())
        return ClipData(ClipDescription("StashDrop selection", mimes), entry(items.first())).apply { items.drop(1).forEach { addItem(entry(it)) } }
    }
}
