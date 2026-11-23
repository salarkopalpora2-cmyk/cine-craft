package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrandingWatermark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AgentMixSettings
import com.example.model.AutoAgentState
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioOnSurface
import com.example.ui.theme.StudioOnSurfaceVariant
import com.example.ui.theme.StudioPrimaryContainer
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioSecondaryInteractive
import com.example.ui.theme.StudioTextStyles
import com.example.ui.theme.StudioViolet
import com.example.viewmodel.formatMmSs

// Row 13: BGM & Mix Grid
@Composable
fun BgmMixGridRow(
    agent: AutoAgentState,
    onPickBgmClick: () -> Unit,
    onClearBgm: () -> Unit,
    onUpdateMixSettings: ((AgentMixSettings) -> AgentMixSettings) -> Unit
) {
    val speechBgmOptions = listOf(12, 18, 22, 28)
    val breakLoudOptions = listOf(70, 85, 90, 100)
    val breakTimeOptions = listOf(4, 5, 6)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // Cell 1: Add BGM / Remove BGM
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioViolet)
                        .clickable(onClick = onPickBgmClick)
                        .padding(horizontal = 10.dp)
                        .testTag("row13_add_bgm_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = agent.bgmName ?: "Add BGM",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!agent.bgmName.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioRose.copy(alpha = 0.1f))
                            .clickable(onClick = onClearBgm)
                            .testTag("row13_clear_bgm_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove BGM",
                            tint = StudioRose,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Cell 2: Speech BGM
            CompactGridDropdownCell(
                label = "Speech BGM",
                valueText = "${agent.mixSettings.speechBgmPercent}%",
                options = speechBgmOptions.map { it to "$it%" },
                onSelected = { pct -> onUpdateMixSettings { it.copy(speechBgmPercent = pct) } },
                testTag = "row13_speech_bgm_dropdown",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(1.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // Cell 3: Break Loud
            CompactGridDropdownCell(
                label = "Break Loud",
                valueText = "${agent.mixSettings.breakLoudPercent}%",
                options = breakLoudOptions.map { it to "$it%" },
                onSelected = { pct -> onUpdateMixSettings { it.copy(breakLoudPercent = pct) } },
                testTag = "row13_break_loud_dropdown",
                modifier = Modifier.weight(1f)
            )

            // Cell 4: Break Time
            CompactGridDropdownCell(
                label = "Break Time",
                valueText = "${agent.mixSettings.breakTimeSec}s",
                options = breakTimeOptions.map { it to "${it}s" },
                onSelected = { sec -> onUpdateMixSettings { it.copy(breakTimeSec = sec) } },
                testTag = "row13_break_time_dropdown",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun <T> CompactGridDropdownCell(
    label: String,
    valueText: String,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(Color.White)
            .clickable { expanded = true }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioOnSurfaceVariant
                )
                Text(
                    text = valueText,
                    style = StudioTextStyles.CodeMd,
                    color = StudioOnSurface
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Select $label",
                tint = StudioOnSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    }
                )
            }
        }
    }
}

