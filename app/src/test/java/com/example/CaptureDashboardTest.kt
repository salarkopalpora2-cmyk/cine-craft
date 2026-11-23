package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.model.SmartCaptureState
import com.example.ui.components.SharpCinemaPreviewPlayer
import com.example.ui.screens.CaptureStudioControlTable
import com.example.ui.theme.CineCraftTheme
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class CaptureDashboardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `action row displays Clear Smart Filter and Start or Stop Capture side by side without overlap`() {
        var isGenerating = false
        var startClicked = 0
        var stopClicked = 0

        composeRule.setContent {
            CineCraftTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    CaptureStudioControlTable(
                        state = SmartCaptureState(
                            videoUri = "content://media/video/101",
                            videoDurationSec = 1800f,
                            enableSmartFrameFilter = true,
                            screenshotCount = 30,
                            isGenerating = isGenerating,
                            captureCurrentIndex = if (isGenerating) 4 else 0,
                            captureTotalCount = 30
                        ),
                        hasVideo = true,
                        onClearVideo = {},
                        onToggleSmartFrameFilter = {},
                        onPickVideoClick = {},
                        onStepCount = {},
                        onUpdateStartTrim = {},
                        onUpdateEndTrim = {},
                        onUpdateZoomLevel = {},
                        onStartGenerating = { startClicked++ },
                        onStopGenerating = { stopClicked++ }
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val clearBounds: Rect = composeRule
            .onNodeWithTag("capture_clear_video_button")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        val filterBounds: Rect = composeRule
            .onNodeWithTag("capture_smart_filter_toggle")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        val captureBounds: Rect = composeRule
            .onNodeWithTag("capture_generate_screenshots_button")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot

        // All 3 buttons must share the exact same horizontal row
        assertTrue(
            "Clear and Smart Filter must be on same row: clearTop=${clearBounds.top}, filterTop=${filterBounds.top}",
            abs(clearBounds.top - filterBounds.top) <= 3f
        )
        assertTrue(
            "Smart Filter and Capture must be on same row: filterTop=${filterBounds.top}, captureTop=${captureBounds.top}",
            abs(filterBounds.top - captureBounds.top) <= 3f
        )
        assertTrue(
            "Buttons must not overlap horizontally: clearRight=${clearBounds.right}, filterLeft=${filterBounds.left}",
            clearBounds.right <= filterBounds.left + 1f
        )
        assertTrue(
            "Buttons must not overlap horizontally: filterRight=${filterBounds.right}, captureLeft=${captureBounds.left}",
            filterBounds.right <= captureBounds.left + 1f
        )

        composeRule.onNodeWithText("Clear").assertIsDisplayed()
        composeRule.onNodeWithText("Smart Filter ON").assertIsDisplayed()
        composeRule.onNodeWithText("Start Capture").assertIsDisplayed().performClick()
        assertEquals(1, startClicked)
    }

    @Test
    fun `start capture toggles cleanly to stop capture when generating`() {
        var stopClicks = 0
        composeRule.setContent {
            CineCraftTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    CaptureStudioControlTable(
                        state = SmartCaptureState(
                            videoUri = "content://media/video/101",
                            videoDurationSec = 1800f,
                            isGenerating = true,
                            captureCurrentIndex = 7,
                            captureTotalCount = 30
                        ),
                        hasVideo = true,
                        onClearVideo = {},
                        onToggleSmartFrameFilter = {},
                        onPickVideoClick = {},
                        onStepCount = {},
                        onUpdateStartTrim = {},
                        onUpdateEndTrim = {},
                        onUpdateZoomLevel = {},
                        onStartGenerating = {},
                        onStopGenerating = { stopClicks++ }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("capture_progress_text").assertTextEquals("7/30")
        composeRule.onNodeWithText("Stop Capture").assertIsDisplayed()
        composeRule.onNodeWithTag("capture_generate_screenshots_button").performClick()
        assertEquals(1, stopClicks)
    }

    @Test
    fun `player controls expose play rewind forward speed mute and fullscreen cleanly on 360dp width`() {
        var playClicks = 0
        var rewindClicks = 0
        var forwardClicks = 0
        var speedClicks = 0
        var muteClicks = 0
        var fullscreenClicks = 0

        composeRule.setContent {
            CineCraftTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    SharpCinemaPreviewPlayer(
                        isPlaying = false,
                        currentTimeSec = 95f,
                        totalDurationSec = 1829f,
                        volume = 0.85f,
                        isMuted = false,
                        controlsEnabled = true,
                        onTogglePlay = { playClicks++ },
                        onSeek = {},
                        onSeekFinished = {},
                        onRewind = { rewindClicks++ },
                        onForward = { forwardClicks++ },
                        playbackSpeed = 1.25f,
                        onCycleSpeed = { speedClicks++ },
                        onToggleFullscreen = { fullscreenClicks++ },
                        onVolumeChange = {},
                        onToggleMute = { muteClicks++ },
                        trimStartPct = 0.08f,
                        trimEndPct = 0.88f,
                        previewTestTag = "capture_preview_box"
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val playBounds = composeRule.onNodeWithTag("capture_preview_box_play_pause_button")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val rewindBounds = composeRule.onNodeWithTag("capture_preview_box_rewind_button")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val forwardBounds = composeRule.onNodeWithTag("capture_preview_box_forward_button")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val speedBounds = composeRule.onNodeWithTag("capture_preview_box_speed_button")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val muteBounds = composeRule.onNodeWithTag("capture_preview_box_mute_button")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val fullscreenBounds = composeRule.onNodeWithTag("capture_preview_box_fullscreen_button")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot

        assertTrue(playBounds.right <= rewindBounds.left + 1f)
        assertTrue(rewindBounds.right <= forwardBounds.left + 1f)
        assertTrue(forwardBounds.right <= speedBounds.left + 1f)
        assertTrue(speedBounds.right <= muteBounds.left + 1f)
        assertTrue(muteBounds.right <= fullscreenBounds.left + 1f)

        composeRule.onNodeWithText("1.25x").assertIsDisplayed()
        composeRule.onNodeWithTag("capture_preview_box_play_pause_button").performClick()
        composeRule.onNodeWithTag("capture_preview_box_rewind_button").performClick()
        composeRule.onNodeWithTag("capture_preview_box_forward_button").performClick()
        composeRule.onNodeWithTag("capture_preview_box_speed_button").performClick()
        composeRule.onNodeWithTag("capture_preview_box_mute_button").performClick()
        composeRule.onNodeWithTag("capture_preview_box_fullscreen_button").performClick()

        assertEquals(1, playClicks)
        assertEquals(1, rewindClicks)
        assertEquals(1, forwardClicks)
        assertEquals(1, speedClicks)
        assertEquals(1, muteClicks)
        assertEquals(1, fullscreenClicks)
    }

    @Test
    fun `capture dashboard fits above bottom navigation bar without overlap on 360x780 phone viewport`() {
        composeRule.setContent {
            CineCraftTheme {
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .height(780.dp)
                ) {
                    CineCraftStudioRoot()
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_tab_capture").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        val bottomDockBounds = composeRule
            .onNodeWithTag("cine_craft_bottom_dock")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot

        val row1CaptureBounds = composeRule
            .onNodeWithTag("capture_generate_screenshots_button")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot

        val zoomSliderBounds = composeRule
            .onNodeWithTag("capture_zoom_slider")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(
            "Capture controls top row must sit above bottom dock: btnBottom=${row1CaptureBounds.bottom}, dockTop=${bottomDockBounds.top}",
            row1CaptureBounds.bottom <= bottomDockBounds.top + 1f
        )
        assertTrue(
            "Last row (Zoom slider) must NOT overlap bottom navigation dock: zoomBottom=${zoomSliderBounds.bottom}, dockTop=${bottomDockBounds.top}",
            zoomSliderBounds.bottom <= bottomDockBounds.top + 1f
        )
    }
}
