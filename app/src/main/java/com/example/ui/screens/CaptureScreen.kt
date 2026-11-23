package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.model.SmartCaptureState
import com.example.ui.components.CaptureVideoPlayerController
import com.example.ui.components.CaptureVideoTextureSurface
import com.example.ui.components.FullscreenCaptureVideoDialog
import com.example.ui.components.SharpCinemaPreviewPlayer
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioPrimaryContainer
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioTextStyles
import com.example.viewmodel.formatMmSs
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

@Composable
fun CaptureScreen(
    state: SmartCaptureState,
    onLoadVideoFile: (uriString: String, fileName: String) -> Unit,
    onClearVideo: () -> Unit,
    onSeekPreview: (Float) -> Unit,
    onRewindPreview: () -> Unit,
    onForwardPreview: () -> Unit,
    onTogglePreviewPlay: () -> Unit,
    onSyncPlayerTime: (Float) -> Unit,
    onPausePreview: () -> Unit,
    onPreviewVolumeChanged: (Float) -> Unit,
    onTogglePreviewMute: () -> Unit,
    onCyclePlaybackSpeed: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onUpdateZoomLevel: (Float) -> Unit,
    onUpdatePan: (deltaXPercent: Float, deltaYPercent: Float) -> Unit,
    onUpdateStartTrim: (Int) -> Unit,
    onUpdateEndTrim: (Int) -> Unit,
    onStepCount: (Int) -> Unit,
    onToggleSmartFrameFilter: () -> Unit,
    onStartGenerating: () -> Unit,
    onStopGenerating: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnPause by rememberUpdatedState(onPausePreview)
    val latestOnSyncTime by rememberUpdatedState(onSyncPlayerTime)
    var isScrubbing by remember { mutableStateOf(false) }

    val playerController = remember(context) {
        CaptureVideoPlayerController(
            context = context,
            onVideoCompleted = { latestOnPause() }
        )
    }

    DisposableEffect(lifecycleOwner, playerController) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                playerController.syncPlayState(false)
                latestOnPause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerController.release()
        }
    }

    LaunchedEffect(state.videoUri) {
        playerController.loadUriIfNeeded(state.videoUri, state.currentTimeSec)
    }

    LaunchedEffect(state.isPreviewPlaying, state.isGenerating) {
        playerController.syncPlayState(state.isPreviewPlaying && !state.isGenerating)
    }

    LaunchedEffect(state.previewVolume, state.previewMuted) {
        playerController.syncVolume(state.previewVolume, state.previewMuted)
    }

    LaunchedEffect(state.playbackSpeed) {
        playerController.syncSpeed(state.playbackSpeed)
    }

    LaunchedEffect(state.isPreviewPlaying, state.isGenerating, isScrubbing, state.videoUri) {
        if (state.videoUri != null && state.isPreviewPlaying && !state.isGenerating && !isScrubbing) {
            while (true) {
                playerController.currentPositionSec()?.let { posSec ->
                    latestOnSyncTime(posSec)
                }
                delay(150L)
            }
        }
    }

    val handleSeekChanged: (Float) -> Unit = { targetSec ->
        isScrubbing = true
        onSeekPreview(targetSec)
    }
    val handleSeekFinished: () -> Unit = {
        playerController.seekToSec(state.currentTimeSec)
        isScrubbing = false
    }
    val handleRewind: () -> Unit = {
        val target = (state.currentTimeSec - 10f).coerceAtLeast(0f)
        onRewindPreview()
        playerController.seekToSec(target)
    }
    val handleForward: () -> Unit = {
        val maxSec = state.videoDurationSec.coerceAtLeast(1f)
        val target = (state.currentTimeSec + 10f).coerceAtMost(maxSec)
        onForwardPreview()
        playerController.seekToSec(target)
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = resolveVideoDisplayName(context, uri)
            onLoadVideoFile(uri.toString(), fileName)
        }
    }

    val launchVideoPicker = {
        videoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoColumn = maxWidth >= 840.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isTwoColumn) Modifier else Modifier.widthIn(max = 640.dp)),
            contentPadding = if (isTwoColumn) {
                PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp)
            } else {
                PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 12.dp)
            },
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (isTwoColumn) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        SmartCapturePreviewDisplay(
                            state = state,
                            playerController = playerController,
                            renderLiveSurface = !state.isFullscreenPreview,
                            onUploadClick = launchVideoPicker,
                            onTogglePlay = onTogglePreviewPlay,
                            onRewind = handleRewind,
                            onForward = handleForward,
                            onSeek = handleSeekChanged,
                            onSeekFinished = handleSeekFinished,
                            onVolumeChange = onPreviewVolumeChanged,
                            onToggleMute = onTogglePreviewMute,
                            onCycleSpeed = onCyclePlaybackSpeed,
                            onToggleFullscreen = onToggleFullscreen,
                            onUpdatePan = onUpdatePan,
                            onStopGenerating = onStopGenerating,
                            modifier = Modifier.weight(7f)
                        )

                        CaptureStudioControlTable(
                            state = state,
                            hasVideo = state.videoUri != null,
                            onClearVideo = onClearVideo,
                            onToggleSmartFrameFilter = onToggleSmartFrameFilter,
                            onPickVideoClick = launchVideoPicker,
                            onStepCount = onStepCount,
                            onUpdateStartTrim = onUpdateStartTrim,
                            onUpdateEndTrim = onUpdateEndTrim,
                            onUpdateZoomLevel = onUpdateZoomLevel,
                            onStartGenerating = onStartGenerating,
                            onStopGenerating = onStopGenerating,
                            modifier = Modifier.weight(5f)
                        )
                    }
                }
            } else {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape,
                        color = StudioCardSurface,
                        border = BorderStroke(1.dp, StudioBorderSubtle)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SmartCapturePreviewDisplay(
                                state = state,
                                playerController = playerController,
                                renderLiveSurface = !state.isFullscreenPreview,
                                onUploadClick = launchVideoPicker,
                                onTogglePlay = onTogglePreviewPlay,
                                onRewind = handleRewind,
                                onForward = handleForward,
                                onSeek = handleSeekChanged,
                                onSeekFinished = handleSeekFinished,
                                onVolumeChange = onPreviewVolumeChanged,
                                onToggleMute = onTogglePreviewMute,
                                onCycleSpeed = onCyclePlaybackSpeed,
                                onToggleFullscreen = onToggleFullscreen,
                                onUpdatePan = onUpdatePan,
                                onStopGenerating = onStopGenerating
                            )

                            HorizontalDivider(color = StudioBorderSubtle.copy(alpha = 0.7f))

                            CaptureStudioControlTable(
                                state = state,
                                hasVideo = state.videoUri != null,
                                onClearVideo = onClearVideo,
                                onToggleSmartFrameFilter = onToggleSmartFrameFilter,
                                onPickVideoClick = launchVideoPicker,
                                onStepCount = onStepCount,
                                onUpdateStartTrim = onUpdateStartTrim,
                                onUpdateEndTrim = onUpdateEndTrim,
                                onUpdateZoomLevel = onUpdateZoomLevel,
                                onStartGenerating = onStartGenerating,
                                onStopGenerating = onStopGenerating
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.isFullscreenPreview && state.videoUri != null) {
        FullscreenCaptureVideoDialog(
            controller = playerController,
            previewFramePath = state.previewFramePath,
            isPlaying = state.isPreviewPlaying,
            currentTimeSec = state.currentTimeSec,
            totalDurationSec = state.videoDurationSec.coerceAtLeast(1f),
            volume = state.previewVolume,
            isMuted = state.previewMuted,
            playbackSpeed = state.playbackSpeed,
            zoomLevel = state.zoomLevel,
            panX = state.panX,
            panY = state.panY,
            onTogglePlay = onTogglePreviewPlay,
            onRewind = handleRewind,
            onForward = handleForward,
            onSeek = handleSeekChanged,
            onSeekFinished = { target ->
                playerController.seekToSec(target)
                isScrubbing = false
            },
            onVolumeChange = onPreviewVolumeChanged,
            onToggleMute = onTogglePreviewMute,
            onCycleSpeed = onCyclePlaybackSpeed,
            onUpdatePan = onUpdatePan,
            onDismiss = onToggleFullscreen
        )
    }
}

