package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPreviewCanvas
import com.example.ui.theme.StudioPrimaryContainer
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioTextStyles
import com.example.viewmodel.formatMmSs
import kotlin.math.roundToInt

/**
 * Unified sharp 4-corner (0.dp radius) 16:9 Cinema Preview Display + Bottom Video Player Control Bar
 * used across Agent, Capture, and Video tables for 100% consistent display sizing and player UX.
 */
@Composable
fun SharpCinemaPreviewPlayer(
    isPlaying: Boolean,
    currentTimeSec: Float,
    totalDurationSec: Float,
    volume: Float,
    isMuted: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
    controlsEnabled: Boolean = true,
    onRewind: (() -> Unit)? = null,
    onForward: (() -> Unit)? = null,
    onSeekFinished: (() -> Unit)? = null,
    playbackSpeed: Float? = null,
    onCycleSpeed: (() -> Unit)? = null,
    onToggleFullscreen: (() -> Unit)? = null,
    isFullscreen: Boolean = false,
    trimStartPct: Float? = null,
    trimEndPct: Float? = null,
    activeStatusLabel: String? = null,
    onCancelStatusAction: (() -> Unit)? = null,
    extraBadgeText: String? = null,
    previewTestTag: String = "unified_preview_canvas",
    content: @Composable BoxScope.() -> Unit
) {
    val safeTotal = totalDurationSec.coerceAtLeast(1f)
    val safeTime = currentTimeSec.coerceIn(0f, safeTotal)
    val effectiveVol = if (isMuted) 0f else volume.coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioPreviewCanvas)
    ) {
        // 1. Sharp 4-Corner Full-Width 16:9 Display Surface (Zero side padding, zero corner radius)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RectangleShape)
                .clipToBounds()
                .background(StudioPreviewCanvas)
                .clickable(enabled = controlsEnabled, onClick = onTogglePlay)
                .testTag(previewTestTag),
            contentAlignment = Alignment.Center,
            content = content
        )

        // Hairline divider separating 16:9 screen from bottom player controls
        HorizontalDivider(thickness = 1.dp, color = Color(0xFF1E293B))

        // 2. Compact Bottom Video Player Control Bar (Trim Strip + Seek Timeline + Full Controls)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B1221))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Optional 3-color Trim visualization bar
            if (trimStartPct != null && trimEndPct != null) {
                val startFrac = trimStartPct.coerceIn(0f, 1f)
                val activeFrac = (trimEndPct - trimStartPct).coerceIn(0f, 1f - startFrac)
                val endFrac = (1f - trimEndPct).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                        .height(4.dp)
                        .background(Color(0xFF1E293B))
                ) {
                    if (startFrac > 0.001f) {
                        Box(
                            modifier = Modifier
                                .weight(startFrac)
                                .fillMaxHeight()
                                .background(StudioAmber.copy(alpha = 0.85f))
                        )
                    }
                    if (activeFrac > 0.001f) {
                        Box(
                            modifier = Modifier
                                .weight(activeFrac)
                                .fillMaxHeight()
                                .background(StudioEmerald)
                        )
                    }
                    if (endFrac > 0.001f) {
                        Box(
                            modifier = Modifier
                                .weight(endFrac)
                                .fillMaxHeight()
                                .background(StudioRose.copy(alpha = 0.85f))
                        )
                    }
                }
            }

            // Real-Time Seek Slider + Timecode Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatMmSs(safeTime.toInt()),
                    style = StudioTextStyles.CodeSm,
                    color = Color.White
                )

                Slider(
                    value = safeTime,
                    onValueChange = onSeek,
                    onValueChangeFinished = { onSeekFinished?.invoke() },
                    enabled = controlsEnabled,
                    valueRange = 0f..safeTotal,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = StudioPrimaryContainer,
                        inactiveTrackColor = Color(0xFF334155),
                        disabledThumbColor = Color(0xFF64748B),
                        disabledActiveTrackColor = Color(0xFF475569),
                        disabledInactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .testTag("${previewTestTag}_seek_slider")
                )

                Text(
                    text = formatMmSs(safeTotal.toInt()),
                    style = StudioTextStyles.CodeSm,
                    color = Color(0xFF94A3B8)
                )
            }

            // Bottom Action Row: Play/Pause + Rewind/Forward + Speed + Volume Slider + Fullscreen
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (controlsEnabled) StudioPrimaryContainer
                            else StudioPrimaryContainer.copy(alpha = 0.4f)
                        )
                        .clickable(enabled = controlsEnabled, onClick = onTogglePlay)
                        .testTag("${previewTestTag}_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause video" else "Play video",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                if (onRewind != null) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF1E293B))
                            .clickable(enabled = controlsEnabled, onClick = onRewind)
                            .testTag("${previewTestTag}_rewind_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastRewind,
                            contentDescription = "Rewind 10 seconds",
                            tint = if (controlsEnabled) Color.White else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                if (onForward != null) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF1E293B))
                            .clickable(enabled = controlsEnabled, onClick = onForward)
                            .testTag("${previewTestTag}_forward_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastForward,
                            contentDescription = "Forward 10 seconds",
                            tint = if (controlsEnabled) Color.White else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                if (playbackSpeed != null && onCycleSpeed != null) {
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .widthIn(min = 36.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF1E293B))
                            .clickable(enabled = controlsEnabled, onClick = onCycleSpeed)
                            .padding(horizontal = 5.dp)
                            .testTag("${previewTestTag}_speed_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatPlaybackSpeed(playbackSpeed),
                            style = StudioTextStyles.CodeSm.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (controlsEnabled) Color(0xFF93C5FD) else Color(0xFF64748B),
                            maxLines = 1
                        )
                    }
                }

                if (!activeStatusLabel.isNullOrBlank() && onCancelStatusAction != null && onCycleSpeed == null) {
                    Text(
                        text = activeStatusLabel,
                        style = StudioTextStyles.CodeSm,
                        color = Color(0xFF93C5FD),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(64.dp)
                    )
                    Box(
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioRose)
                            .clickable(onClick = onCancelStatusAction)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("${previewTestTag}_cancel_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Stop",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                } else if (!extraBadgeText.isNullOrBlank() && onToggleFullscreen == null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = extraBadgeText,
                            style = StudioTextStyles.CodeSm.copy(fontSize = 10.sp),
                            color = Color(0xFFCBD5E1),
                            maxLines = 1
                        )
                    }
                }

                if (onToggleFullscreen == null) {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleMute)
                        .testTag("${previewTestTag}_mute_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted || volume <= 0.01f) {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = if (isMuted) "Unmute sound" else "Mute sound",
                        tint = if (isMuted) StudioRose else Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Slider(
                    value = effectiveVol,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF60A5FA),
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = if (onToggleFullscreen != null) {
                        Modifier
                            .weight(1f)
                            .height(22.dp)
                            .testTag("${previewTestTag}_volume_slider")
                    } else {
                        Modifier
                            .widthIn(min = 56.dp, max = 80.dp)
                            .height(22.dp)
                            .testTag("${previewTestTag}_volume_slider")
                    }
                )

                Text(
                    text = "${(effectiveVol * 100f).roundToInt()}%",
                    style = StudioTextStyles.CodeSm.copy(fontSize = 10.sp),
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(28.dp)
                )

                if (onToggleFullscreen != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF1E293B))
                            .clickable(enabled = controlsEnabled, onClick = onToggleFullscreen)
                            .testTag("${previewTestTag}_fullscreen_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) {
                                Icons.Rounded.FullscreenExit
                            } else {
                                Icons.Rounded.Fullscreen
                            },
                            contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen preview",
                            tint = if (controlsEnabled) Color.White else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
