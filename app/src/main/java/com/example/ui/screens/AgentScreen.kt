package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.components.SharpCinemaPreviewPlayer
import com.example.model.AgentMixSettings
import com.example.model.AutoAgentState
import com.example.model.MissingFieldKey
import com.example.model.VoiceSourceMode
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
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.formatMmSs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(
    uiState: StudioUiState,
    onTogglePreviewPlaying: () -> Unit,
    onSeekPreview: (Float) -> Unit,
    onRewindPreview: () -> Unit,
    onPreviewVolumeChanged: (Float) -> Unit,
    onTogglePreviewMute: () -> Unit,
    onUpdateReleaseDate: (String) -> Unit,
    onSelectSeriesById: (String) -> Unit,
    onToggleLowerThird: () -> Unit,
    onUpdateActiveEpisode: (String) -> Unit,
    onStepEpisode: (Int) -> Unit,
    onSelectChannelSlot: (Int) -> Unit,
    onRenameChannel: (Int, String) -> Unit,
    onToggleChannelNameQuick: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onToggleEffects: () -> Unit,
    onToggleCtaQuick: () -> Unit,
    onUploadVideo: (String, String, Int) -> Unit,
    onClearVideo: () -> Unit,
    onStepZoomLevel: (Float) -> Unit,
    onAdjustStartTrim: (Int) -> Unit,
    onAdjustEndTrim: (Int) -> Unit,
    onAdjustFinalDuration: (Int) -> Unit,
    onResetFinalDurationAuto: () -> Unit,
    onAdaptScriptToFinalDuration: () -> Unit,
    onChangePreferredVoiceSource: (VoiceSourceMode) -> Unit,
    onUploadRecordedVoice: (String, String) -> Unit,
    onClearRecordedVoice: () -> Unit,
    onStartMicRecording: () -> Unit,
    onStopMicRecording: () -> Unit,
    onToggleForceRegenerateVoice: () -> Unit,
    onScriptTextChange: (String) -> Unit,
    onDraftScriptWithAi: () -> Unit,
    onUploadBgm: (String, String) -> Unit,
    onClearBgm: () -> Unit,
    onUpdateMixSettings: ((AgentMixSettings) -> AgentMixSettings) -> Unit,
    onMixPreviewAudio: () -> Unit,
    onToggleMasterAudioPlay: () -> Unit,
    onSeekMasterAudio: (Int) -> Unit,
    onStepMasterAudio: (Int) -> Unit,
    onRestartMasterAudio: () -> Unit,
    onStopMasterAudio: () -> Unit,
    onToggleMasterAudioMute: () -> Unit,
    onMasterAudioVolumeChanged: (Float) -> Unit,
    onMasterAudioSpeedChanged: (Float) -> Unit,
    onUpdatePerformanceMode: (String) -> Unit,
    onUpdateExportRatio: (String) -> Unit,
    onUpdateExportFps: (Int) -> Unit,
    onToggleWatermarkQuick: () -> Unit,
    onToggleIntroPanel: () -> Unit,
    onAttachIntroVideo: (Int) -> Unit,
    onRemoveAttachedIntro: () -> Unit,
    onSetChannelSettingsVisible: (Boolean) -> Unit,
    onGenerateClick: () -> Unit,
    onStopPipeline: () -> Unit,
    onDismissPreFlight: () -> Unit,
    onFocusMissingField: (MissingFieldKey) -> Unit,
    onProceedPastPreFlight: () -> Unit,
    onOpenVideoTab: () -> Unit,
    onDownloadExportedMp4: () -> Unit,
    modifier: Modifier = Modifier
) {
    val agent = uiState.autoAgent
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = queryFileName(context, uri) ?: "Source_Video.mp4"
            val dur = queryVideoDurationSec(context, uri)
            onUploadVideo(uri.toString(), name, dur)
        }
    }

    val voiceAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = queryFileName(context, uri) ?: "Recorded_Voice.m4a"
            onUploadRecordedVoice(uri.toString(), name)
        }
    }

    val bgmAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = queryFileName(context, uri) ?: "Studio_BGM.mp3"
            onUploadBgm(uri.toString(), name)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartMicRecording()
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(agent.focusedFieldKey) {
        if (agent.focusedFieldKey != null) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main 17-Row Luminous Material 3 Card / Table (Sharp 4-corner full-width table)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                color = StudioCardSurface,
                border = BorderStroke(1.dp, StudioBorderSubtle)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // TOP 16:9 SHARP-CORNER CINEMA PREVIEW + BOTTOM PLAYER CONTROL BAR
                    TopCinemaPreviewBox(
                        agent = agent,
                        onTogglePlay = onTogglePreviewPlaying,
                        onSeek = onSeekPreview,
                        onRewind = onRewindPreview,
                        onVolumeChange = onPreviewVolumeChanged,
                        onToggleMute = onTogglePreviewMute,
                        onCancelPipeline = onStopPipeline
                    )

                    RowDivider()

                    // ROW 1: Date (Release Date)
                    ReleaseDateRow(
                        agent = agent,
                        onOpenDatePicker = { showDatePicker = true },
                        onClearDate = { onUpdateReleaseDate("") }
                    )
                    RowDivider()

                    // ROW 2: Drama + Title ON/OFF
                    DramaSelectorRow(
                        agent = agent,
                        onSelectSeries = onSelectSeriesById,
                        onToggleLowerThird = onToggleLowerThird
                    )
                    RowDivider()

                    // ROW 3: Episode Stepper (Bengali + English numerals)
                    EpisodeStepperRow(
                        agent = agent,
                        onUpdateEpisode = onUpdateActiveEpisode,
                        onStepEpisode = onStepEpisode
                    )
                    RowDivider()

                    // ROW 4: Channel + Channel Name ON/OFF
                    ChannelSelectorRow(
                        agent = agent,
                        onSelectChannelSlot = onSelectChannelSlot,
                        onToggleChannelName = onToggleChannelNameQuick
                    )
                    RowDivider()

                    // ROW 5 (3 Sub-rows): Video Additions
                    VideoAdditionsSubRows(
                        agent = agent,
                        onToggleSubtitles = onToggleSubtitles,
                        onToggleEffects = onToggleEffects,
                        onToggleCta = onToggleCtaQuick
                    )
                    RowDivider()

                    // ROW 6: Source Video Upload
                    SourceVideoUploadRow(
                        agent = agent,
                        onUploadClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        onClearVideo = onClearVideo
                    )
                    RowDivider()

                    // ROW 7: Zoom Crop Level (100% - 250%, default 140%)
                    ZoomCropLevelRow(
                        zoomLevel = agent.zoomLevel,
                        onStepZoom = onStepZoomLevel
                    )
                    RowDivider()

                    // ROW 8: Start Trim & End Trim
                    StartEndTrimRow(
                        agent = agent,
                        onAdjustStart = onAdjustStartTrim,
                        onAdjustEnd = onAdjustEndTrim
                    )
                    RowDivider()

                    // ROW 9: Final Duration Bar
                    FinalDurationBarRow(
                        agent = agent,
                        onAdjustDuration = onAdjustFinalDuration,
                        onResetAuto = onResetFinalDurationAuto,
                        onFitScript = onAdaptScriptToFinalDuration
                    )
                    RowDivider()

                    // ROW 10: Voice Mode Switcher
                    VoiceModeSwitcherRow(
                        mode = agent.preferredVoiceSource,
                        onModeSelected = onChangePreferredVoiceSource
                    )
                    RowDivider()

                    // ROW 11: Voice Action Controls
                    VoiceActionControlsRow(
                        agent = agent,
                        onUploadVoiceClick = { voiceAudioPickerLauncher.launch("audio/*") },
                        onClearVoice = onClearRecordedVoice,
                        onMicToggle = {
                            if (agent.isMicRecording) {
                                onStopMicRecording()
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    onStartMicRecording()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onSpeedSelected = { sp ->
                            onUpdateMixSettings { it.copy(speechSpeed = sp) }
                        },
                        onDraftWithAi = onDraftScriptWithAi,
                        onToggleCached = onToggleForceRegenerateVoice
                    )

                    // ROW 12: Review Script (Only in AI Script mode)
                    if (agent.preferredVoiceSource == VoiceSourceMode.SCRIPT) {
                        RowDivider()
                        ReviewScriptRow(
                            agent = agent,
                            onScriptChange = onScriptTextChange
                        )
                    }

                    RowDivider()

                    // ROW 13: BGM & Mix Grid
                    BgmMixGridRow(
                        agent = agent,
                        onPickBgmClick = { bgmAudioPickerLauncher.launch("audio/*") },
                        onClearBgm = onClearBgm,
                        onUpdateMixSettings = onUpdateMixSettings
                    )
                    RowDivider()

                    // ROW 14: Final Master Audio Player
                    MasterAudioPlayerRow(
                        agent = agent,
                        onMixPreviewAudio = onMixPreviewAudio,
                        onTogglePlay = onToggleMasterAudioPlay,
                        onSeek = onSeekMasterAudio,
                        onStep = onStepMasterAudio,
                        onRestart = onRestartMasterAudio,
                        onStop = onStopMasterAudio,
                        onToggleMute = onToggleMasterAudioMute,
                        onVolumeChanged = onMasterAudioVolumeChanged,
                        onSpeedChanged = onMasterAudioSpeedChanged
                    )
                    RowDivider()

                    // ROW 15: Export & Speed Strip
                    ExportSpeedStripRow(
                        agent = agent,
                        onUpdatePerformanceMode = onUpdatePerformanceMode,
                        onUpdateExportRatio = onUpdateExportRatio,
                        onUpdateExportFps = onUpdateExportFps
                    )
                    RowDivider()

                    // ROW 16: Watermark + Intro + Settings
                    QuickTogglesRow(
                        agent = agent,
                        onToggleWatermark = onToggleWatermarkQuick,
                        onToggleIntroPanel = onToggleIntroPanel,
                        onOpenSettings = { onSetChannelSettingsVisible(true) },
                        onAttachIntro = onAttachIntroVideo
                    )
                    RowDivider()

                    // ROW 17: Generate & Auto Export
                    GenerateExportActionRow(
                        agent = agent,
                        onRemoveAttachedIntro = onRemoveAttachedIntro,
                        onGenerateClick = onGenerateClick,
                        onStopPipeline = onStopPipeline
                    )
                }
            }
        }

        // 5-Stage Live Execution Tracker
        if (agent.isAgentRunning || agent.stages.any { it.percent > 0 }) {
            item {
                FiveStageLiveTrackerCard(
                    agent = agent,
                    onPlayVoicePreview = onToggleMasterAudioPlay,
                    onMixMasterAudio = onMixPreviewAudio
                )
            }
        }

        // Final Export Result Card
        if (agent.lastExportedVideo != null) {
            item {
                FinalExportResultCard(
                    result = agent.lastExportedVideo,
                    onDownloadMp4Click = onDownloadExportedMp4,
                    onOpenVideoTab = onOpenVideoTab
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            onUpdateReleaseDate(fmt.format(Date(millis)))
                        }
                        showDatePicker = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (agent.showPreFlightModal) {
        PreFlightMissingModal(
            missingItems = agent.missingItemsList,
            onDismiss = onDismissPreFlight,
            onFocusMissingField = onFocusMissingField,
            onProceedAnyway = onProceedPastPreFlight
        )
    }

    if (agent.showChannelSettingsModal) {
        ChannelSettingsDialog(
            agent = agent,
            onDismiss = { onSetChannelSettingsVisible(false) },
            onRenameChannel = onRenameChannel
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1F5F9))
}

@Composable
private fun TopCinemaPreviewBox(
    agent: AutoAgentState,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onRewind: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onCancelPipeline: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "previewPan")
    val panProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pan"
    )
    val effectivePan = if (agent.isPreviewPlaying) panProgress * 12f else 0f
    val totalPreviewDur = agent.effectiveFinalDurationSec.toFloat().coerceAtLeast(15f)
    val trimDur = agent.uploadedVideoDurationSec.toFloat().coerceAtLeast(1f)
    val trimStartPct = (agent.startTrimSec / trimDur).coerceIn(0f, 1f)
    val trimEndPct = (agent.endTrimSec / trimDur).coerceIn(trimStartPct, 1f)

    SharpCinemaPreviewPlayer(
        isPlaying = agent.isPreviewPlaying,
        currentTimeSec = agent.previewPositionSec,
        totalDurationSec = totalPreviewDur,
        volume = agent.previewVolume,
        isMuted = agent.previewMuted,
        onTogglePlay = onTogglePlay,
        onSeek = onSeek,
        onVolumeChange = onVolumeChange,
        onToggleMute = onToggleMute,
        onRewind = onRewind,
        trimStartPct = trimStartPct,
        trimEndPct = trimEndPct,
        activeStatusLabel = if (agent.isAgentRunning) "${agent.currentStageTitle} · ${agent.overallProgressPercent}%" else null,
        onCancelStatusAction = if (agent.isAgentRunning) onCancelPipeline else null,
        extraBadgeText = "Zoom ${(agent.zoomLevel * 100).toInt()}%",
        previewTestTag = "agent_top_preview_canvas"
    ) {
        // 1. Zoom/Pan Source Frame
        Image(
            painter = painterResource(id = R.drawable.img_scene_neon_tokyo),
            contentDescription = "Live Cinema Canvas Preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = agent.zoomLevel
                    scaleY = agent.zoomLevel
                    translationX = effectivePan
                }
        )

        // 2. Video Effects Overlay (Cinematic anamorphic glow + vignette)
        if (agent.showEffects) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x66070E1C)
                        )
                    )
                )
                drawLine(
                    color = Color(0x5560A5FA),
                    start = Offset(0f, size.height * 0.28f),
                    end = Offset(size.width, size.height * 0.28f),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // 3. Channel Name, Watermark & Subscribe CTA
        if (agent.showChannelName) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "CH ${agent.selectedChannelSlot} · ${agent.activeChannel.name}",
                    style = StudioTextStyles.CodeSm,
                    color = Color.White
                )
            }
        }

        if (agent.showWatermark) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "CINECRAFT",
                    style = StudioTextStyles.CodeSm.copy(fontSize = 9.sp),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        if (agent.showCta) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 36.dp, end = 10.dp)
                    .clip(CircleShape)
                    .background(StudioRose)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "SUBSCRIBE",
                    style = StudioTextStyles.CodeSm.copy(fontSize = 9.sp),
                    color = Color.White
                )
            }
        }

        // 4. Broadcast Lower-Third + Bengali Subtitle
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (agent.showSubtitles && agent.scriptText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = agent.scriptText.take(64),
                        style = StudioTextStyles.DualScriptBody.copy(fontSize = 11.sp, lineHeight = 15.sp),
                        color = Color(0xFFFEF08A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (agent.showLowerThird) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = agent.activeSeries?.title ?: "Drama Title",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${agent.activeEpisode} • ${agent.activeChannel.scheduleText}",
                            style = StudioTextStyles.CodeSm.copy(fontSize = 10.sp),
                            color = Color(0xFF93C5FD),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (agent.releaseDate.isNotBlank()) {
                        Text(
                            text = formatShortEnDate(agent.releaseDate),
                            style = StudioTextStyles.CodeSm,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

// Row 1: Release Date
@Composable
private fun ReleaseDateRow(
    agent: AutoAgentState,
    onOpenDatePicker: () -> Unit,
    onClearDate: () -> Unit
) {
    val highlightEmpty = agent.highlightMissingFields && agent.releaseDate.isBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(if (highlightEmpty) Color(0xFFFFFBEB) else Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = StudioPrimaryContainer,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Date",
            style = MaterialTheme.typography.labelLarge,
            color = StudioOnSurface
        )
        Box(
            modifier = Modifier
                .width(126.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF0F9FF))
                .border(
                    1.dp,
                    if (highlightEmpty) Color(0xFFFBBF24) else Color(0xFFBAE6FD),
                    RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onOpenDatePicker)
                .padding(horizontal = 10.dp)
                .testTag("row1_date_picker_box"),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = agent.releaseDate.ifBlank { "Pick date" },
                style = StudioTextStyles.CodeSm,
                color = if (agent.releaseDate.isBlank()) StudioAmber else StudioOnSurface
            )
        }
        Text(
            text = if (agent.releaseDate.isBlank()) "Not set" else formatShortEnDate(agent.releaseDate),
            style = MaterialTheme.typography.bodySmall,
            color = StudioOnSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (agent.releaseDate.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable(onClick = onClearDate)
                    .testTag("row1_clear_date_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = "Clear release date",
                    tint = StudioOnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Row 2: Drama + Title ON/OFF
@Composable
private fun DramaSelectorRow(
    agent: AutoAgentState,
    onSelectSeries: (String) -> Unit,
    onToggleLowerThird: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(StudioAlternatingTint)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Movie,
            contentDescription = null,
            tint = StudioAmber,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Drama",
            style = MaterialTheme.typography.labelLarge,
            color = StudioOnSurface
        )
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xB3FFFBEB))
                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 10.dp)
                    .testTag("row2_drama_dropdown"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = agent.activeSeries?.title ?: "Select Drama…",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = StudioAmber,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                agent.activeSeriesList.forEach { series ->
                    DropdownMenuItem(
                        text = { Text(series.title) },
                        onClick = {
                            expanded = false
                            onSelectSeries(series.id)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Clear Drama") },
                    onClick = {
                        expanded = false
                        onSelectSeries("")
                    }
                )
            }
        }
        OnOffEyeToggleButton(
            isOn = agent.showLowerThird,
            onClick = onToggleLowerThird,
            testTag = "row2_lower_third_toggle"
        )
    }
}

// Row 3: Episode Stepper (supports Bengali ০-৯ and English 0-9)
@Composable
private fun EpisodeStepperRow(
    agent: AutoAgentState,
    onUpdateEpisode: (String) -> Unit,
    onStepEpisode: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Episode",
            style = MaterialTheme.typography.labelLarge,
            color = StudioOnSurface
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable { onStepEpisode(-1) }
                    .testTag("row3_episode_minus"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Previous episode",
                    tint = StudioOnSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(86.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, StudioBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = agent.activeEpisode,
                    onValueChange = onUpdateEpisode,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.labelLarge.copy(
                        color = StudioOnSurface,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(StudioPrimaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("row3_episode_input")
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioSecondaryInteractive)
                    .clickable { onStepEpisode(1) }
                    .testTag("row3_episode_plus"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Next episode",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Row 4: Channel + Channel Name ON/OFF
@Composable
private fun ChannelSelectorRow(
    agent: AutoAgentState,
    onSelectChannelSlot: (Int) -> Unit,
    onToggleChannelName: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val activeCh = agent.activeChannel

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(StudioAlternatingTint)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEEF2FF))
                    .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp)
                    .testTag("row4_channel_dropdown"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CH ${activeCh.slot} · ${activeCh.name} (${activeCh.genderTag})",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF312E81),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Select channel slot",
                    tint = Color(0xFF312E81),
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                agent.channelProfiles.forEach { profile ->
                    DropdownMenuItem(
                        text = {
                            Text("CH ${profile.slot} · ${profile.name} [${profile.voiceId} (${profile.genderTag})]")
                        },
                        onClick = {
                            expanded = false
                            onSelectChannelSlot(profile.slot)
                        }
                    )
                }
            }
        }
        OnOffEyeToggleButton(
            isOn = agent.showChannelName,
            onClick = onToggleChannelName,
            testTag = "row4_channel_name_toggle"
        )
    }
}

// Row 5 (3 Sub-rows): Video Additions
@Composable
private fun VideoAdditionsSubRows(
    agent: AutoAgentState,
    onToggleSubtitles: () -> Unit,
    onToggleEffects: () -> Unit,
    onToggleCta: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AdditionSubRow(
            icon = Icons.Rounded.ClosedCaption,
            label = "Subtitles",
            isOn = agent.showSubtitles,
            bg = Color.White,
            onClick = onToggleSubtitles,
            testTag = "row5_subtitles_toggle"
        )
        RowDivider()
        AdditionSubRow(
            icon = Icons.Rounded.AutoAwesome,
            label = "Video Effects",
            isOn = agent.showEffects,
            bg = StudioAlternatingTint,
            onClick = onToggleEffects,
            testTag = "row5_effects_toggle"
        )
        RowDivider()
        AdditionSubRow(
            icon = Icons.Rounded.NotificationsActive,
            label = "Subscribe CTA",
            isOn = agent.showCta,
            bg = Color.White,
            onClick = onToggleCta,
            testTag = "row5_cta_toggle"
        )
    }
}

@Composable
private fun AdditionSubRow(
    icon: ImageVector,
    label: String,
    isOn: Boolean,
    bg: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(bg)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StudioPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = StudioOnSurface
            )
        }
        OnOffPillButton(isOn = isOn, onClick = onClick, testTag = testTag)
    }
}

