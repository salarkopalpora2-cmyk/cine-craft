package com.example.ui.components

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.StudioPrimaryContainer
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioTextStyles
import com.example.viewmodel.formatMmSs
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PLAYER_TAG = "CaptureVideoPlayer"

@Stable
class CaptureVideoPlayerController(
    private val context: Context,
    private val onVideoCompleted: () -> Unit
) {
    private var mediaPlayer: MediaPlayer? = null
    private var activeUri: String? = null
    private var activeSurfaceTexture: SurfaceTexture? = null
    private var activeSurface: Surface? = null

    var isPrepared by mutableStateOf(false)
        private set
    var hasFirstFrame by mutableStateOf(false)
        private set
    var videoAspectRatio by mutableStateOf(16f / 9f)
        private set

    private var desiredPlaying: Boolean = false
    private var desiredVolume: Float = 0.85f
    private var desiredSpeed: Float = 1.0f
    private var pendingSeekMs: Int? = null

    fun loadUriIfNeeded(uriString: String?, initialSeekSec: Float) {
        if (uriString.isNullOrBlank()) {
            release()
            return
        }
        if (uriString == activeUri && mediaPlayer != null) return

        releasePlayerOnly()
        activeUri = uriString
        isPrepared = false
        hasFirstFrame = false
        pendingSeekMs = (initialSeekSec * 1000f).roundToInt().coerceAtLeast(0)

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                setDataSource(context, Uri.parse(uriString))
                activeSurface?.let { setSurface(it) }
                setOnVideoSizeChangedListener { _, w, h ->
                    if (w > 0 && h > 0) {
                        videoAspectRatio = w.toFloat() / h.toFloat()
                    }
                }
                setOnPreparedListener { player ->
                    isPrepared = true
                    player.setVolume(desiredVolume, desiredVolume)
                    pendingSeekMs?.let { ms ->
                        seekInternal(player, ms)
                        pendingSeekMs = null
                    }
                    if (desiredPlaying) {
                        applySpeedIfPlaying(player, forceApply = true)
                        player.start()
                    }
                }
                setOnSeekCompleteListener {
                    hasFirstFrame = true
                }
                setOnInfoListener { _, what, _ ->
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        hasFirstFrame = true
                    }
                    false
                }
                setOnCompletionListener {
                    desiredPlaying = false
                    onVideoCompleted()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(PLAYER_TAG, "MediaPlayer error what=$what extra=$extra")
                    isPrepared = false
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            Log.e(PLAYER_TAG, "Failed initializing MediaPlayer for $uriString", e)
            releasePlayerOnly()
        }
    }

    fun attachSurface(surfaceTexture: SurfaceTexture) {
        if (activeSurfaceTexture === surfaceTexture && activeSurface?.isValid == true) return
        activeSurfaceTexture = surfaceTexture
        activeSurface?.release()
        val surface = Surface(surfaceTexture)
        activeSurface = surface
        try {
            mediaPlayer?.setSurface(surface)
        } catch (e: Exception) {
            Log.w(PLAYER_TAG, "setSurface error: ${e.message}")
        }
    }

    fun detachSurfaceIfSame(surfaceTexture: SurfaceTexture) {
        if (activeSurfaceTexture === surfaceTexture) {
            activeSurfaceTexture = null
            try {
                mediaPlayer?.setSurface(null)
            } catch (_: Exception) {
            }
            activeSurface?.release()
            activeSurface = null
        }
    }

    fun syncPlayState(shouldPlay: Boolean) {
        desiredPlaying = shouldPlay
        val mp = mediaPlayer ?: return
        if (!isPrepared) return
        try {
            if (shouldPlay && !mp.isPlaying) {
                mp.setVolume(desiredVolume, desiredVolume)
                mp.start()
                applySpeedIfPlaying(mp, forceApply = false)
            } else if (!shouldPlay && mp.isPlaying) {
                mp.pause()
            }
        } catch (e: Exception) {
            Log.w(PLAYER_TAG, "syncPlayState error: ${e.message}")
        }
    }

    fun syncVolume(volume: Float, isMuted: Boolean) {
        desiredVolume = if (isMuted) 0f else volume.coerceIn(0f, 1f)
        val mp = mediaPlayer ?: return
        if (!isPrepared) return
        try {
            mp.setVolume(desiredVolume, desiredVolume)
        } catch (_: Exception) {
        }
    }

    fun syncSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 2.0f)
        if (abs(desiredSpeed - safeSpeed) < 0.01f) return
        desiredSpeed = safeSpeed
        val mp = mediaPlayer ?: return
        if (!isPrepared) return
        applySpeedIfPlaying(mp, forceApply = false)
    }

    private fun applySpeedIfPlaying(mp: MediaPlayer, forceApply: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (forceApply || mp.isPlaying)) {
            try {
                mp.playbackParams = mp.playbackParams.setSpeed(desiredSpeed)
            } catch (e: Exception) {
                Log.w(PLAYER_TAG, "PlaybackParams speed error: ${e.message}")
            }
        }
    }

    fun seekToSec(sec: Float) {
        val targetMs = (sec * 1000f).roundToInt().coerceAtLeast(0)
        val mp = mediaPlayer
        if (mp == null || !isPrepared) {
            pendingSeekMs = targetMs
            return
        }
        seekInternal(mp, targetMs)
    }

    private fun seekInternal(mp: MediaPlayer, targetMs: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mp.seekTo(targetMs.toLong(), MediaPlayer.SEEK_CLOSEST)
            } else {
                mp.seekTo(targetMs)
            }
        } catch (_: Exception) {
        }
    }

    fun currentPositionSec(): Float? {
        val mp = mediaPlayer ?: return null
        if (!isPrepared) return null
        return try {
            mp.currentPosition.coerceAtLeast(0) / 1000f
        } catch (_: Exception) {
            null
        }
    }

    fun isActuallyPlaying(): Boolean {
        val mp = mediaPlayer ?: return false
        return isPrepared && try {
            mp.isPlaying
        } catch (_: Exception) {
            false
        }
    }

    private fun releasePlayerOnly() {
        isPrepared = false
        hasFirstFrame = false
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }

    fun release() {
        releasePlayerOnly()
        activeUri = null
        activeSurfaceTexture = null
        activeSurface?.release()
        activeSurface = null
    }
}

