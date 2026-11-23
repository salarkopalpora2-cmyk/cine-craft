package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AgentStageKey
import com.example.model.AutoAgentState
import com.example.model.ExportedVideoResult
import com.example.model.MissingFieldKey
import com.example.ui.components.StudioCard
import com.example.ui.components.StudioPrimaryButton
import com.example.ui.components.StudioSecondaryButton
import com.example.ui.theme.StudioAlternatingTint
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioOnSurface
import com.example.ui.theme.StudioOnSurfaceVariant
import com.example.ui.theme.StudioPreviewCanvas
import com.example.ui.theme.StudioPrimaryContainer
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioSecondaryInteractive
import com.example.ui.theme.StudioTextStyles
import com.example.viewmodel.formatMmSs

@Composable
fun PreFlightMissingModal(
    missingItems: List<MissingFieldKey>,
    onDismiss: () -> Unit,
    onFocusMissingField: (MissingFieldKey) -> Unit,
    onProceedAnyway: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioCardSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = StudioAmber
                )
                Text(
                    text = "কয়েকটি ঘর খালি আছে",
                    style = MaterialTheme.typography.titleLarge,
                    color = StudioOnSurface
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ভালো কোয়ালিটির অটো ভিডিওর জন্য নিচের তথ্যগুলো পরীক্ষা করুন:",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioOnSurfaceVariant
                )
                missingItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (item.isRequired) StudioRose.copy(alpha = 0.08f)
                                else StudioAmber.copy(alpha = 0.08f)
                            )
                            .border(
                                1.dp,
                                if (item.isRequired) StudioRose.copy(alpha = 0.25f)
                                else StudioAmber.copy(alpha = 0.25f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (item.isRequired) "প্রয়োজনীয়" else "ঐচ্ছিক",
                                style = StudioTextStyles.CodeSm,
                                color = if (item.isRequired) StudioRose else StudioAmber
                            )
                            Text(
                                text = item.titleBn,
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioOnSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(StudioPrimaryContainer)
                                .clickable { onFocusMissingField(item) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("preflight_fix_${item.name.lowercase()}")
                        ) {
                            Text(
                                text = item.actionLabelBn,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            StudioPrimaryButton(
                text = "বাদ দিয়ে ভিডিও তৈরি শুরু করুন",
                onClick = onProceedAnyway,
                modifier = Modifier.fillMaxWidth(),
                testTag = "preflight_proceed_anyway_button"
            )
        },
        dismissButton = {
            StudioSecondaryButton(
                text = "হ্যাঁ, ঘরগুলো পূরণ করবো",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                testTag = "preflight_fill_fields_button"
            )
        }
    )
}

@Composable
fun FiveStageLiveTrackerCard(
    agent: AutoAgentState,
    onPlayVoicePreview: () -> Unit,
    onMixMasterAudio: () -> Unit
) {
    StudioCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "5-Stage Execution Tracker",
                style = MaterialTheme.typography.titleMedium,
                color = StudioOnSurface
            )
            Text(
                text = "${agent.overallProgressPercent}%",
                style = StudioTextStyles.CodeMd,
                color = StudioPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { agent.overallProgressPercent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = StudioPrimaryContainer,
            trackColor = Color(0xFFE2E8F0)
        )
        Spacer(modifier = Modifier.height(12.dp))

        agent.stages.forEachIndexed { idx, stage ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (idx % 2 == 0) StudioAlternatingTint else Color.White,
                border = BorderStroke(
                    1.dp,
                    when {
                        stage.isComplete -> StudioEmerald.copy(alpha = 0.35f)
                        stage.isActive -> StudioPrimaryContainer.copy(alpha = 0.5f)
                        else -> StudioBorderSubtle
                    }
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stage.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = StudioOnSurface
                        )
                        Text(
                            text = "${stage.percent}%",
                            style = StudioTextStyles.CodeSm,
                            color = if (stage.isComplete) StudioEmerald else StudioPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stage.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioOnSurfaceVariant
                    )
                    if (stage.isComplete && stage.key == AgentStageKey.VOICE) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioSecondaryInteractive.copy(alpha = 0.12f))
                                .clickable(onClick = onPlayVoicePreview)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("tracker_voice_mp3_button")
                        ) {
                            Text(
                                text = "Script Voice Only MP3 • Listen",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioSecondaryInteractive
                            )
                        }
                    } else if (stage.isComplete && stage.key == AgentStageKey.BGM) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioEmerald.copy(alpha = 0.12f))
                                .clickable(onClick = onMixMasterAudio)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("tracker_master_audio_button")
                        ) {
                            Text(
                                text = "Final Master Audio MP3 • Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinalExportResultCard(
    result: ExportedVideoResult,
    onDownloadMp4Click: () -> Unit,
    onOpenVideoTab: () -> Unit
) {
    var isPreviewActive by remember { mutableStateOf(false) }

    StudioCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = StudioEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Final Export Ready",
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioOnSurface
                )
            }
            Text(
                text = String.format("%.1f MB", result.sizeMb),
                style = StudioTextStyles.CodeSm,
                color = StudioEmerald
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(StudioPreviewCanvas)
                .clickable { isPreviewActive = !isPreviewActive }
                .testTag("export_result_preview_box"),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_scene_neon_tokyo),
                contentDescription = result.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Preview exported video",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${result.fileName} • ${formatMmSs(result.durationSec)} • ${result.resolution}",
                    style = StudioTextStyles.CodeSm,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        StudioPrimaryButton(
            text = "MP4 ভিডিও ডাউনলোড করুন",
            icon = Icons.Rounded.Download,
            containerColor = StudioEmerald,
            onClick = onDownloadMp4Click,
            modifier = Modifier.fillMaxWidth(),
            testTag = "download_exported_mp4_button"
        )
        Spacer(modifier = Modifier.height(8.dp))
        StudioSecondaryButton(
            text = "ভিডিও টাইমলাইনে দেখুন",
            icon = Icons.Rounded.Movie,
            onClick = onOpenVideoTab,
            modifier = Modifier.fillMaxWidth(),
            testTag = "open_video_tab_button"
        )
    }
}

@Composable
fun ChannelSettingsDialog(
    agent: AutoAgentState,
    onDismiss: () -> Unit,
    onRenameChannel: (Int, String) -> Unit
) {
    val active = agent.activeChannel
    var draftName by remember(active.slot) { mutableStateOf(active.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioCardSurface,
        title = {
            Text(
                text = "Channel ${active.slot} Settings",
                style = MaterialTheme.typography.titleLarge,
                color = StudioOnSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Voice ID: ${active.voiceId} (${active.genderTag}) • ${active.scheduleText}",
                    style = StudioTextStyles.CodeSm,
                    color = StudioPrimaryContainer
                )
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    label = { Text("Channel Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            StudioPrimaryButton(
                text = "Save Channel",
                onClick = {
                    if (draftName.isNotBlank()) {
                        onRenameChannel(active.slot, draftName.trim())
                    }
                    onDismiss()
                },
                testTag = "save_channel_settings_button"
            )
        },
        dismissButton = {
            StudioSecondaryButton(
                text = "Close",
                onClick = onDismiss,
                testTag = "close_channel_settings_button"
            )
        }
    )
}
