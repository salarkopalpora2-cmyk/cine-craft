package com.example.model

enum class VoiceSourceMode {
    RECORDED,
    SCRIPT
}

enum class MissingFieldKey(
    val titleBn: String,
    val actionLabelBn: String,
    val isRequired: Boolean
) {
    VIDEO("সোর্স ভিডিও আপলোড করা হয়নি", "যুক্ত করুন", true),
    VOICE_OR_SCRIPT("ভয়েস বা AI স্ক্রিপ্ট খালি আছে", "ভয়েস দিন", true),
    DRAMA("নাটক সিলেক্ট করা হয়নি", "যুক্ত করুন", false),
    EPISODE("পর্ব নম্বর খালি আছে", "যুক্ত করুন", false),
    DATE("রিলিজের তারিখ দেওয়া হয়নি", "যুক্ত করুন", false),
    CHANNEL_NAME("চ্যানেল নাম বন্ধ বা খালি", "যুক্ত করুন", false),
    BGM("ব্যাকগ্রাউন্ড মিউজিক (BGM) নেই", "যুক্ত করুন", false)
}

enum class AgentStageKey(val label: String) {
    VOICE("voice"),
    BGM("bgm"),
    SCREENSHOTS("screenshots"),
    TIMELINE("timeline"),
    EXPORT("export")
}

data class AgentStageProgress(
    val key: AgentStageKey,
    val title: String,
    val percent: Int = 0,
    val detail: String = "Waiting…",
    val isComplete: Boolean = false,
    val isActive: Boolean = false
)

data class ChannelVoiceProfile(
    val slot: Int,
    val name: String,
    val voiceId: String,
    val genderTag: String,
    val scheduleText: String
)

data class DramaSeriesItem(
    val id: String,
    val title: String,
    val defaultEpisode: String
)

data class AgentMixSettings(
    val speechSpeed: Float = 1.0f,
    val speechBgmPercent: Int = 18,
    val breakLoudPercent: Int = 85,
    val breakTimeSec: Int = 5
)

data class ExportedVideoResult(
    val fileName: String,
    val durationSec: Int,
    val sizeMb: Float,
    val resolution: String,
    val fps: Int
)

data class AutoAgentState(
    // Preview & Pipeline
    val isPreviewPlaying: Boolean = true,
    val previewPositionSec: Float = 0f,
    val previewVolume: Float = 0.85f,
    val previewMuted: Boolean = false,
    val isAgentRunning: Boolean = false,
    val currentStageTitle: String = "",
    val overallProgressPercent: Int = 0,
    val stages: List<AgentStageProgress> = AutoAgentDefaults.initialStages,
    val lastExportedVideo: ExportedVideoResult? = null,
    // Row 1: Release Date
    val releaseDate: String = "2026-11-23",
    val highlightMissingFields: Boolean = false,
    // Row 2: Drama + Title ON/OFF
    val activeSeriesList: List<DramaSeriesItem> = AutoAgentDefaults.seriesList,
    val selectedSeriesId: String = AutoAgentDefaults.seriesList.first().id,
    val showLowerThird: Boolean = true,
    // Row 3: Episode Stepper (supports English & Bengali numerals)
    val activeEpisode: String = "পর্ব ০১",
    // Row 4: Channel + Channel Name ON/OFF
    val channelProfiles: List<ChannelVoiceProfile> = AutoAgentDefaults.channelProfiles,
    val selectedChannelSlot: Int = 1,
    val showChannelName: Boolean = true,
    // Row 5: Video Additions
    val showSubtitles: Boolean = true,
    val showEffects: Boolean = true,
    val showCta: Boolean = true,
    // Row 6: Source Video Upload
    val uploadedVideoName: String? = null,
    val uploadedVideoUri: String? = null,
    val uploadedVideoDurationSec: Int = 330,
    // Row 7: Zoom Crop Level (1.0 .. 2.5, default 1.4 = 140%)
    val zoomLevel: Float = 1.4f,
    // Row 8: Start & End Trim (seconds)
    val startTrimSec: Int = 0,
    val endTrimSec: Int = 330,
    // Row 9: Final Duration Override (null = Auto Est.)
    val customFinalDurationSec: Int? = null,
    val isExactLock: Boolean = false,
    // Row 10: Voice Mode
    val preferredVoiceSource: VoiceSourceMode = VoiceSourceMode.SCRIPT,
    // Row 11: Voice Actions (Recorded vs Script)
    val recordedVoiceName: String? = null,
    val recordedVoiceUri: String? = null,
    val isMicRecording: Boolean = false,
    val micRecordingElapsedSec: Int = 0,
    val forceRegenerateVoice: Boolean = false,
    // Row 12: Review Script
    val scriptText: String = "আজকের পর্বে রহস্যের জাল আরও ঘনীভূত হয়। গভীর রাতে শহরের নির্জন রাস্তায় উঠে আসে এক অজানা সত্য।",
    val isDraftingScriptWithAi: Boolean = false,
    // Row 13: BGM & Mix
    val bgmName: String? = null,
    val bgmUri: String? = null,
    val mixSettings: AgentMixSettings = AgentMixSettings(),
    // Row 14: Final Master Audio Player
    val isMixingAudio: Boolean = false,
    val isMasterAudioReady: Boolean = true,
    val isMasterAudioPlaying: Boolean = false,
    val masterAudioPositionSec: Int = 0,
    val masterAudioTotalSec: Int = 24,
    val masterAudioMuted: Boolean = false,
    val masterAudioVolume: Float = 0.85f,
    val masterAudioSpeed: Float = 1.0f,
    // Row 15: Export & Speed Strip
    val performanceMode: String = "Auto (Smart)",
    val exportRatio: String = "720p",
    val exportFps: Int = 24,
    // Row 16: Watermark + Intro + Settings
    val showWatermark: Boolean = true,
    val showIntroPanel: Boolean = false,
    val attachedIntroDurationSec: Int? = null,
    val showChannelSettingsModal: Boolean = false,
    // Pre-Flight Missing Modal
    val showPreFlightModal: Boolean = false,
    val missingItemsList: List<MissingFieldKey> = emptyList(),
    val focusedFieldKey: MissingFieldKey? = null
) {
    val activeSeries: DramaSeriesItem?
        get() = activeSeriesList.firstOrNull { it.id == selectedSeriesId }

    val activeChannel: ChannelVoiceProfile
        get() = channelProfiles.firstOrNull { it.slot == selectedChannelSlot } ?: channelProfiles.first()

    val trimmedDurationSec: Int
        get() = (endTrimSec - startTrimSec).coerceAtLeast(10)

    val scriptWordCount: Int
        get() = scriptText.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    val estimatedScriptDurationSec: Int
        get() = ((scriptWordCount / (2.2f * mixSettings.speechSpeed)).toInt()).coerceAtLeast(if (scriptWordCount > 0) 6 else 0)

    val effectiveFinalDurationSec: Int
        get() = customFinalDurationSec ?: if (preferredVoiceSource == VoiceSourceMode.SCRIPT) {
            estimatedScriptDurationSec.coerceAtLeast(15)
        } else {
            trimmedDurationSec.coerceAtMost(180)
        }

    val estimatedShotsCount: Int
        get() = (effectiveFinalDurationSec / 5).coerceIn(3, 60)

    val durationModeLabel: String
        get() = when {
            customFinalDurationSec == null -> "Auto"
            customFinalDurationSec < trimmedDurationSec -> "Condense"
            customFinalDurationSec > trimmedDurationSec -> "Expand"
            else -> "Custom"
        }

    val durationLockLabel: String
        get() = when {
            customFinalDurationSec == null -> "Auto Est."
            isExactLock -> "Exact Lock"
            else -> "Exact Mixed"
        }
}