// Row 6: Source Video Upload
@Composable
private fun SourceVideoUploadRow(
    agent: AutoAgentState,
    onUploadClick: () -> Unit,
    onClearVideo: () -> Unit
) {
    val highlightEmpty = agent.highlightMissingFields && agent.uploadedVideoName.isNullOrBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(if (highlightEmpty) Color(0xFFFFFBEB) else Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(StudioPrimaryContainer)
                .clickable(onClick = onUploadClick)
                .padding(horizontal = 12.dp)
                .testTag("row6_upload_video_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.UploadFile,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (!agent.uploadedVideoName.isNullOrBlank()) {
                    "${agent.uploadedVideoName} (${formatMmSs(agent.uploadedVideoDurationSec)})"
                } else {
                    "Upload Video"
                },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!agent.uploadedVideoName.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioRose.copy(alpha = 0.1f))
                    .clickable(onClick = onClearVideo)
                    .testTag("row6_clear_video_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove Video",
                    tint = StudioRose,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Row 7: Zoom Crop Level (1.0 .. 2.5, default 1.4 = 140%)
@Composable
private fun ZoomCropLevelRow(
    zoomLevel: Float,
    onStepZoom: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(StudioAlternatingTint)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Zoom Crop Level",
            style = MaterialTheme.typography.labelLarge,
            color = StudioOnSurface
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, StudioBorderSubtle, RoundedCornerShape(12.dp))
                    .clickable(enabled = zoomLevel > 1.01f) { onStepZoom(-0.1f) }
                    .testTag("row7_zoom_minus"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Decrease zoom",
                    tint = StudioOnSurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F9FF))
                    .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(zoomLevel * 100).toInt()}%",
                    style = StudioTextStyles.CodeMd,
                    color = Color(0xFF1D4ED8)
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioPrimaryContainer)
                    .clickable(enabled = zoomLevel < 2.49f) { onStepZoom(0.1f) }
                    .testTag("row7_zoom_plus"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Increase zoom",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Row 8: Start Trim & End Trim
@Composable
private fun StartEndTrimRow(
    agent: AutoAgentState,
    onAdjustStart: (Int) -> Unit,
    onAdjustEnd: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        TrimHalfColumn(
            title = "Start Trim",
            totalSec = agent.startTrimSec,
            accent = Color(0xFFB45309),
            onAdjust = onAdjustStart,
            testTagPrefix = "row8_start",
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(74.dp)
                .background(Color(0xFFF1F5F9))
        )
        TrimHalfColumn(
            title = "End Trim",
            totalSec = agent.endTrimSec,
            accent = Color(0xFFBE123C),
            onAdjust = onAdjustEnd,
            testTagPrefix = "row8_end",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TrimHalfColumn(
    title: String,
    totalSec: Int,
    accent: Color,
    onAdjust: (Int) -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val mins = totalSec / 60
    val secs = totalSec % 60
    Column(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = accent
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable { onAdjust(-10) }
                    .padding(horizontal = 8.dp)
                    .testTag("${testTagPrefix}_minus_10"),
                contentAlignment = Alignment.Center
            ) {
                Text("-10s", style = StudioTextStyles.CodeSm, color = StudioOnSurface)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${mins.toString().padStart(2, '0')}m:${secs.toString().padStart(2, '0')}s",
                    style = StudioTextStyles.CodeSm,
                    color = StudioOnSurface
                )
            }
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable { onAdjust(10) }
                    .padding(horizontal = 8.dp)
                    .testTag("${testTagPrefix}_plus_10"),
                contentAlignment = Alignment.Center
            ) {
                Text("+10s", style = StudioTextStyles.CodeSm, color = StudioOnSurface)
            }
        }
    }
}