// Row 14: Final Master Audio Player
@Composable
fun MasterAudioPlayerRow(
    agent: AutoAgentState,
    onMixPreviewAudio: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Int) -> Unit,
    onStep: (Int) -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
    onToggleMute: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onSpeedChanged: (Float) -> Unit
) {
    var speedExpanded by remember { mutableStateOf(false) }
    val speedOptions = listOf(0.75f, 0.9f, 1.0f, 1.15f, 1.25f, 1.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF6F9FF))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = StudioEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Final Master Audio",
                        style = MaterialTheme.typography.labelLarge,
                        color = StudioOnSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (agent.isMasterAudioReady) StudioEmerald.copy(alpha = 0.14f)
                                else Color(0xFFE2E8F0)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (agent.isMixingAudio) "Mixing…" else if (agent.isMasterAudioReady) "Ready" else "Idle",
                            style = StudioTextStyles.CodeSm,
                            color = if (agent.isMasterAudioReady) StudioEmerald else StudioOnSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Total: ${formatMmSs(agent.masterAudioTotalSec)}",
                    style = StudioTextStyles.CodeSm,
                    color = StudioOnSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioEmerald)
                    .clickable(enabled = !agent.isMixingAudio, onClick = onMixPreviewAudio)
                    .padding(horizontal = 12.dp)
                    .testTag("row14_mix_audio_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (agent.isMasterAudioReady) "Re-Mix Audio" else "Mix & Listen",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }

        // Seek bar + timecode
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formatMmSs(agent.masterAudioPositionSec),
                style = StudioTextStyles.CodeSm,
                color = StudioPrimaryContainer
            )
            Slider(
                value = agent.masterAudioPositionSec.toFloat(),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..agent.masterAudioTotalSec.coerceAtLeast(1).toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = StudioPrimaryContainer,
                    activeTrackColor = StudioPrimaryContainer
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("row14_audio_seek_slider")
            )
            Text(
                text = formatMmSs(agent.masterAudioTotalSec),
                style = StudioTextStyles.CodeSm,
                color = StudioOnSurfaceVariant
            )
        }

        // Playback Controls + Volume + Speed
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MiniAudioBtn(
                icon = if (agent.isMasterAudioPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDesc = if (agent.isMasterAudioPlaying) "Pause master audio" else "Play master audio",
                bg = StudioPrimaryContainer,
                tint = Color.White,
                testTag = "row14_play_pause",
                onClick = onTogglePlay
            )
            MiniAudioBtn(
                icon = Icons.Rounded.Replay10,
                contentDesc = "Back 10 seconds",
                testTag = "row14_back_10s",
                onClick = { onStep(-10) }
            )
            MiniAudioBtn(
                icon = Icons.Rounded.Forward10,
                contentDesc = "Forward 10 seconds",
                testTag = "row14_fwd_10s",
                onClick = { onStep(10) }
            )
            MiniAudioBtn(
                icon = Icons.Rounded.Replay,
                contentDesc = "Restart 00:00",
                testTag = "row14_restart",
                onClick = onRestart
            )
            MiniAudioBtn(
                icon = Icons.Rounded.Stop,
                contentDesc = "Stop master audio",
                testTag = "row14_stop",
                onClick = onStop
            )
            MiniAudioBtn(
                icon = if (agent.masterAudioMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDesc = if (agent.masterAudioMuted) "Unmute" else "Mute",
                testTag = "row14_mute",
                onClick = onToggleMute
            )
            Slider(
                value = if (agent.masterAudioMuted) 0f else agent.masterAudioVolume,
                onValueChange = onVolumeChanged,
                valueRange = 0f..1f,
                modifier = Modifier
                    .width(74.dp)
                    .testTag("row14_volume_slider")
            )
            Box {
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp))
                        .clickable { speedExpanded = true }
                        .padding(horizontal = 8.dp)
                        .testTag("row14_speed_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${agent.masterAudioSpeed}x",
                        style = StudioTextStyles.CodeSm,
                        color = StudioOnSurface
                    )
                }
                DropdownMenu(expanded = speedExpanded, onDismissRequest = { speedExpanded = false }) {
                    speedOptions.forEach { sp ->
                        DropdownMenuItem(
                            text = { Text("${sp}x") },
                            onClick = {
                                speedExpanded = false
                                onSpeedChanged(sp)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAudioBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    testTag: String,
    bg: Color = Color.White,
    tint: Color = StudioOnSurface,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

// Row 15: Export & Speed Strip
@Composable
fun ExportSpeedStripRow(
    agent: AutoAgentState,
    onUpdatePerformanceMode: (String) -> Unit,
    onUpdateExportRatio: (String) -> Unit,
    onUpdateExportFps: (Int) -> Unit
) {
    val perfOptions = listOf("Auto (Smart)", "Mobile Fast", "High Quality")
    val ratioOptions = listOf("360p", "480p", "720p", "1080p")
    val fpsOptions = listOf(20, 24, 30, 60)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFF))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Export & Speed:",
                style = MaterialTheme.typography.labelMedium,
                color = StudioOnSurfaceVariant
            )
            PillDropdownSelect(
                label = agent.performanceMode,
                options = perfOptions.map { it to it },
                onSelected = onUpdatePerformanceMode,
                testTag = "row15_perf_dropdown"
            )
            PillDropdownSelect(
                label = "Ratio: ${agent.exportRatio}",
                options = ratioOptions.map { it to it },
                onSelected = onUpdateExportRatio,
                testTag = "row15_ratio_dropdown"
            )
            PillDropdownSelect(
                label = "${agent.exportFps} FPS",
                options = fpsOptions.map { it to "$it FPS" },
                onSelected = onUpdateExportFps,
                testTag = "row15_fps_dropdown"
            )
        }
    }
}