object AutoAgentDefaults {
    val seriesList = listOf(
        DramaSeriesItem("d1", "মায়া জাল (Maya Jaal)", "পর্ব ০১"),
        DramaSeriesItem("d2", "নীল আকাশের নিচে (Under Blue Sky)", "পর্ব ১২"),
        DramaSeriesItem("d3", "শেষ প্রহরের গল্প (Last Hour Story)", "পর্ব ০৭"),
        DramaSeriesItem("d4", "অচেনা শহর (Stranger City)", "পর্ব ২৪")
    )

    val channelProfiles = listOf(
        ChannelVoiceProfile(1, "CineDrama BD", "v_aria_f", "F", "প্রতিদিন রাত ৮:০০ টায়"),
        ChannelVoiceProfile(2, "Golpo Kotha", "v_orion_m", "M", "রাত ৯:৩০ টায়"),
        ChannelVoiceProfile(3, "Bangla Natok HD", "v_meera_f", "F", "সন্ধ্যা ৭:০০ টায়"),
        ChannelVoiceProfile(4, "Rupkotha Studio", "v_kael_m", "M", "প্রতি শুক্র ও শনি"),
        ChannelVoiceProfile(5, "Kotha Mala", "v_aria_f", "F", "রাত ১০:০০ টায়"),
        ChannelVoiceProfile(6, "Dhaka Reel", "v_orion_m", "M", "বিকাল ৫:০০ টায়"),
        ChannelVoiceProfile(7, "Priyo Golpo", "v_meera_f", "F", "রাত ৮:৩০ টায়"),
        ChannelVoiceProfile(8, "Swapno Chaya", "v_kael_m", "M", "রাত ৯:০০ টায়"),
        ChannelVoiceProfile(9, "Nodi Teere", "v_aria_f", "F", "সকাল ১১:০০ টায়"),
        ChannelVoiceProfile(10, "CineCraft Prime", "v_orion_m", "M", "প্রতিদিন প্রাইম টাইম")
    )

    val initialStages = listOf(
        AgentStageProgress(AgentStageKey.VOICE, "1. Voice Synthesis / Track"),
        AgentStageProgress(AgentStageKey.BGM, "2. BGM Auto-Ducking & Mix"),
        AgentStageProgress(AgentStageKey.SCREENSHOTS, "3. Smart Scene Extraction"),
        AgentStageProgress(AgentStageKey.TIMELINE, "4. Timeline Assembly & Subtitles"),
        AgentStageProgress(AgentStageKey.EXPORT, "5. Final MP4 Render & Export")
    )
}