// Row 9: Final Duration Bar (with press-and-hold 150ms repeat!)
@Composable
private fun FinalDurationBarRow(
    agent: AutoAgentState,
    onAdjustDuration: (Int) -> Unit,
    onResetAuto: () -> Unit,
    onFitScript: () -> Unit
) {
    val total = agent.effectiveFinalDurationSec
    val mins = total / 60
    val secs = total % 60
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x80ECFDF5))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Final Duration",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioOnSurface
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(StudioEmerald.copy(alpha = 0.14f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = agent.durationModeLabel,
                        style = StudioTextStyles.CodeSm,
                        color = StudioEmerald
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioSecondaryInteractive)
                        .clickable(enabled = !agent.isDraftingScriptWithAi, onClick = onFitScript)
                        .padding(horizontal = 8.dp)
                        .testTag("row9_fit_script_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (agent.isDraftingScriptWithAi) "Fitting…" else "Fit Script",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp))
                        .clickable(onClick = onResetAuto)
                        .padding(horizontal = 8.dp)
                        .testTag("row9_auto_reset_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Auto",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioOnSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, StudioBorderSubtle, CircleShape)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${agent.estimatedShotsCount}p",
                        style = StudioTextStyles.CodeSm,
                        color = StudioPrimaryContainer
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minute Stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HoldRepeatStepButton("-1m", "row9_minus_1m", scope) { onAdjustDuration(-60) }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text("${mins}m", style = StudioTextStyles.CodeSm, color = StudioOnSurface)
                }
                HoldRepeatStepButton("+1m", "row9_plus_1m", scope) { onAdjustDuration(60) }
            }

            // Second Stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HoldRepeatStepButton("-5s", "row9_minus_5s", scope) { onAdjustDuration(-5) }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text("${secs}s", style = StudioTextStyles.CodeSm, color = StudioOnSurface)
                }
                HoldRepeatStepButton("+5s", "row9_plus_5s", scope) { onAdjustDuration(5) }
            }

            Text(
                text = agent.durationLockLabel,
                style = StudioTextStyles.CodeSm,
                color = StudioEmerald
            )
        }
    }
}