@Composable
fun CaptureVideoTextureSurface(
    controller: CaptureVideoPlayerController,
    previewFramePath: String?,
    currentTimeSec: Float,
    zoomLevel: Float,
    panX: Float,
    panY: Float,
    enablePan: Boolean,
    onUpdatePan: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .then(
                if (enablePan && zoomLevel > 1.0f) {
                    Modifier.pointerInput(zoomLevel) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dxPct = (-dragAmount.x / size.width.coerceAtLeast(1)) * 100f
                            val dyPct = (-dragAmount.y / size.height.coerceAtLeast(1)) * 100f
                            onUpdatePan(dxPct, dyPct)
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        // Fallback crisp frame shown until TextureView draws its live decoded frame
        val frameFile = previewFramePath?.let { File(it) }
        if (frameFile != null && frameFile.exists()) {
            AsyncImage(
                model = frameFile,
                contentDescription = "Video frame preview at ${formatMmSs(currentTimeSec.toInt())}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomLevel
                        scaleY = zoomLevel
                        translationX = -(panX / 100f) * size.width * (zoomLevel - 1f) * 0.5f
                        translationY = -(panY / 100f) * size.height * (zoomLevel - 1f) * 0.5f
                    }
            )
        } else if (!controller.hasFirstFrame) {
            Icon(
                imageVector = Icons.Rounded.VideoLibrary,
                contentDescription = "Preparing video preview",
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
            )
        }

        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    isOpaque = false
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            controller.attachSurface(surface)
                            applyTextureAspectTransform(this@apply, width, height, controller.videoAspectRatio)
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            applyTextureAspectTransform(this@apply, width, height, controller.videoAspectRatio)
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            controller.detachSurfaceIfSame(surface)
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                        }
                    }
                }
            },
            update = { textureView ->
                if (textureView.isAvailable && textureView.width > 0 && textureView.height > 0) {
                    textureView.surfaceTexture?.let { controller.attachSurface(it) }
                    applyTextureAspectTransform(
                        textureView,
                        textureView.width,
                        textureView.height,
                        controller.videoAspectRatio
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomLevel
                    scaleY = zoomLevel
                    translationX = -(panX / 100f) * size.width * (zoomLevel - 1f) * 0.5f
                    translationY = -(panY / 100f) * size.height * (zoomLevel - 1f) * 0.5f
                }
        )
    }
}

