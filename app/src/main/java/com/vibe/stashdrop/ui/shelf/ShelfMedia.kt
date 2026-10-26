package com.vibe.stashdrop.ui.shelf

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.geometry.Rect
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.ui.unit.dp
import com.vibe.stashdrop.features.stashshelf.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

internal object ShelfThumbnails {
    private val cache=object: LruCache<String,Bitmap>(12*1024*1024) {
        override fun sizeOf(key: String,value: Bitmap)=value.allocationByteCount
    }
    private fun key(file: File,mime: String)="${file.absolutePath}:${file.length()}:${file.lastModified()}:$mime"
    /** Alternate decoder for devices/codecs where ImageDecoder rejects a readable file. */
    private fun decodeSampled(file: File): Bitmap? {
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeFile(file.absolutePath,bounds)
        if(bounds.outWidth<=0 || bounds.outHeight<=0) return null
        var sample=1
        while(max(bounds.outWidth,bounds.outHeight)/sample>640) sample*=2
        val decoded=BitmapFactory.decodeFile(file.absolutePath,BitmapFactory.Options().apply { inSampleSize=sample }) ?: return null
        val ratio=(320f/max(decoded.width,decoded.height)).coerceAtMost(1f)
        val scaled=Bitmap.createScaledBitmap(decoded,max(1,(decoded.width*ratio).roundToInt()),max(1,(decoded.height*ratio).roundToInt()),true)
        if(scaled!==decoded) decoded.recycle()
        val orientation=runCatching { ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix=Matrix().apply { when(orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f,1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f,-1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f,1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f,1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
        } }
        val oriented=Bitmap.createBitmap(scaled,0,0,scaled.width,scaled.height,matrix,true)
        if(oriented!==scaled) scaled.recycle()
        return oriented
    }
    fun cached(file: File,mime: String): Bitmap? = cache.get(key(file,mime))
    /** Bounded, local decoding only; no image upload or network fetch. ImageDecoder honors EXIF. */
    fun load(file: File,mime: String): Bitmap? {
        cached(file,mime)?.let { return it }
        if(!file.isFile) return null
        val bitmap=try {
            if(mime.startsWith("video/")) {
                val retriever=MediaMetadataRetriever()
                try { retriever.setDataSource(file.absolutePath); retriever.getScaledFrameAtTime(-1,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,320,320) }
                finally { retriever.release() }
            } else if(mime.startsWith("image/")) {
                try {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder,info,_ ->
                        val ratio=(320f/max(info.size.width,info.size.height)).coerceAtMost(1f)
                        decoder.setTargetSize(max(1,(info.size.width*ratio).roundToInt()),max(1,(info.size.height*ratio).roundToInt()))
                        decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } catch (_: Exception) { decodeSampled(file) }
            } else null
        } catch (error: Exception) { android.util.Log.w("StashDropMedia","Local media thumbnail unavailable ($mime)",error); null }
        bitmap?.let { cache.put(key(file,mime),it) }
        return bitmap
    }
}

@Composable
fun ShelfMediaThumbnail(item: StashItemEntity,file: File?,modifier: Modifier=Modifier) {
    val media=item.mimeType.startsWith("image/") || item.mimeType.startsWith("video/")
    var bitmap by remember(item.id,file?.absolutePath) { mutableStateOf(if(media && file!=null) ShelfThumbnails.cached(file,item.mimeType) else null) }
    val host=LocalView.current
    // Unknown until first positioned; keep an already cached preview instead of flashing the placeholder.
    var visible by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(item.id,file?.absolutePath,item.mimeType,visible) {
        bitmap=when {
            !media || file==null || visible==false -> null
            visible==true -> ShelfThumbnails.cached(file,item.mimeType) ?: withContext(Dispatchers.IO) { ShelfThumbnails.load(file,item.mimeType) }
            else -> ShelfThumbnails.cached(file,item.mimeType)
        }
    }
    val shape=RoundedCornerShape(16.dp)
    Box(modifier.onGloballyPositioned { coordinates ->
        visible=coordinates.boundsInWindow().overlaps(Rect(0f,0f,host.width.toFloat(),host.height.toFloat()))
    }.clip(shape).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=androidx.compose.ui.Alignment.Center) {
        val frame=bitmap
        if(frame!=null) Image(frame.asImageBitmap(),"${item.category().label} preview",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        else Icon(when(item.category()) {
            ShelfCategory.VIDEOS -> Icons.Outlined.Videocam
            ShelfCategory.IMAGES,ShelfCategory.STICKERS -> Icons.Outlined.Image
            ShelfCategory.URLS -> Icons.Outlined.Link
            ShelfCategory.OTP -> Icons.Outlined.Password
            ShelfCategory.TEXT -> Icons.Outlined.Description
            else -> Icons.AutoMirrored.Outlined.InsertDriveFile
        },if(media) "Preview unavailable" else null,Modifier.size(24.dp),tint=MaterialTheme.colorScheme.primary)
        if(frame!=null && item.mimeType.startsWith("video/")) Icon(Icons.Outlined.PlayCircle,"Video",tint=androidx.compose.ui.graphics.Color.White)
    }
}

@Composable
fun ShelfTypeFilters(selected: ShelfCategory,onSelect: (ShelfCategory)->Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        ShelfCategory.entries.forEach { type ->
            com.vibe.stashdrop.ui.theme.OriginChip(type.label,selected==type) { onSelect(type) }
        }
    }
}
