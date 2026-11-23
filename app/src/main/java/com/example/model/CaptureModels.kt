package com.example.model

import android.graphics.Bitmap

object CaptureDefaults {
    const val DEFAULT_ZOOM_LEVEL = 1.4f
    const val MIN_ZOOM_LEVEL = 1.0f
    const val MAX_ZOOM_LEVEL = 3.0f
    const val DEFAULT_START_TRIM_SEC = 330
    const val DEFAULT_END_TRIM_SEC = 330
    const val DEFAULT_SCREENSHOT_COUNT = 30
    val COUNT_SUGGESTIONS = listOf(10, 20, 30, 40, 50, 60, 75, 100, 125, 150, 175, 200, 250, 300)
    val CANDIDATE_OFFSETS_SEC = listOf(
        0f,
        2.0f, -2.0f,
        4.5f, -4.5f,
        7.5f, -7.5f,
        11.0f, -11.0f,
        15.0f, -15.0f,
        20.0f, -20.0f,
        26.0f, -26.0f
    )
}

enum class QualityStatus(val label: String) {
    PASSED("passed"),
    RETRIED("retried"),
    REVIEWED("reviewed")
}

enum class OverlayKind {
    CORNER_TEXT,
    YOUTUBE_LOGO,
    CHANNEL_WATERMARK,
    LOWER_THIRD_BANNER,
    CENTER_CREDIT_TEXT
}

data class OverlayRegion(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val kind: OverlayKind
)

data class DetectedSubjectFace(
    val centerX: Float,
    val centerY: Float,
    val eyesDistance: Float,
    val confidence: Float
)

data class FrameEvaluationResult(
    val overallScore: Float,
    val sharpnessScore: Float,
    val hasAdOverlay: Boolean,
    val isBlurry: Boolean,
    val isAcceptable: Boolean,
    val rejectionReason: String?,
    val hasValidSubject: Boolean = false,
    val hasHeavyUnremovableText: Boolean = false,
    val overlayRegions: List<OverlayRegion> = emptyList(),
    val detectedFaceCount: Int = 0,
    val subjectConfidence: Float = 0f
)

data class ProcessedFrameResult(
    val bitmap: Bitmap?,
    val evaluation: FrameEvaluationResult,
    val accepted: Boolean,
    val wasWatermarkCleaned: Boolean,
    val cleanedOverlayCount: Int,
    val rejectionReason: String?
)

data class ScreenshotItem(
    val id: String,
    val filePath: String,
    val timestampSec: Float,
    val formattedTime: String,
    val index: Int,
    val width: Int,
    val height: Int,
    val qualityStatus: QualityStatus,
    val qualityTag: String,
    val hasAdDetected: Boolean,
    val sharpnessScore: Float,
    val wasReplaced: Boolean,
    val replacedReason: String?,
    val originalTimeSec: Float,
    val sceneSignature: List<Float>,
    val wasWatermarkCleaned: Boolean = false
)

data class SmartCaptureState(
    val videoUri: String? = null,
    val videoName: String = "",
    val videoDurationSec: Float = 0f,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val currentTimeSec: Float = 0f,
    val isPreviewPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isFullscreenPreview: Boolean = false,
    val previewVolume: Float = 0.85f,
    val previewMuted: Boolean = false,
    val previewFramePath: String? = null,
    val zoomLevel: Float = CaptureDefaults.DEFAULT_ZOOM_LEVEL,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val startTrimSec: Int = CaptureDefaults.DEFAULT_START_TRIM_SEC,
    val endTrimSec: Int = CaptureDefaults.DEFAULT_END_TRIM_SEC,
    val screenshotCount: Int = CaptureDefaults.DEFAULT_SCREENSHOT_COUNT,
    val customCountInput: String = CaptureDefaults.DEFAULT_SCREENSHOT_COUNT.toString(),
    val enableSmartFrameFilter: Boolean = true,
    val isGenerating: Boolean = false,
    val captureCurrentIndex: Int = 0,
    val captureTotalCount: Int = 0,
    val captureStatusMessage: String = "",
    val screenshots: List<ScreenshotItem> = emptyList()
) {
    val activeDurationSec: Float
        get() {
            if (videoDurationSec <= 0f) return 0f
            val effectiveEnd = (videoDurationSec - endTrimSec.toFloat()).coerceAtLeast(startTrimSec + 0.5f)
            return (effectiveEnd - startTrimSec.toFloat()).coerceAtLeast(0f)
        }

    val startTrimPct: Float
        get() = if (videoDurationSec <= 0f) 0f else (startTrimSec / videoDurationSec).coerceIn(0f, 1f)

    val endTrimPct: Float
        get() = if (videoDurationSec <= 0f) 1f else ((videoDurationSec - endTrimSec) / videoDurationSec).coerceIn(startTrimPct, 1f)

    val jpegQuality: Int
        get() = when {
            screenshotCount > 120 -> 76
            screenshotCount > 60 -> 80
            else -> 92
        }
}
