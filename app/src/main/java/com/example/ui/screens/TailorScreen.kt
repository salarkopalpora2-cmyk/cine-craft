package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AiUiState
import com.example.model.AspectRatioPreset
import com.example.model.ExportProfile
import com.example.ui.components.PreviewMonitorFrame
import com.example.ui.components.StudioCard
import com.example.ui.components.StudioParameterChip
import com.example.ui.components.StudioPrimaryButton
import com.example.ui.components.StudioSecondaryButton
import com.example.ui.components.StudioSectionHeader
import com.example.ui.components.StudioSegmentedControl
import com.example.ui.theme.StudioAlternatingTint
import com.example.ui.theme.StudioAppCanvas
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioOnSurface
import com.example.ui.theme.StudioOnSurfaceVariant
import com.example.ui.theme.StudioPrimaryContainer
import com.example.ui.theme.StudioTextStyles
import com.example.ui.theme.StudioViolet
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.formatSmpte

@Composable
fun TailorScreen(
    uiState: StudioUiState,
    onAspectChanged: (AspectRatioPreset) -> Unit,
    onApplyPlatformPreset: (String, AspectRatioPreset, String) -> Unit,
    onUpdateExportProfile: ((ExportProfile) -> ExportProfile) -> Unit,
    onGenerateCopyPack: () -> Unit,
    onStartRenderExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resolutions = listOf("4K UHD", "1080p HD", "720p Draft")
    val frameRates = listOf("24 fps", "30 fps", "60 fps")
    val codecs = listOf("H.265 HEVC", "H.264 AVC", "ProRes 422")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 600.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StudioSectionHeader(
                title = "Tailor & Export",
                subtitle = "Smart aspect reframing, mastering specs & AI launch copy",
                trailing = {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(StudioEmerald.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = uiState.exportProfile.resolution,
                            style = StudioTextStyles.CodeSm,
                            color = StudioEmerald
                        )
                    }
                }
            )
        }

        // Live Framing Preview Monitor
        item {
            PreviewMonitorFrame(
                aspectRatio = uiState.canvasOptions.aspectRatio,
                timecode = formatSmpte(uiState.totalTimelineDurationSec),
                showGrid = false,
                showSafeZones = uiState.canvasOptions.showSafeZones,
                badgeLabel = "${uiState.canvasOptions.aspectRatio.label} • ${uiState.canvasOptions.aspectRatio.resolutionLabel}"
            ) {
                Image(
                    painter = painterResource(
                        id = uiState.activeClip?.thumbnailRes ?: R.drawable.img_scene_coastal_drone
                    ),
                    contentDescription = "Tailored export framing preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Platform & Aspect Presets Card
        item {
            StudioCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = StudioPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Platform & Frame Preset",
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioOnSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                StudioSegmentedControl(
                    options = AspectRatioPreset.entries,
                    selectedOption = uiState.canvasOptions.aspectRatio,
                    labelProvider = { it.label },
                    onOptionSelected = onAspectChanged,
                    testTagPrefix = "tailor_ratio"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioParameterChip(
                        label = "YouTube Cinema",
                        selected = uiState.exportProfile.targetPlatform == "YouTube Cinema",
                        onClick = {
                            onApplyPlatformPreset("YouTube Cinema", AspectRatioPreset.WIDE_16_9, "24 fps")
                        },
                        testTag = "preset_youtube"
                    )
                    StudioParameterChip(
                        label = "Reels / Shorts",
                        selected = uiState.exportProfile.targetPlatform == "Reels / Shorts",
                        accentColor = StudioViolet,
                        onClick = {
                            onApplyPlatformPreset("Reels / Shorts", AspectRatioPreset.VERTICAL_9_16, "60 fps")
                        },
                        testTag = "preset_reels"
                    )
                    StudioParameterChip(
                        label = "Square Feed",
                        selected = uiState.exportProfile.targetPlatform == "Square Feed",
                        accentColor = StudioEmerald,
                        onClick = {
                            onApplyPlatformPreset("Square Feed", AspectRatioPreset.SQUARE_1_1, "30 fps")
                        },
                        testTag = "preset_square"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Mastering Specs",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioOnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    resolutions.forEach { res ->
                        StudioParameterChip(
                            label = res,
                            selected = uiState.exportProfile.resolution == res,
                            icon = Icons.Rounded.HighQuality,
                            onClick = { onUpdateExportProfile { it.copy(resolution = res) } },
                            testTag = "res_chip_${res.lowercase().replace(" ", "_")}"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    frameRates.forEach { fps ->
                        StudioParameterChip(
                            label = fps,
                            selected = uiState.exportProfile.frameRate == fps,
                            onClick = { onUpdateExportProfile { it.copy(frameRate = fps) } },
                            testTag = "fps_chip_${fps.lowercase().replace(" ", "_")}"
                        )
                    }
                    codecs.forEach { codec ->
                        StudioParameterChip(
                            label = codec,
                            selected = uiState.exportProfile.codec == codec,
                            onClick = { onUpdateExportProfile { it.copy(codec = codec) } },
                            testTag = "codec_chip_${codec.lowercase().replace(" ", "_")}"
                        )
                    }
                }
            }
        }

        // Gemini AI Launch Copy Generator Card
        item {
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
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = StudioViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "AI Platform Copy Pack",
                            style = MaterialTheme.typography.titleMedium,
                            color = StudioOnSurface
                        )
                    }
                    Text(
                        text = uiState.exportProfile.targetPlatform,
                        style = StudioTextStyles.CodeSm,
                        color = StudioViolet
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                StudioSecondaryButton(
                    text = if (uiState.copyPackState is AiUiState.Loading) "Writing Copy…" else "Generate Title & Hashtags",
                    icon = Icons.Rounded.AutoAwesome,
                    enabled = uiState.copyPackState !is AiUiState.Loading,
                    onClick = onGenerateCopyPack,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "generate_tailor_copy_button"
                )

                when (val copyState = uiState.copyPackState) {
                    is AiUiState.Loading -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Tailoring metadata for ${uiState.exportProfile.targetPlatform}…",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioOnSurfaceVariant
                            )
                        }
                    }

                    is AiUiState.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        AiErrorBanner(message = copyState.message)
                    }

                    is AiUiState.Success -> {
                        val pack = copyState.data
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = StudioAlternatingTint,
                            border = BorderStroke(1.dp, StudioBorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = pack.headline,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StudioOnSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = pack.caption,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StudioOnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    pack.hashtags.forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(StudioViolet.copy(alpha = 0.12f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                style = StudioTextStyles.CodeSm,
                                                color = StudioViolet
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }

        // Render & Export Card
        item {
            StudioCard {
                StudioPrimaryButton(
                    text = if (uiState.isExporting) {
                        "Rendering ${(uiState.exportProgress * 100).toInt()}%…"
                    } else {
                        "Render & Export to Gallery"
                    },
                    icon = Icons.Rounded.IosShare,
                    enabled = !uiState.isExporting,
                    containerColor = if (uiState.isExporting) StudioPrimaryContainer else StudioEmerald,
                    onClick = onStartRenderExport,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "render_export_button"
                )

                if (uiState.isExporting) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { uiState.exportProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = StudioEmerald,
                        trackColor = StudioAppCanvas
                    )
                }

                if (!uiState.exportCompleteMessage.isNullOrBlank() && !uiState.isExporting) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioEmerald.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = StudioEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.exportCompleteMessage,
                            style = MaterialTheme.typography.labelMedium,
                            color = StudioEmerald
                        )
                    }
                }
            }
        }
    }
}
