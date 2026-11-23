package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AiUiState
import com.example.model.DirectorShotPlan
import com.example.ui.components.SharpCinemaPreviewPlayer
import com.example.ui.components.StudioParameterChip
import com.example.ui.components.StudioPrimaryButton
import com.example.ui.components.StudioPromptTextField
import com.example.ui.components.StudioSecondaryButton
import com.example.ui.theme.StudioAlternatingTint
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAppCanvas
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioOnSurface
import com.example.ui.theme.StudioOnSurfaceVariant
import com.example.ui.theme.StudioPrimaryContainer
import com.example.ui.theme.StudioTextStyles
import com.example.ui.theme.StudioViolet
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.formatSmpte

@Composable
fun VideoScreen(
    uiState: StudioUiState,
    onSelectClip: (String) -> Unit,
    onPlayheadChanged: (Float) -> Unit,
    onTogglePlayback: () -> Unit,
    onRewindPlayback: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onTrimClipEnd: (Float) -> Unit,
    onSetMotionPreset: (String) -> Unit,
    onTimelinePromptChanged: (String) -> Unit,
    onGenerateStoryboard: () -> Unit,
    onApplyShotPlan: (DirectorShotPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeClip = uiState.activeClip
    val motionPresets = listOf("Dolly Push 1.2x", "Aerial Orbit", "Static 50mm", "Whip Pan")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Unified Sharp 4-Corner Table (Matches Agent & Capture Tables Identically)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                color = StudioCardSurface,
                border = BorderStroke(1.dp, StudioBorderSubtle)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. Sharp 4-Corner 16:9 Preview Display + Bottom Video Player Controls
                    SharpCinemaPreviewPlayer(
                        isPlaying = uiState.isPlayingTimeline,
                        currentTimeSec = uiState.playheadSec,
                        totalDurationSec = uiState.totalTimelineDurationSec,
                        volume = uiState.timelineVolume,
                        isMuted = uiState.timelineMuted,
                        onTogglePlay = onTogglePlayback,
                        onSeek = onPlayheadChanged,
                        onRewind = onRewindPlayback,
                        onVolumeChange = onVolumeChanged,
                        onToggleMute = onToggleMute,
                        trimStartPct = activeClip?.trimStartFraction,
                        trimEndPct = activeClip?.trimEndFraction,
                        extraBadgeText = activeClip?.motionPreset ?: "SEQUENCE",
                        previewTestTag = "video_top_preview_canvas"
                    ) {
                        Image(
                            painter = painterResource(
                                id = activeClip?.thumbnailRes ?: R.drawable.img_scene_neon_tokyo
                            ),
                            contentDescription = activeClip?.title ?: "Video sequence monitor",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (uiState.canvasOptions.showGridLines) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val thirdW = size.width / 3f
                                val thirdH = size.height / 3f
                                val gridColor = Color.White.copy(alpha = 0.18f)
                                drawLine(gridColor, Offset(thirdW, 0f), Offset(thirdW, size.height), 1f)
                                drawLine(gridColor, Offset(thirdW * 2f, 0f), Offset(thirdW * 2f, size.height), 1f)
                                drawLine(gridColor, Offset(0f, thirdH), Offset(size.width, thirdH), 1f)
                                drawLine(gridColor, Offset(0f, thirdH * 2f), Offset(size.width, thirdH * 2f), 1f)
                            }
                        }

                        // Top-left SMPTE indicator
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = formatSmpte(uiState.playheadSec),
                                style = StudioTextStyles.CodeSm,
                                color = Color.White
                            )
                        }

                        // Top-right preset badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = uiState.canvasOptions.lutPreset,
                                style = StudioTextStyles.CodeSm,
                                color = Color(0xFF93C5FD)
                            )
                        }

                        // Bottom active clip caption bar inside frame
                        if (activeClip != null) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                        )
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeClip.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${activeClip.trackName} • ${activeClip.motionPreset}",
                                    style = StudioTextStyles.CodeSm,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1F5F9))

                    // Row 1: Video Timeline Track V1 Header + Clip Strip
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StudioCardSurface)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Video Timeline • Track V1 (${uiState.timelineClips.size} Clips)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = StudioOnSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioPrimaryContainer.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = formatSmpte(uiState.totalTimelineDurationSec),
                                    style = StudioTextStyles.CodeSm,
                                    color = StudioPrimaryContainer
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            uiState.timelineClips.forEach { clip ->
                                val isSelected = clip.id == activeClip?.id
                                val effectiveSec = clip.durationSec * (clip.trimEndFraction - clip.trimStartFraction)
                                Surface(
                                    modifier = Modifier
                                        .width(148.dp)
                                        .clickable { onSelectClip(clip.id) }
                                        .testTag("timeline_clip_${clip.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) StudioPrimaryContainer.copy(alpha = 0.08f) else StudioAlternatingTint,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) StudioPrimaryContainer else StudioBorderSubtle
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(68.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            Image(
                                                painter = painterResource(id = clip.thumbnailRes),
                                                contentDescription = clip.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.65f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = String.format("%.1fs", effectiveSec),
                                                    style = StudioTextStyles.CodeSm,
                                                    color = Color.White
                                                )
                                            }
                                            if (clip.trimEndFraction < 0.99f) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(4.dp)
                                                        .clip(CircleShape)
                                                        .background(StudioAmber)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "TRIM",
                                                        style = StudioTextStyles.CodeSm,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = clip.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = StudioOnSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = clip.motionPreset,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StudioOnSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Row 2: Clip Inspector & Trim Controls
                    if (activeClip != null) {
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1F5F9))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StudioAlternatingTint)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Clip Inspector • ${activeClip.title}",
                                style = MaterialTheme.typography.labelLarge,
                                color = StudioOnSurface
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                motionPresets.forEach { preset ->
                                    StudioParameterChip(
                                        label = preset,
                                        selected = activeClip.motionPreset == preset,
                                        accentColor = StudioViolet,
                                        onClick = { onSetMotionPreset(preset) },
                                        testTag = "motion_chip_${preset.lowercase().replace(" ", "_")}"
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StudioSecondaryButton(
                                    text = "Trim -0.5s",
                                    icon = Icons.Rounded.ContentCut,
                                    onClick = { onTrimClipEnd(-0.15f) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "trim_shorter_button"
                                )
                                StudioSecondaryButton(
                                    text = "Extend +0.5s",
                                    icon = Icons.Rounded.ContentCut,
                                    onClick = { onTrimClipEnd(0.15f) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "trim_longer_button"
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1F5F9))

                    // Row 3: AI Sequence Generator
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StudioCardSurface)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = StudioViolet,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "AI Sequence Generator",
                                style = MaterialTheme.typography.titleMedium,
                                color = StudioOnSurface
                            )
                        }
                        StudioPromptTextField(
                            value = uiState.timelinePrompt,
                            onValueChange = onTimelinePromptChanged,
                            placeholder = "e.g., Golden hour coastal drone montage with fast cuts…",
                            onSubmit = onGenerateStoryboard,
                            testTag = "timeline_ai_prompt_input"
                        )
                        StudioPrimaryButton(
                            text = if (uiState.storyboardState is AiUiState.Loading) "Generating…" else "Generate AI Shots",
                            icon = Icons.Rounded.AutoAwesome,
                            enabled = uiState.timelinePrompt.isNotBlank() && uiState.storyboardState !is AiUiState.Loading,
                            onClick = onGenerateStoryboard,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "timeline_generate_shots_button"
                        )

                        when (val state = uiState.storyboardState) {
                            is AiUiState.Loading -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = "Building Gemini shot sequence…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudioOnSurfaceVariant
                                    )
                                }
                            }

                            is AiUiState.Error -> {
                                AiErrorBanner(message = state.message)
                            }

                            is AiUiState.Success -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = StudioAppCanvas,
                                    border = BorderStroke(1.dp, StudioBorderSubtle)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = state.data.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = StudioOnSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        state.data.shots.forEach { shot ->
                                            Text(
                                                text = "${shot.shotCode} (${shot.durationSec}s) • ${shot.framing}",
                                                style = StudioTextStyles.CodeSm,
                                                color = StudioEmerald
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        StudioSecondaryButton(
                                            text = "Replace Timeline with Shots",
                                            icon = Icons.Rounded.PlaylistAddCheck,
                                            onClick = { onApplyShotPlan(state.data) },
                                            modifier = Modifier.fillMaxWidth(),
                                            testTag = "apply_timeline_storyboard_button"
                                        )
                                    }
                                }
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}
