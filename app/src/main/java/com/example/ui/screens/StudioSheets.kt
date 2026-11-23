package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AiUiState
import com.example.model.AspectRatioPreset
import com.example.model.CanvasOptions
import com.example.model.ExportProfile
import com.example.ui.components.StudioParameterChip
import com.example.ui.components.StudioPrimaryButton
import com.example.ui.components.StudioSegmentedControl
import com.example.ui.theme.StudioAlternatingTint
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioErrorContainer
import com.example.ui.theme.StudioOnErrorContainer
import com.example.ui.theme.StudioOnSurface
import com.example.ui.theme.StudioOnSurfaceVariant
import com.example.ui.theme.StudioPrimaryContainer
import com.example.ui.theme.StudioTextStyles
import com.example.ui.theme.StudioViolet
import com.example.viewmodel.StudioUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInspectorSheet(
    uiState: StudioUiState,
    onDismiss: () -> Unit,
    onRunAudit: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioCardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = StudioViolet,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "AI Project Inspector",
                    style = MaterialTheme.typography.headlineSmall,
                    color = StudioOnSurface
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Gemini 3.8 Flash audits your timeline clips, script cues, and framing setup.",
                style = MaterialTheme.typography.bodySmall,
                color = StudioOnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState.inspectorState) {
                is AiUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = StudioPrimaryContainer
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "Inspecting studio sequence…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StudioOnSurfaceVariant
                        )
                    }
                }

                is AiUiState.Error -> {
                    AiErrorBanner(message = state.message)
                }

                is AiUiState.Success -> {
                    val report = state.data
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = StudioAlternatingTint,
                        border = BorderStroke(1.dp, StudioBorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Studio Readiness",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StudioOnSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(StudioEmerald.copy(alpha = 0.14f))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${report.healthScore}/100",
                                        style = StudioTextStyles.CodeMd,
                                        color = StudioEmerald
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = report.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = StudioOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            report.recommendations.forEach { rec ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = StudioPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = rec,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudioOnSurface
                                    )
                                }
                            }
                        }
                    }
                }

                else -> Unit
            }

            Spacer(modifier = Modifier.height(16.dp))
            StudioPrimaryButton(
                text = "Re-run AI Audit",
                icon = Icons.Rounded.Refresh,
                enabled = uiState.inspectorState !is AiUiState.Loading,
                onClick = onRunAudit,
                modifier = Modifier.fillMaxWidth(),
                testTag = "rerun_inspector_button"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasOptionsSheet(
    options: CanvasOptions,
    onDismiss: () -> Unit,
    onUpdateOptions: ((CanvasOptions) -> CanvasOptions) -> Unit
) {
    val luts = listOf("CineBlue LUT", "Anamorphic Teal", "IMAX Warm", "Rec.709 Neutral")
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioCardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Canvas Options",
                style = MaterialTheme.typography.headlineSmall,
                color = StudioOnSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            StudioSegmentedControl(
                options = AspectRatioPreset.entries,
                selectedOption = options.aspectRatio,
                labelProvider = { it.label },
                onOptionSelected = { ratio -> onUpdateOptions { it.copy(aspectRatio = ratio) } },
                testTagPrefix = "sheet_canvas_ratio"
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.GridOn, contentDescription = null, tint = StudioPrimaryContainer)
                    Text("Rule of Thirds Grid", style = MaterialTheme.typography.bodyLarge, color = StudioOnSurface)
                }
                Switch(
                    checked = options.showGridLines,
                    onCheckedChange = { checked -> onUpdateOptions { it.copy(showGridLines = checked) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = StudioPrimaryContainer),
                    modifier = Modifier.testTag("sheet_grid_switch")
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Action Safe Zone Guides", style = MaterialTheme.typography.bodyLarge, color = StudioOnSurface)
                Switch(
                    checked = options.showSafeZones,
                    onCheckedChange = { checked -> onUpdateOptions { it.copy(showSafeZones = checked) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = StudioPrimaryContainer),
                    modifier = Modifier.testTag("sheet_safezone_switch")
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("Monitor Color LUT", style = MaterialTheme.typography.labelLarge, color = StudioOnSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                luts.forEach { lut ->
                    StudioParameterChip(
                        label = lut,
                        selected = options.lutPreset == lut,
                        onClick = { onUpdateOptions { it.copy(lutPreset = lut) } },
                        testTag = "lut_chip_${lut.lowercase().replace(" ", "_")}"
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportProfileSheet(
    profile: ExportProfile,
    onDismiss: () -> Unit,
    onUpdateProfile: ((ExportProfile) -> ExportProfile) -> Unit,
    onExportNow: () -> Unit
) {
    val bitrates = listOf("20 Mbps", "45 Mbps", "80 Mbps")
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioCardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Export Profile",
                style = MaterialTheme.typography.headlineSmall,
                color = StudioOnSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${profile.resolution} • ${profile.frameRate} • ${profile.codec} • ${profile.bitrate}",
                style = StudioTextStyles.CodeSm,
                color = StudioPrimaryContainer
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text("Target Bitrate", style = MaterialTheme.typography.labelLarge, color = StudioOnSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bitrates.forEach { bitrate ->
                    StudioParameterChip(
                        label = bitrate,
                        selected = profile.bitrate == bitrate,
                        onClick = { onUpdateProfile { it.copy(bitrate = bitrate) } },
                        testTag = "bitrate_chip_${bitrate.lowercase().replace(" ", "_")}"
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            StudioPrimaryButton(
                text = "Open Tailor & Render",
                onClick = onExportNow,
                modifier = Modifier.fillMaxWidth(),
                testTag = "sheet_open_tailor_button"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AiErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StudioErrorContainer,
        border = BorderStroke(1.dp, StudioError.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = "AI Error",
                tint = StudioOnErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = StudioOnErrorContainer
            )
        }
    }
}