@Composable
private fun HoldRepeatStepButton(
    label: String,
    testTag: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onStep: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp))
            .pointerInput(label) {
                detectTapGestures(
                    onTap = { onStep() },
                    onPress = {
                        val job = scope.launch {
                            delay(350L)
                            while (true) {
                                onStep()
                                delay(150L)
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
            .padding(horizontal = 7.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = StudioTextStyles.CodeSm, color = StudioOnSurface)
    }
}

// Row 10: Voice Mode Switcher
@Composable
private fun VoiceModeSwitcherRow(
    mode: VoiceSourceMode,
    onModeSelected: (VoiceSourceMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val recordedActive = mode == VoiceSourceMode.RECORDED
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (recordedActive) StudioEmerald else Color.Transparent)
                    .clickable { onModeSelected(VoiceSourceMode.RECORDED) }
                    .testTag("row10_recorded_voice_tab"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = if (recordedActive) Color.White else StudioOnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Recorded Voice",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (recordedActive) Color.White else StudioOnSurfaceVariant
                )
            }

            val scriptActive = mode == VoiceSourceMode.SCRIPT
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (scriptActive) StudioSecondaryInteractive else Color.Transparent)
                    .clickable { onModeSelected(VoiceSourceMode.SCRIPT) }
                    .testTag("row10_ai_script_tab"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = if (scriptActive) Color.White else StudioOnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AI Script",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (scriptActive) Color.White else StudioOnSurfaceVariant
                )
            }
        }
    }
}

