package com.vibe.stashdrop.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.vibe.stashdrop.core.shizuku.ShizukuConnectionManager
import com.vibe.stashdrop.features.stashshelf.StashItemEntity
import com.vibe.stashdrop.features.stashshelf.StashSettings
import com.vibe.stashdrop.ui.theme.StashDropTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class DashboardInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val samples = listOf(
        StashItemEntity("first", "First saved note", null, "text/plain", "Inbox", 1L),
        StashItemEntity("second", "Second saved note", null, "text/plain", "Ideas", 2L),
    )
    private fun screen(actions: DashboardActions, interactions: Boolean = false) {
        compose.setContent { StashDropTheme {
            DashboardScreen(samples, StashSettings(), ShizukuConnectionManager.State(), false,
                SetupState(overlay=true), "", "Auto-stash is off", actions,
                initialTab=if(interactions) 2 else 1, initialInteractions=interactions)
        } }
    }
    @Test fun deletionRequiresConfirmationAndCancelKeepsSelection() {
        var deleted = emptyList<String>()
        screen(DashboardActions(deleteMany={deleted=it.map { item -> item.id }}))
        compose.onNodeWithText("First saved note").performClick()
        fun requestDelete() {
            compose.onNodeWithContentDescription("More selection actions").performClick()
            compose.onNodeWithText("Delete selected").performClick()
        }
        requestDelete()
        compose.onNodeWithText("Keep items").performClick()
        assertTrue(deleted.isEmpty())
        compose.onNodeWithText("1 selected").assertExists()
        requestDelete()
        compose.onNodeWithText("Delete permanently").performClick()
        assertEquals(listOf("first"),deleted)
    }
    @Test fun selectedCardsReachShareAndMoveCallbacks() {
        var shared=emptyList<String>()
        var moved=emptyList<String>()
        var collection=""
        screen(DashboardActions(share={items,_->shared=items.map {it.id}},collectMany={items,name->moved=items.map {it.id};collection=name}))
        compose.onNodeWithText("First saved note").performClick()
        compose.onNodeWithText("Second saved note").performScrollTo().performClick()
        compose.onNodeWithText("2 selected").assertExists()
        compose.onNodeWithContentDescription("Share selected").performClick()
        assertEquals(listOf("first","second"),shared)
        compose.onNodeWithContentDescription("More selection actions").performClick()
        compose.onNodeWithText("Move to collection").performClick()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("Research")
        compose.onNodeWithText("Move selected").performClick()
        assertEquals(listOf("first","second"),moved)
        assertEquals("Research",collection)
    }
    @Test fun copiedTextNeedsExplicitOptIn() {
        val preferences=mutableListOf<Pair<String,Boolean>>()
        screen(DashboardActions(preference={key,value->preferences+=key to value}),interactions=true)
        compose.onNodeWithText("Auto-stash copied text").performScrollTo().performClick()
        compose.onNodeWithText("Keep it off").performClick()
        assertTrue(preferences.isEmpty())
        compose.onNodeWithText("Auto-stash copied text").performScrollTo().performClick()
        compose.onNodeWithText("Enable auto-stash").performClick()
        assertEquals(listOf("auto" to true),preferences)
    }
    @Test fun noteInputRejectsBlankAndSendsEnteredText() {
        var saved=""
        screen(DashboardActions(addText={saved=it}))
        compose.onNodeWithText("Note").performClick()
        compose.onNodeWithText("Save note").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("Keep this useful idea")
        compose.onNodeWithText("Save note").performClick()
        assertEquals("Keep this useful idea",saved)
    }
    @Test fun typeFiltersClearSelectionAndShowRealImagePreviews() {
        val context=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val image=java.io.File(context.cacheDir,"test-preview.png")
        val bitmap=android.graphics.Bitmap.createBitmap(80,80,android.graphics.Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLUE) }
        image.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        val photo=StashItemEntity("photo",null,"photo.png","image/png")
        val otp=StashItemEntity("otp","Your OTP is 654321",null,"text/plain")
        compose.setContent { StashDropTheme {
            DashboardScreen(samples+photo+otp,StashSettings(),ShizukuConnectionManager.State(),false,SetupState(),"","",DashboardActions(),
                initialTab=1,mediaFile={if(it.id=="photo") image else null})
        } }
        compose.onNodeWithText("First saved note").performClick()
        compose.onNodeWithText("Images").performClick()
        compose.onNodeWithText("First saved note").assertDoesNotExist()
        compose.onNodeWithText("1 selected").assertDoesNotExist()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Images preview").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("OTP").performScrollTo().performClick()
        compose.onNodeWithText("Your OTP is 654321").assertExists()
        compose.onNodeWithText("Saved images").assertDoesNotExist()
        image.delete()
    }

}
