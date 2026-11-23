package com.example.model

import androidx.annotation.DrawableRes
import com.example.R

enum class StudioTab(val label: String) {
    Agent("Agent"),
    Voice("Voice"),
    Capture("Capture"),
    Video("Video"),
    Gallery("Gallery"),
    Tailor("Tailor")
}

enum class AspectRatioPreset(
    val label: String,
    val ratio: Float,
    val resolutionLabel: String
) {
    WIDE_16_9("16:9", 16f / 9f, "1920x1080"),
    VERTICAL_9_16("9:16", 9f / 16f, "1080x1920"),
    SQUARE_1_1("1:1", 1f, "1080x1080")
}

enum class AssetStatus(val label: String) {
    EXPORTED("Exported"),
    DRAFT("Draft"),
    SCHEDULED("Scheduled")
}

enum class AssetKind(val label: String) {
    VIDEO("Video"),
    STILL("Frame"),
    VOICEOVER("Voice")
}

sealed interface AiUiState<out T> {
    data object Idle : AiUiState<Nothing>
    data object Loading : AiUiState<Nothing>
    data class Streaming(val partialText: String) : AiUiState<Nothing>
    data class Success<T>(val data: T) : AiUiState<T>
    data class Error(val message: String) : AiUiState<Nothing>
}

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampLabel: String,
    val isStreaming: Boolean = false
)

data class DirectorShotPlan(
    val title: String,
    val logline: String,
    val shots: List<ShotBreakdown>
)

data class ShotBreakdown(
    val shotCode: String,
    val framing: String,
    val durationSec: Int,
    val visualPrompt: String
)

data class VoicePersona(
    val id: String,
    val name: String,
    val tone: String,
    val languageTag: String,
    val pitch: Float,
    val speed: Float
)

data class ScriptLine(
    val id: String,
    val speaker: String,
    val timecode: String,
    val text: String,
    val secondaryText: String? = null,
    val durationSec: Float
)

data class TimelineClip(
    val id: String,
    val title: String,
    val trackName: String,
    val durationSec: Float,
    val trimStartFraction: Float = 0f,
    val trimEndFraction: Float = 1f,
    val motionPreset: String,
    @DrawableRes val thumbnailRes: Int,
    val isAiGenerated: Boolean = false
)

data class MediaAsset(
    val id: String,
    val title: String,
    val kind: AssetKind,
    val status: AssetStatus,
    val durationOrSize: String,
    val aspectLabel: String,
    val createdAt: String,
    @DrawableRes val drawableRes: Int? = null,
    val localUri: String? = null
)

data class CanvasOptions(
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.WIDE_16_9,
    val showGridLines: Boolean = true,
    val showSafeZones: Boolean = true,
    val lutPreset: String = "CineBlue LUT"
)

data class ExportProfile(
    val resolution: String = "4K UHD",
    val frameRate: String = "24 fps",
    val codec: String = "H.265 HEVC",
    val bitrate: String = "45 Mbps",
    val targetPlatform: String = "YouTube Cinema"
)

data class TailorCopyPack(
    val headline: String,
    val caption: String,
    val hashtags: List<String>
)

data class InspectorReport(
    val healthScore: Int,
    val summary: String,
    val recommendations: List<String>
)

data class StudioSnapshot(
    val scriptLines: List<ScriptLine>,
    val timelineClips: List<TimelineClip>,
    val canvasOptions: CanvasOptions,
    val exportProfile: ExportProfile,
    val actionDescription: String
)

object StudioDefaults {
    val personas = listOf(
        VoicePersona("v1", "Aria Studio", "Warm Documentary", "en-US", 1.0f, 1.0f),
        VoicePersona("v2", "Orion Deep", "Trailer Narrator", "en-US", 0.82f, 0.92f),
        VoicePersona("v3", "Meera Dual", "Bilingual Host", "en-IN", 1.08f, 1.02f),
        VoicePersona("v4", "Kael Crisp", "Tech Explainer", "en-GB", 1.02f, 1.12f)
    )

    val initialScriptLines = listOf(
        ScriptLine(
            id = "s1",
            speaker = "NARRATOR",
            timecode = "00:00:00:00",
            text = "Beneath the cobalt rain of Neo-Shinjuku, every reflection holds a signal waiting to be framed.",
            secondaryText = "নিও-শিনজুকুর নীল বৃষ্টির নিচে প্রতিটি প্রতিফলনে লুকিয়ে আছে একটি গল্প।",
            durationSec = 4.5f
        ),
        ScriptLine(
            id = "s2",
            speaker = "DIRECTOR",
            timecode = "00:00:04:12",
            text = "Push in 24mm anamorphic. Hold on the neon puddle ripple before cutting to the aerial coastline.",
            durationSec = 5.0f
        ),
        ScriptLine(
            id = "s3",
            speaker = "NARRATOR",
            timecode = "00:00:09:12",
            text = "From electric alleyways to untouched sea cliffs, CineCraft stitches light, voice, and tempo in real time.",
            durationSec = 5.5f
        )
    )

    val initialClips = listOf(
        TimelineClip(
            id = "c1",
            title = "Tokyo Rain Alley",
            trackName = "V1",
            durationSec = 4.5f,
            motionPreset = "Dolly Push 1.2x",
            thumbnailRes = R.drawable.img_scene_neon_tokyo,
            isAiGenerated = true
        ),
        TimelineClip(
            id = "c2",
            title = "Pacific Cliff Sweep",
            trackName = "V1",
            durationSec = 5.0f,
            motionPreset = "Aerial Orbit",
            thumbnailRes = R.drawable.img_scene_coastal_drone,
            isAiGenerated = true
        ),
        TimelineClip(
            id = "c3",
            title = "Keylight Monologue",
            trackName = "V1",
            durationSec = 5.5f,
            motionPreset = "Static 50mm",
            thumbnailRes = R.drawable.img_scene_studio_portrait,
            isAiGenerated = false
        )
    )

    val initialAssets: List<MediaAsset> = emptyList()
}