// Row 11: Voice Action Controls
@Composable
private fun VoiceActionControlsRow(
    agent: AutoAgentState,
    onUploadVoiceClick: () -> Unit,
    onClearVoice: () -> Unit,
    onMicToggle: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onDraftWithAi: () -> Unit,
    onToggleCached: () -> Unit
) {
    var speedExpanded by remember { mutableStateOf(false) }
    val speedOptions = listOf(0.90f, 0.95f, 1.0f, 1.25f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioAlternatingTint)
    ) {
        if (agent.preferredVoiceSource == VoiceSourceMode.RECORDED) {
            // Left: Upload Voice + Remove
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioEmerald)
                        .clickable(onClick = onUploadVoiceClick)
                        .padding(horizontal = 8.dp)
                        .testTag("row11_upload_voice_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.UploadFile,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = agent.recordedVoiceName ?: "Upload Voice",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!agent.recordedVoiceName.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioRose.copy(alpha = 0.12f))
                            .clickable(onClick = onClearVoice)
                            .testTag("row11_clear_voice_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove Voice",
                            tint = StudioRose,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(56.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // Right: Record Mic / Stop (MM:SS)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (agent.isMicRecording) StudioRose else Color.White)
                        .border(
                            1.dp,
                            if (agent.isMicRecording) StudioRose else StudioBorderSubtle,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = onMicToggle)
                        .padding(horizontal = 10.dp)
                        .testTag("row11_record_mic_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (agent.isMicRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = if (agent.isMicRecording) Color.White else StudioRose,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (agent.isMicRecording) {
                            "Stop (${formatMmSs(agent.micRecordingElapsedSec)})"
                        } else {
                            "Record Mic"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (agent.isMicRecording) Color.White else StudioOnSurface
                    )
                }
            }
        } else {
            // AI Script Mode: Left Speed Dropdown, Right AI Draft + Cached Checkbox
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Speed",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioOnSurfaceVariant
                )
                Box {
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { speedExpanded = true }
                            .padding(horizontal = 10.dp)
                            .testTag("row11_script_speed_dropdown"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${agent.mixSettings.speechSpeed}x",
                            style = StudioTextStyles.CodeSm,
                            color = StudioOnSurface
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = StudioOnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(expanded = speedExpanded, onDismissRequest = { speedExpanded = false }) {
                        speedOptions.forEach { sp ->
                            DropdownMenuItem(
                                text = { Text("${sp}x") },
                                onClick = {
                                    speedExpanded = false
                                    onSpeedSelected(sp)
                                }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(54.dp)
                    .background(Color(0xFFE2E8F0))
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFFBEB))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(10.dp))
                        .clickable(enabled = !agent.isDraftingScriptWithAi, onClick = onDraftWithAi)
                        .padding(horizontal = 8.dp)
                        .testTag("row11_ai_draft_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = StudioAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (agent.isDraftingScriptWithAi) "Drafting…" else "AI Draft",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF92400E)
                    )
                }

                Row(
                    modifier = Modifier
                        .clickable(onClick = onToggleCached)
                        .testTag("row11_cached_checkbox"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = !agent.forceRegenerateVoice,
                        onCheckedChange = { onToggleCached() },
                        colors = CheckboxDefaults.colors(checkedColor = StudioSecondaryInteractive),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cached",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioOnSurfaceVariant
                    )
                }
            }
        }
    }
}

