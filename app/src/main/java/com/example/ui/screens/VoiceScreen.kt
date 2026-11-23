package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AiUiState
import com.example.model.ScriptLine
import com.example.model.VoicePersona
import com.example.ui.components.StudioCard
import com.example.ui.components.StudioParameterChip
import com.example.ui.components.StudioPrimaryButton
import com.example.ui.components.StudioPromptTextField
import com.example.ui.components.StudioSecondaryButton
import com.example.ui.components.StudioSectionHeader
import com.example.ui.theme.StudioAlternatingTint
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioOnSurface
import com.example.ui.theme.StudioOnSurfaceVariant
import com.example.ui.theme.StudioPreviewCanvas
import com.example.ui.theme.StudioPrimaryContainer
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioTextStyles
import com.example.ui.theme.StudioViolet
import com.example.viewmodel.StudioUiState
import kotlin.math.sin

@Composable
fun VoiceScreen(
    uiState: StudioUiState,
    onSelectPersona: (VoicePersona) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSelectScriptLine: (String) -> Unit,
    onToggleSpeakLine: (ScriptLine) -> Unit,
    onPolishSelectedLine: () -> Unit,
    onAddScriptLine: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newLineText by remember { mutableStateOf("") }
    val selectedLine = uiState.scriptLines.firstOrNull { it.id == uiState.selectedScriptLineId }
        ?: uiState.scriptLines.firstOrNull()
    val isSpeaking = uiState.isSpeakingLineId != null

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 600.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StudioSectionHeader(
                title = "Voice & Script Booth",
                subtitle = "Dual-script narration, neural persona tuning & live TTS preview",
                trailing = {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSpeaking) StudioEmerald.copy(alpha = 0.14f)
                                else StudioViolet.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSpeaking) "ON AIR" else "48kHz STUDIO",
                            style = StudioTextStyles.CodeSm,
                            color = if (isSpeaking) StudioEmerald else StudioViolet
                        )
                    }
                }
            )
        }

        // Waveform & Voice Persona Card
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
                            imageVector = Icons.Rounded.RecordVoiceOver,
                            contentDescription = null,
                            tint = StudioViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Voice Persona",
                            style = MaterialTheme.typography.titleMedium,
                            color = StudioOnSurface
                        )
                    }
                    Text(
                        text = uiState.activePersona.tone,
                        style = StudioTextStyles.CodeSm,
                        color = StudioPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.personas.forEach { persona ->
                        StudioParameterChip(
                            label = persona.name,
                            selected = persona.id == uiState.selectedPersonaId,
                            icon = Icons.Rounded.GraphicEq,
                            accentColor = StudioViolet,
                            onClick = { onSelectPersona(persona) },
                            testTag = "persona_chip_${persona.id}"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                // Deep Midnight Waveform Monitor
                VoiceWaveformMonitor(isActive = isSpeaking)

                Spacer(modifier = Modifier.height(12.dp))
                // Pitch & Speed Sliders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pitch", style = MaterialTheme.typography.labelMedium, color = StudioOnSurfaceVariant)
                            Text(
                                text = String.format("%.2fx", uiState.voicePitch),
                                style = StudioTextStyles.CodeSm,
                                color = StudioPrimaryContainer
                            )
                        }
                        Slider(
                            value = uiState.voicePitch,
                            onValueChange = onPitchChanged,
                            valueRange = 0.7f..1.4f,
                            colors = SliderDefaults.colors(
                                thumbColor = StudioPrimaryContainer,
                                activeTrackColor = StudioPrimaryContainer
                            ),
                            modifier = Modifier.testTag("voice_pitch_slider")
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tempo", style = MaterialTheme.typography.labelMedium, color = StudioOnSurfaceVariant)
                            Text(
                                text = String.format("%.2fx", uiState.voiceSpeed),
                                style = StudioTextStyles.CodeSm,
                                color = StudioPrimaryContainer
                            )
                        }
                        Slider(
                            value = uiState.voiceSpeed,
                            onValueChange = onSpeedChanged,
                            valueRange = 0.75f..1.4f,
                            colors = SliderDefaults.colors(
                                thumbColor = StudioPrimaryContainer,
                                activeTrackColor = StudioPrimaryContainer
                            ),
                            modifier = Modifier.testTag("voice_speed_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedLine != null) {
                        val speakingSelected = uiState.isSpeakingLineId == selectedLine.id
                        StudioPrimaryButton(
                            text = if (speakingSelected) "Stop Preview" else "Speak Line",
                            icon = if (speakingSelected) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            containerColor = if (speakingSelected) StudioRose else StudioPrimaryContainer,
                            onClick = { onToggleSpeakLine(selectedLine) },
                            modifier = Modifier.weight(1f),
                            testTag = "speak_selected_line_button"
                        )
                    }
                    StudioSecondaryButton(
                        text = if (uiState.voicePolishState is AiUiState.Loading) "Polishing…" else "AI Polish",
                        icon = Icons.Rounded.AutoAwesome,
                        enabled = uiState.voicePolishState !is AiUiState.Loading && selectedLine != null,
                        onClick = onPolishSelectedLine,
                        modifier = Modifier.weight(1f),
                        testTag = "ai_polish_line_button"
                    )
                }

                if (uiState.voicePolishState is AiUiState.Error) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AiErrorBanner(message = uiState.voicePolishState.message)
                }
            }
        }

        // Script Line Tracker Header
        item {
            Text(
                text = "Script Line Tracker (${uiState.scriptLines.size})",
                style = MaterialTheme.typography.titleMedium,
                color = StudioOnSurface
            )
        }

        // Alternating #F9FBFF / #FFFFFF Script Lines
        itemsIndexed(uiState.scriptLines, key = { _, line -> line.id }) { index, line ->
            val isSelected = line.id == uiState.selectedScriptLineId
            val isLineSpeaking = uiState.isSpeakingLineId == line.id
            val rowBg = if (index % 2 == 0) StudioCardSurface else StudioAlternatingTint

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectScriptLine(line.id) }
                    .testTag("script_line_card_${line.id}"),
                shape = RoundedCornerShape(24.dp),
                color = rowBg,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) StudioPrimaryContainer else StudioBorderSubtle
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) StudioPrimaryContainer.copy(alpha = 0.12f)
                                        else StudioAlternatingTint
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = line.speaker,
                                    style = StudioTextStyles.CodeSm,
                                    color = if (isSelected) StudioPrimaryContainer else StudioOnSurfaceVariant
                                )
                            }
                            Text(
                                text = line.timecode,
                                style = StudioTextStyles.CodeSm,
                                color = StudioOnSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isLineSpeaking) StudioRose.copy(alpha = 0.12f)
                                    else StudioPrimaryContainer.copy(alpha = 0.10f)
                                )
                                .clickable { onToggleSpeakLine(line) }
                                .testTag("play_line_button_${line.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLineSpeaking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = if (isLineSpeaking) "Stop line" else "Play line",
                                tint = if (isLineSpeaking) StudioRose else StudioPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = StudioOnSurface
                    )
                    if (!line.secondaryText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = line.secondaryText,
                            style = StudioTextStyles.DualScriptBody,
                            color = StudioOnSurfaceVariant
                        )
                    }
                }
            }
        }

        // Add Script Line Input
        item {
            StudioCard(contentPadding = PaddingValues(12.dp)) {
                StudioPromptTextField(
                    value = newLineText,
                    onValueChange = { newLineText = it },
                    placeholder = "Add new narration cue…",
                    onSubmit = {
                        onAddScriptLine("NARRATOR", newLineText)
                        newLineText = ""
                    },
                    testTag = "add_script_line_input",
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(StudioPrimaryContainer)
                                .clickable {
                                    onAddScriptLine("NARRATOR", newLineText)
                                    newLineText = ""
                                }
                                .testTag("add_script_line_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add script cue",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun VoiceWaveformMonitor(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StudioPreviewCanvas)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barCount = 34
            val gap = size.width / barCount
            val barWidth = (gap * 0.52f).coerceAtLeast(3.dp.toPx())
            for (i in 0 until barCount) {
                val base = if (isActive) {
                    (0.30f + 0.65f * kotlin.math.abs(sin(phase + i * 0.42f)))
                } else {
                    (0.18f + 0.28f * kotlin.math.abs(sin(i * 0.65f)))
                }
                val barHeight = size.height * base
                val top = (size.height - barHeight) / 2f
                val color = if (isActive) {
                    if (i % 4 == 0) Color(0xFF34D399) else Color(0xFF60A5FA)
                } else {
                    Color(0xFF60A5FA).copy(alpha = 0.55f)
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(i * gap, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
