package com.vibe.stashdrop.features.dragdrop

import android.content.*
import android.content.pm.*
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.vibe.stashdrop.features.circletosearch.VisualLensBridge
import com.vibe.stashdrop.features.stashshelf.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class CompatibleDropTargetsTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun receiver(pkg: String)=ResolveInfo().apply {
        activityInfo=ActivityInfo().apply { packageName=pkg; name="$pkg.Share"; exported=true; applicationInfo=ApplicationInfo().apply { packageName=pkg } }
        nonLocalizedLabel=pkg
    }
    @Test fun targetClickRoutesExactPayloadAndRanksChosenAppWithoutRemovingItems() {
        val repository=StashRepository(context,StashDatabase.create(context).stash())
        val items=listOf(StashItemEntity("note","Private saved note",null,"text/plain"))
        val intent=VisualLensBridge.intent(repository,items)
        shadowOf(context.packageManager).addResolveInfoForIntent(intent,receiver("org.telegram.messenger"))
        shadowOf(context.packageManager).addResolveInfoForIntent(intent,receiver("com.whatsapp"))
        var completed=false
        val row=CompatibleDropTargets.create(context,repository,items,{completed=true},{fail(it)}) as LinearLayout
        assertEquals(2,row.childCount)
        row.getChildAt(1).performClick()
        val started=shadowOf(context as android.app.Application).nextStartedActivity
        assertEquals("Private saved note",started.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals("text/plain",started.type)
        assertNotNull(started.component)
        assertTrue(started.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(completed)
        assertTrue(context.getSharedPreferences("share-targets",Context.MODE_PRIVATE).getLong(started.component!!.packageName,0)>0)
    }
    @Test fun unsupportedPayloadHasNoSuggestedTargets() {
        val repository=StashRepository(context,StashDatabase.create(context).stash())
        val row=CompatibleDropTargets.create(context,repository,listOf(StashItemEntity("x","Text",null,"text/plain")),{}, {fail(it)})
        assertNull(row)
    }
}