// Row 12: Review Script (only in AI Script mode)
@Composable
private fun ReviewScriptRow(
    agent: AutoAgentState,
    onScriptChange: (String) -> Unit
) {
    val isEmpty = agent.scriptText.isBlank()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Review Script",
                style = MaterialTheme.typography.labelLarge,
                color = StudioOnSurface
            )
            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(StudioAmber.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Required",
                        style = StudioTextStyles.CodeSm,
                        color = StudioAmber
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF8FAFC))
                .border(
                    1.dp,
                    if (isEmpty && agent.highlightMissingFields) StudioAmber else StudioBorderSubtle,
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            if (isEmpty) {
                Text(
                    text = "এখানে বাংলা বা ইংরেজি ভয়েসওভার স্ক্রিপ্ট লিখুন অথবা 'AI Draft' চাপুন…",
                    style = StudioTextStyles.DualScriptBody,
                    color = StudioOnSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            BasicTextField(
                value = agent.scriptText,
                onValueChange = onScriptChange,
                textStyle = StudioTextStyles.DualScriptBody.copy(color = StudioOnSurface),
                cursorBrush = SolidColor(StudioPrimaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("row12_review_script_input")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${agent.scriptWordCount} words",
                style = StudioTextStyles.CodeSm,
                color = StudioOnSurfaceVariant
            )
            Text(
                text = "~${agent.estimatedScriptDurationSec}s (${formatMmSs(agent.estimatedScriptDurationSec)})",
                style = StudioTextStyles.CodeSm,
                color = StudioPrimaryContainer
            )
        }
    }
}

@Composable
private fun OnOffEyeToggleButton(
    isOn: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isOn) StudioEmerald else Color.White)
            .border(
                1.dp,
                if (isOn) StudioEmerald else Color(0xFFCBD5E1),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = if (isOn) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
            contentDescription = null,
            tint = if (isOn) Color.White else StudioOnSurfaceVariant,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = if (isOn) "ON" else "OFF",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isOn) Color.White else StudioOnSurface
        )
    }
}

@Composable
private fun OnOffPillButton(
    isOn: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isOn) StudioEmerald else Color.White)
            .border(
                1.dp,
                if (isOn) StudioEmerald else Color(0xFFCBD5E1),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isOn) "ON" else "OFF",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isOn) Color.White else StudioOnSurface
        )
    }
}

private fun formatShortEnDate(isoDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val out = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val parsed = parser.parse(isoDate)
        if (parsed != null) out.format(parsed) else isoDate
    } catch (_: Exception) {
        isoDate
    }
}

private fun queryFileName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    } catch (_: Exception) {
        uri.lastPathSegment
    }
}

private fun queryVideoDurationSec(context: Context, uri: Uri): Int {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        ((ms ?: 330_000L) / 1000L).toInt().coerceAtLeast(10)
    } catch (_: Exception) {
        330
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
}
