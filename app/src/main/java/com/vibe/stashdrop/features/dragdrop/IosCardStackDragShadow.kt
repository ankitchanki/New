package com.vibe.stashdrop.features.dragdrop

import android.graphics.*
import android.view.View
import com.vibe.stashdrop.features.stashshelf.*

class IosCardStackDragShadow(view: View, items: List<StashItemEntity>, repository: StashRepository): View.DragShadowBuilder(view) {
    private val density=view.resources.displayMetrics.density
    private val width=(124*density).toInt(); private val height=(154*density).toInt()
    private val labels=items.take(3).map { it.text?.take(70) ?: it.mimeType }
    private val count=items.size
    private val thumbnails=items.take(3).map { item ->
        repository.file(item)?.takeIf { item.mimeType.startsWith("image/") }?.let { file ->
            BitmapFactory.decodeFile(file.path,BitmapFactory.Options().apply { inSampleSize=8 })
        }
    }
    override fun onProvideShadowMetrics(size: Point, touch: Point) { size.set(width,height); touch.set(width/2,(height*.72f).toInt()) }
    override fun onDrawShadow(canvas: Canvas) {
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        val rect=RectF(15*density,15*density,width-15*density,height-24*density)
        val angles=listOf(-4f,2f,5f)
        for(i in labels.indices.reversed()) {
            canvas.save(); canvas.rotate(angles[i],width/2f,height/2f); canvas.translate(i*2*density,-i*3*density)
            paint.color=Color.rgb(29+i*12,35+i*12,49+i*12); paint.setShadowLayer(7*density,0f,3*density,0x66000000)
            canvas.drawRoundRect(rect,14*density,14*density,paint); paint.clearShadowLayer()
            val image=thumbnails[i]
            if(image!=null) { canvas.save(); val path=Path().apply { addRoundRect(rect,14*density,14*density,Path.Direction.CW) }; canvas.clipPath(path); canvas.drawBitmap(image,null,rect,paint); canvas.restore() }
            else { paint.color=Color.WHITE; paint.textSize=11*density; labels[i].chunked(14).take(6).forEachIndexed { line,text -> canvas.drawText(text,rect.left+8*density,rect.top+(22+line*15)*density,paint) } }
            canvas.restore()
        }
        paint.color=Color.rgb(180,201,255); canvas.drawCircle(width-24*density,22*density,15*density,paint)
        paint.color=Color.rgb(15,20,32); paint.textSize=12*density; paint.textAlign=Paint.Align.CENTER
        canvas.drawText(count.toString(),width-24*density,26*density,paint)
    }
}