@Composable
private fun SmartCapturePreviewDisplay(
    state: SmartCaptureState,
    playerController: CaptureVideoPlayerController,
    renderLiveSurface: Boolean,
    onUploadClick: () -> Unit,
    onTogglePlay: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onUpdatePan: (Float, Float) -> Unit,
    onStopGenerating: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zoomPercent = (state.zoomLevel * 100f).roundToInt()
    val hasVideo = state.videoUri != null

    SharpCinemaPreviewPlayer(
        isPlaying = state.isPreviewPlaying,
        currentTimeSec = state.currentTimeSec,
        totalDurationSec = state.videoDurationSec.coerceAtLeast(1f),
        volume = state.previewVolume,
        isMuted = state.previewMuted,
        controlsEnabled = hasVideo,
        onTogglePlay = onTogglePlay,
        onSeek = onSeek,
        onSeekFinished = onSeekFinished,
        onRewind = onRewind,
        onForward = onForward,
        playbackSpeed = state.playbackSpeed,
        onCycleSpeed = onCycleSpeed,
        onToggleFullscreen = onToggleFullscreen,
        isFullscreen = state.isFullscreenPreview,
        onVolumeChange = onVolumeChange,
        onToggleMute = onToggleMute,
        trimStartPct = if (hasVideo) state.startTrimPct else null,
        trimEndPct = if (hasVideo) state.endTrimPct else null,
        activeStatusLabel = if (state.isGenerating) {
            "CAP ${state.captureCurrentIndex}/${state.captureTotalCount}"
        } else {
            null
        },
        onCancelStatusAction = if (state.isGenerating) onStopGenerating else null,
        extraBadgeText = null,
        previewTestTag = "capture_preview_box",
        modifier = modifier
    ) {
        if (hasVideo) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (renderLiveSurface) {
                    CaptureVideoTextureSurface(
                        controller = playerController,
                        previewFramePath = state.previewFramePath,
                        currentTimeSec = state.currentTimeSec,
                        zoomLevel = state.zoomLevel,
                        panX = state.panX,
                        panY = state.panY,
                        enablePan = true,
                        onUpdatePan = onUpdatePan,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val frameFile = state.previewFramePath?.let { File(it) }
                    if (frameFile != null && frameFile.exists()) {
                        AsyncImage(
                            model = frameFile,
                            contentDescription = "Video frame preview at ${formatMmSs(state.currentTimeSec.toInt())}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = state.zoomLevel
                                    scaleY = state.zoomLevel
                                    translationX = -(state.panX / 100f) * size.width * (state.zoomLevel - 1f) * 0.5f
                                    translationY = -(state.panY / 100f) * size.height * (state.zoomLevel - 1f) * 0.5f
                                }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.68f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Zoom $zoomPercent%",
                        style = StudioTextStyles.CodeSm,
                        color = Color.White
                    )
                }

                if (state.isGenerating) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(StudioRose.copy(alpha = 0.92f))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text(
                            text = "CAPTURING ${state.captureCurrentIndex}/${state.captureTotalCount}",
                            style = StudioTextStyles.CodeSm.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.VideoLibrary,
                    contentDescription = "No video selected",
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "No Video Selected",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
                Button(
                    onClick = onUploadClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioPrimaryContainer,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("capture_empty_upload_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Video", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun resolveVideoDisplayName(context: Context, uri: Uri): String {
    var name: String? = null
    try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = cursor.getString(idx)
                }
            }
    } catch (_: Exception) {
    }
    return name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Selected_Video.mp4"
}
