package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CaptureDefaults
import com.example.model.SmartCaptureState
import com.example.ui.theme.StudioAlternatingTint
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioOnSurface
import com.example.ui.theme.StudioOnSurfaceVariant
import com.example.ui.theme.StudioOutlineVariant
import com.example.ui.theme.StudioPrimaryContainer
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioSecondaryInteractive
import com.example.ui.theme.StudioTextStyles
import com.example.viewmodel.formatMmSs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CaptureStudioControlTable(
    state: SmartCaptureState,
    hasVideo: Boolean,
    onClearVideo: () -> Unit,
    onToggleSmartFrameFilter: () -> Unit,
    onPickVideoClick: () -> Unit,
    onStepCount: (Int) -> Unit,
    onUpdateStartTrim: (Int) -> Unit,
    onUpdateEndTrim: (Int) -> Unit,
    onUpdateZoomLevel: (Float) -> Unit,
    onStartGenerating: () -> Unit,
    onStopGenerating: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxTrimRange = maxOf(330f, state.videoDurationSec)
    val scope = rememberCoroutineScope()
    val zoomPercent = (state.zoomLevel * 100f).roundToInt()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = StudioCardSurface,
        border = BorderStroke(1.dp, StudioBorderSubtle)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Upload/Change Video (Left) + Symmetrical Compact Count Stepper [-  30  +] (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioCardSurface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPickVideoClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioPrimaryContainer,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("capture_panel_upload_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.videoUri == null) "Upload Video" else "Change Video",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Count Stepper Box: [-]  [30]  [+]
                Row(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioAlternatingTint)
                        .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HoldRepeatCountButton(
                        icon = Icons.Rounded.Remove,
                        contentDescription = "Decrease screenshot count",
                        testTag = "capture_count_minus_button",
                        scope = scope,
                        onStep = { onStepCount(-1) }
                    )

                    Text(
                        text = state.screenshotCount.toString(),
                        style = StudioTextStyles.CodeMd.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = StudioOnSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(42.dp)
                            .testTag("capture_count_value_text")
                    )

                    HoldRepeatCountButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Increase screenshot count",
                        testTag = "capture_count_plus_button",
                        scope = scope,
                        onStep = { onStepCount(1) }
                    )
                }
            }

            HorizontalDivider(color = StudioBorderSubtle.copy(alpha = 0.7f))

            // Row 2: Upload/Change Video (Left) + Symmetrical Compact Count Stepper [-  30  +] (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioCardSurface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPickVideoClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioPrimaryContainer,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("capture_panel_upload_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.videoUri == null) "Upload Video" else "Change Video",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Count Stepper Box: [-]  [30]  [+]
                Row(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioAlternatingTint)
                        .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HoldRepeatCountButton(
                        icon = Icons.Rounded.Remove,
                        contentDescription = "Decrease screenshot count",
                        testTag = "capture_count_minus_button",
                        scope = scope,
                        onStep = { onStepCount(-1) }
                    )

                    Text(
                        text = state.screenshotCount.toString(),
                        style = StudioTextStyles.CodeMd.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = StudioOnSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(42.dp)
                            .testTag("capture_count_value_text")
                    )

                    HoldRepeatCountButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Increase screenshot count",
                        testTag = "capture_count_plus_button",
                        scope = scope,
                        onStep = { onStepCount(1) }
                    )
                }
            }

            HorizontalDivider(color = StudioBorderSubtle.copy(alpha = 0.7f))

            // Row 3: Start Trim (Left) & End Trim (Right) side-by-side in one compact row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioAlternatingTint)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Start Trim
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start Trim",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = StudioAmber
                        )
                        Text(
                            text = formatMmSs(state.startTrimSec),
                            style = StudioTextStyles.CodeSm.copy(fontWeight = FontWeight.Bold),
                            color = StudioAmber,
                            modifier = Modifier.testTag("capture_start_trim_value_text")
                        )
                    }
                    Slider(
                        value = state.startTrimSec.toFloat().coerceIn(0f, maxTrimRange),
                        onValueChange = { onUpdateStartTrim(it.roundToInt()) },
                        valueRange = 0f..maxTrimRange,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioAmber,
                            activeTrackColor = StudioAmber,
                            inactiveTrackColor = StudioAmber.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .testTag("capture_start_trim_slider")
                    )
                }

                // Right: End Trim
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "End Trim",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = StudioRose
                        )
                        Text(
                            text = formatMmSs(state.endTrimSec),
                            style = StudioTextStyles.CodeSm.copy(fontWeight = FontWeight.Bold),
                            color = StudioRose,
                            modifier = Modifier.testTag("capture_end_trim_value_text")
                        )
                    }
                    Slider(
                        value = state.endTrimSec.toFloat().coerceIn(0f, maxTrimRange),
                        onValueChange = { onUpdateEndTrim(it.roundToInt()) },
                        valueRange = 0f..maxTrimRange,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioRose,
                            activeTrackColor = StudioRose,
                            inactiveTrackColor = StudioRose.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .testTag("capture_end_trim_slider")
                    )
                }
            }

            HorizontalDivider(color = StudioBorderSubtle.copy(alpha = 0.7f))

            // Row 4: Compact Zoom Row -> Zoom Out (-) | Zoom Slider | Live % | Zoom In (+)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioCardSurface)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioAlternatingTint)
                        .border(1.dp, StudioBorderSubtle, RoundedCornerShape(8.dp))
                        .clickable { onUpdateZoomLevel(state.zoomLevel - 0.1f) }
                        .testTag("capture_zoom_out_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomOut,
                        contentDescription = "Zoom out",
                        tint = StudioOnSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Slider(
                    value = state.zoomLevel,
                    onValueChange = onUpdateZoomLevel,
                    valueRange = CaptureDefaults.MIN_ZOOM_LEVEL..CaptureDefaults.MAX_ZOOM_LEVEL,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioPrimaryContainer,
                        activeTrackColor = StudioPrimaryContainer,
                        inactiveTrackColor = StudioBorderSubtle
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .testTag("capture_zoom_slider")
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioAlternatingTint)
                        .border(1.dp, StudioBorderSubtle, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("capture_zoom_value_text")
                ) {
                    Text(
                        text = "$zoomPercent%",
                        style = StudioTextStyles.CodeSm.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = StudioPrimaryContainer
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioAlternatingTint)
                        .border(1.dp, StudioBorderSubtle, RoundedCornerShape(8.dp))
                        .clickable { onUpdateZoomLevel(state.zoomLevel + 0.1f) }
                        .testTag("capture_zoom_in_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomIn,
                        contentDescription = "Zoom in",
                        tint = StudioOnSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = StudioBorderSubtle.copy(alpha = 0.7f))

            // Row 5: Bottom Action Bar -> LEFT: Clear | CENTER: Smart Filter ON/OFF | RIGHT: Start/Stop Capture
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (state.isGenerating) StudioRose.copy(alpha = 0.08f)
                        else StudioAlternatingTint
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (state.isGenerating) {
                    val total = state.captureTotalCount.coerceAtLeast(1)
                    val progress = (state.captureCurrentIndex.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            color = StudioRose,
                            trackColor = StudioBorderSubtle,
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = "${state.captureCurrentIndex}/$total",
                            style = StudioTextStyles.CodeSm.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = StudioRose,
                            modifier = Modifier.testTag("capture_progress_text")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // LEFT: Clear Button
                    Row(
                        modifier = Modifier
                            .weight(0.76f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = hasVideo, onClick = onClearVideo)
                            .background(
                                if (hasVideo) StudioRose.copy(alpha = 0.10f)
                                else StudioOutlineVariant.copy(alpha = 0.16f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (hasVideo) StudioRose.copy(alpha = 0.35f) else StudioBorderSubtle,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 6.dp)
                            .testTag("capture_clear_video_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Clear video",
                            tint = if (hasVideo) StudioRose else StudioOnSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (hasVideo) StudioRose else StudioOnSurfaceVariant.copy(alpha = 0.45f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // CENTER: Smart Filter ON / OFF Toggle
                    Row(
                        modifier = Modifier
                            .weight(1.16f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onToggleSmartFrameFilter)
                            .background(
                                if (state.enableSmartFrameFilter) StudioEmerald.copy(alpha = 0.13f)
                                else StudioOutlineVariant.copy(alpha = 0.24f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (state.enableSmartFrameFilter) StudioEmerald.copy(alpha = 0.45f)
                                else StudioOutlineVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 6.dp)
                            .testTag("capture_smart_filter_toggle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (state.enableSmartFrameFilter) Icons.Rounded.AutoAwesome else Icons.Rounded.FilterList,
                            contentDescription = null,
                            tint = if (state.enableSmartFrameFilter) StudioEmerald else StudioOnSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (state.enableSmartFrameFilter) "Smart Filter ON" else "Smart Filter OFF",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (state.enableSmartFrameFilter) StudioEmerald else StudioOnSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // RIGHT: Start Capture / Stop Capture Toggle Button
                    Button(
                        onClick = if (state.isGenerating) onStopGenerating else onStartGenerating,
                        enabled = state.isGenerating || hasVideo,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isGenerating) StudioRose else StudioSecondaryInteractive,
                            contentColor = Color.White,
                            disabledContainerColor = StudioSecondaryInteractive.copy(alpha = 0.45f),
                            disabledContentColor = Color.White.copy(alpha = 0.85f)
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1.18f)
                            .height(44.dp)
                            .testTag("capture_generate_screenshots_button")
                    ) {
                        if (state.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = "Stop capture",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Stop Capture",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Start Capture",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldRepeatCountButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onStep: () -> Unit
) {
    val latestOnStep by rememberUpdatedState(onStep)

    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 42.dp)
            .pointerInput(testTag) {
                detectTapGestures(
                    onTap = { latestOnStep() },
                    onPress = {
                        val job = scope.launch {
                            delay(280L)
                            while (true) {
                                latestOnStep()
                                delay(65L)
                            }
                        }
                        try {
                            tryAwaitRelease()
                        } finally {
                            job.cancel()
                        }
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = StudioOnSurface,
            modifier = Modifier.size(18.dp)
        )
    }
}