private fun applyTextureAspectTransform(
    textureView: TextureView,
    viewWidth: Int,
    viewHeight: Int,
    videoAspect: Float
) {
    if (viewWidth <= 0 || viewHeight <= 0 || videoAspect <= 0f) return
    val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()
    val scaleX: Float
    val scaleY: Float
    if (videoAspect > viewAspect) {
        scaleX = 1f
        scaleY = viewAspect / videoAspect
    } else {
        scaleX = videoAspect / viewAspect
        scaleY = 1f
    }
    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}

@Composable
fun FullscreenCaptureVideoDialog(
    controller: CaptureVideoPlayerController,
    previewFramePath: String?,
    isPlaying: Boolean,
    currentTimeSec: Float,
    totalDurationSec: Float,
    volume: Float,
    isMuted: Boolean,
    playbackSpeed: Float,
    zoomLevel: Float,
    panX: Float,
    panY: Float,
    onTogglePlay: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onCycleSpeed: () -> Unit,
    onUpdatePan: (Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    val safeTotal = totalDurationSec.coerceAtLeast(1f)
    val safeTime = currentTimeSec.coerceIn(0f, safeTotal)
    val effectiveVol = if (isMuted) 0f else volume.coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CaptureVideoTextureSurface(
                controller = controller,
                previewFramePath = previewFramePath,
                currentTimeSec = currentTimeSec,
                zoomLevel = zoomLevel,
                panX = panX,
                panY = panY,
                enablePan = true,
                onUpdatePan = onUpdatePan,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onTogglePlay)
            )

            // Top bar with Zoom badge & Exit Fullscreen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Zoom ${(zoomLevel * 100f).roundToInt()}%",
                    style = StudioTextStyles.CodeSm,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(onClick = onDismiss)
                        .testTag("capture_fullscreen_exit_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FullscreenExit,
                        contentDescription = "Exit fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Bottom Fullscreen Player Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xCC0B1221))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(formatMmSs(safeTime.toInt()), style = StudioTextStyles.CodeSm, color = Color.White)
                    Slider(
                        value = safeTime,
                        onValueChange = onSeek,
                        onValueChangeFinished = { onSeekFinished(safeTime) },
                        valueRange = 0f..safeTotal,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = StudioPrimaryContainer,
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                    )
                    Text(formatMmSs(safeTotal.toInt()), style = StudioTextStyles.CodeSm, color = Color(0xFF94A3B8))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioPrimaryContainer)
                                .clickable(onClick = onTogglePlay),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .clickable(onClick = onRewind),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.FastRewind, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .clickable(onClick = onForward),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.FastForward, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .clickable(onClick = onCycleSpeed)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatPlaybackSpeed(playbackSpeed),
                                style = StudioTextStyles.CodeSm.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = Color(0xFF93C5FD)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onToggleMute),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMuted || volume <= 0.01f) {
                                    Icons.AutoMirrored.Rounded.VolumeOff
                                } else {
                                    Icons.AutoMirrored.Rounded.VolumeUp
                                },
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = if (isMuted) StudioRose else Color.White,
                                modifier = Modifier.size(18.dp)
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
                            modifier = Modifier
                                .width(84.dp)
                                .height(24.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatPlaybackSpeed(speed: Float): String {
    val clean = (speed * 100f).roundToInt() / 100f
    return if (clean == clean.toInt().toFloat()) {
        "${clean.toInt()}x"
    } else {
        "${clean}x"
    }
}