@Composable
private fun <T> PillDropdownSelect(
    label: String,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = StudioOnSurface
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = StudioOnSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    }
                )
            }
        }
    }
}

// Row 16: Watermark + Intro + Settings (3 equal-width buttons in one row)
@Composable
fun QuickTogglesRow(
    agent: AutoAgentState,
    onToggleWatermark: () -> Unit,
    onToggleIntroPanel: () -> Unit,
    onOpenSettings: () -> Unit,
    onAttachIntro: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Watermark ON/OFF
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (agent.showWatermark) StudioEmerald else Color.White)
                    .border(
                        1.dp,
                        if (agent.showWatermark) StudioEmerald else Color(0xFFCBD5E1),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = onToggleWatermark)
                    .padding(horizontal = 6.dp)
                    .testTag("row16_watermark_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.BrandingWatermark,
                    contentDescription = null,
                    tint = if (agent.showWatermark) Color.White else StudioOnSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (agent.showWatermark) "Watermark ON" else "Watermark OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (agent.showWatermark) Color.White else StudioOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 2. Intro Video / ইন্ট্রো যুক্ত (Xs)
            val hasIntro = agent.attachedIntroDurationSec != null
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (hasIntro) StudioSecondaryInteractive else Color(0xFFEEF2FF))
                    .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleIntroPanel)
                    .padding(horizontal = 6.dp)
                    .testTag("row16_intro_video_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = if (hasIntro) Color.White else StudioSecondaryInteractive,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (hasIntro) "ইন্ট্রো যুক্ত (${agent.attachedIntroDurationSec}s)" else "Intro Video",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasIntro) Color.White else StudioSecondaryInteractive,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. Settings
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, StudioBorderSubtle, RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 6.dp)
                    .testTag("row16_settings_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = StudioOnSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioOnSurface,
                    maxLines = 1
                )
            }
        }

        // TailorStudioPanel inline drawer when Intro Video is toggled
        if (agent.showIntroPanel) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, StudioBorderSubtle)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Tailor Intro Studio Preset",
                        style = MaterialTheme.typography.labelLarge,
                        color = StudioOnSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 8).forEach { sec ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StudioPrimaryContainer)
                                    .clickable { onAttachIntro(sec) }
                                    .testTag("attach_intro_${sec}s"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+ ${sec}s Intro",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Row 17: Generate & Auto Export
@Composable
fun GenerateExportActionRow(
    agent: AutoAgentState,
    onRemoveAttachedIntro: () -> Unit,
    onGenerateClick: () -> Unit,
    onStopPipeline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5FE))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (agent.attachedIntroDurationSec != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ইন্ট্রো ভিডিও সংযুক্ত (${agent.attachedIntroDurationSec}s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioSecondaryInteractive
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioRose.copy(alpha = 0.12f))
                        .clickable(onClick = onRemoveAttachedIntro)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("row17_remove_intro_button")
                ) {
                    Text(
                        text = "বাদ দিন",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioRose
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF2563EB),
                                Color(0xFF4F46E5),
                                Color(0xFF7C3AED)
                            )
                        )
                    )
                    .clickable(enabled = !agent.isAgentRunning, onClick = onGenerateClick)
                    .testTag("row17_generate_auto_export_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (agent.isAgentRunning) {
                            "Generating (${agent.overallProgressPercent}%)…"
                        } else {
                            "Generate & Auto Export"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            if (agent.isAgentRunning) {
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(StudioRose)
                        .clickable(onClick = onStopPipeline)
                        .padding(horizontal = 16.dp)
                        .testTag("row17_stop_pipeline_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Stop",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}
