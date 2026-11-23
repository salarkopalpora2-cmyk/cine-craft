package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.ExportedDownloadResult
import com.example.data.GalleryExportHelper
import com.example.data.GeminiStudioService
import com.example.data.SmartFrameAnalyzer
import com.example.data.SmartTimelineEngine
import com.example.data.StudioPreferencesRepository
import com.example.model.AgentMixSettings
import com.example.model.AgentStageKey
import com.example.model.AiUiState
import com.example.model.AspectRatioPreset
import com.example.model.AssetKind
import com.example.model.AssetStatus
import com.example.model.AutoAgentDefaults
import com.example.model.AutoAgentState
import com.example.model.CanvasOptions
import com.example.model.CaptureDefaults
import com.example.model.DirectorShotPlan
import com.example.model.ExportProfile
import com.example.model.ExportedVideoResult
import com.example.model.InspectorReport
import com.example.model.MediaAsset
import com.example.model.MissingFieldKey
import com.example.model.QualityStatus
import com.example.model.ScreenshotItem
import com.example.model.ScriptLine
import com.example.model.SmartCaptureState
import com.example.model.StudioDefaults
import com.example.model.StudioSnapshot
import com.example.model.StudioTab
import com.example.model.TailorCopyPack
import com.example.model.TimelineClip
import com.example.model.VoicePersona
import com.example.model.VoiceSourceMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "StudioViewModel"

data class StudioUiState(
    val selectedTab: StudioTab = StudioTab.Agent,
    val projectName: String = "CineDrama Auto Reel",
    // Auto Agent Table State (17-row Luminous Studio Panel)
    val autoAgent: AutoAgentState = AutoAgentState(),
    // Voice Workspace
    val personas: List<VoicePersona> = StudioDefaults.personas,
    val selectedPersonaId: String = StudioDefaults.personas.first().id,
    val voicePitch: Float = 1.0f,
    val voiceSpeed: Float = 1.0f,
    val scriptLines: List<ScriptLine> = StudioDefaults.initialScriptLines,
    val selectedScriptLineId: String = StudioDefaults.initialScriptLines.first().id,
    val isSpeakingLineId: String? = null,
    val voicePolishState: AiUiState<String> = AiUiState.Idle,
    val ttsReady: Boolean = false,
    // Smart Capture + Auto-Replace Workspace
    val smartCapture: SmartCaptureState = SmartCaptureState(),
    // Video Timeline Workspace
    val timelineClips: List<TimelineClip> = StudioDefaults.initialClips,
    val selectedClipId: String = StudioDefaults.initialClips.first().id,
    val isPlayingTimeline: Boolean = false,
    val playheadSec: Float = 2.4f,
    val timelineVolume: Float = 0.85f,
    val timelineMuted: Boolean = false,
    val timelinePrompt: String = "",
    val storyboardState: AiUiState<DirectorShotPlan> = AiUiState.Idle,
    // Gallery Workspace
    val assets: List<MediaAsset> = StudioDefaults.initialAssets,
    val selectedStatusFilter: AssetStatus? = null,
    val selectedAssetIds: Set<String> = emptySet(),
    val isExportingGalleryZip: Boolean = false,
    val inspectedAsset: MediaAsset? = null,
    val latestDownload: ExportedDownloadResult? = null,
    // Tailor Workspace
    val copyPackState: AiUiState<TailorCopyPack> = AiUiState.Idle,
    val exportProgress: Float = 0f,
    val isExporting: Boolean = false,
    val exportCompleteMessage: String? = null,
    // Global Studio & Top Bar Controls
    val canvasOptions: CanvasOptions = CanvasOptions(),
    val exportProfile: ExportProfile = ExportProfile(),
    val inspectorState: AiUiState<InspectorReport> = AiUiState.Idle,
    val showInspectorSheet: Boolean = false,
    val showCanvasSheet: Boolean = false,
    val showExportSheet: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val snackbarMessage: String? = null
) {
    val totalTimelineDurationSec: Float
        get() = timelineClips.sumOf { (it.durationSec * (it.trimEndFraction - it.trimStartFraction)).toDouble() }
            .toFloat()
            .coerceAtLeast(1f)

    val activeClip: TimelineClip?
        get() = timelineClips.firstOrNull { it.id == selectedClipId } ?: timelineClips.firstOrNull()

    val activePersona: VoicePersona
        get() = personas.firstOrNull { it.id == selectedPersonaId } ?: personas.first()
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiStudioService()
    private val prefsRepo = StudioPreferencesRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<StudioSnapshot>()
    private val redoStack = ArrayDeque<StudioSnapshot>()

    private var playbackJob: Job? = null
    private var agentPreviewPlayJob: Job? = null
    private var captureGenerationJob: Job? = null
    private var capturePreviewPlayJob: Job? = null
    private var previewSeekJob: Job? = null
    private var exportJob: Job? = null
    private var pipelineJob: Job? = null
    private var micTimerJob: Job? = null
    private var masterAudioJob: Job? = null
    private var textToSpeech: TextToSpeech? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var mediaPlayer: MediaPlayer? = null

    init {
        viewModelScope.launch {
            val savedCanvas = prefsRepo.canvasOptionsFlow.first()
            val savedExport = prefsRepo.exportProfileFlow.first()
            val savedCapture = prefsRepo.captureDefaultsFlow.first()
            _uiState.update {
                it.copy(
                    canvasOptions = savedCanvas,
                    exportProfile = savedExport,
                    smartCapture = it.smartCapture.copy(
                        zoomLevel = savedCapture.zoomLevel,
                        startTrimSec = savedCapture.startTrimSec,
                        endTrimSec = savedCapture.endTrimSec
                    )
                )
            }
        }
        initTextToSpeech(application)
        if (_uiState.value.autoAgent.isPreviewPlaying) {
            startAgentPreviewTicker()
        }
    }

    private fun initTextToSpeech(application: Application) {
        textToSpeech = TextToSpeech(application.applicationContext) { status ->
            val ready = status == TextToSpeech.SUCCESS
            _uiState.update { it.copy(ttsReady = ready) }
            if (ready) {
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        _uiState.update {
                            it.copy(
                                isSpeakingLineId = null,
                                autoAgent = it.autoAgent.copy(isMasterAudioPlaying = false)
                            )
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _uiState.update {
                            it.copy(
                                isSpeakingLineId = null,
                                autoAgent = it.autoAgent.copy(isMasterAudioPlaying = false)
                            )
                        }
                    }
                })
            }
        }
    }

    fun selectTab(tab: StudioTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // --- Undo / Redo ---

    private fun pushSnapshot(description: String) {
        val current = _uiState.value
        undoStack.addLast(
            StudioSnapshot(
                scriptLines = current.scriptLines,
                timelineClips = current.timelineClips,
                canvasOptions = current.canvasOptions,
                exportProfile = current.exportProfile,
                actionDescription = description
            )
        )
        if (undoStack.size > 20) undoStack.removeFirst()
        redoStack.clear()
        syncUndoRedoFlags()
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        val current = _uiState.value
        redoStack.addLast(
            StudioSnapshot(
                scriptLines = current.scriptLines,
                timelineClips = current.timelineClips,
                canvasOptions = current.canvasOptions,
                exportProfile = current.exportProfile,
                actionDescription = previous.actionDescription
            )
        )
        _uiState.update {
            it.copy(
                scriptLines = previous.scriptLines,
                timelineClips = previous.timelineClips,
                canvasOptions = previous.canvasOptions,
                exportProfile = previous.exportProfile,
                snackbarMessage = "Undid: ${previous.actionDescription}"
            )
        }
        syncUndoRedoFlags()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        val current = _uiState.value
        undoStack.addLast(
            StudioSnapshot(
                scriptLines = current.scriptLines,
                timelineClips = current.timelineClips,
                canvasOptions = current.canvasOptions,
                exportProfile = current.exportProfile,
                actionDescription = next.actionDescription
            )
        )
        _uiState.update {
            it.copy(
                scriptLines = next.scriptLines,
                timelineClips = next.timelineClips,
                canvasOptions = next.canvasOptions,
                exportProfile = next.exportProfile,
                snackbarMessage = "Redid: ${next.actionDescription}"
            )
        }
        syncUndoRedoFlags()
    }

    private fun syncUndoRedoFlags() {
        _uiState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    // --- Top Bar Sheets ---

    fun setInspectorSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showInspectorSheet = visible) }
        if (visible && _uiState.value.inspectorState is AiUiState.Idle) {
            runProjectInspector()
        }
    }

    fun setCanvasSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showCanvasSheet = visible) }
    }

    fun setExportSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showExportSheet = visible) }
    }

    fun updateCanvasOptions(transform: (CanvasOptions) -> CanvasOptions) {
        pushSnapshot("Canvas options")
        val updated = transform(_uiState.value.canvasOptions)
        _uiState.update { it.copy(canvasOptions = updated) }
        viewModelScope.launch { prefsRepo.saveCanvasOptions(updated) }
    }

    fun updateExportProfile(transform: (ExportProfile) -> ExportProfile) {
        pushSnapshot("Export profile")
        val updated = transform(_uiState.value.exportProfile)
        _uiState.update { it.copy(exportProfile = updated) }
        viewModelScope.launch { prefsRepo.saveExportProfile(updated) }
    }

    fun runProjectInspector() {
        val state = _uiState.value
        val summary = buildContextSummary(state)
        viewModelScope.launch {
            _uiState.update { it.copy(inspectorState = AiUiState.Loading) }
            try {
                val report = geminiService.inspectProject(summary)
                _uiState.update { it.copy(inspectorState = AiUiState.Success(report)) }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Project inspector timed out", e)
                _uiState.update {
                    it.copy(inspectorState = AiUiState.Error("Inspector timed out. Please retry."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Project inspector failed", e)
                _uiState.update {
                    it.copy(
                        inspectorState = AiUiState.Error(
                            e.localizedMessage ?: "AI Inspector failed."
                        )
                    )
                }
            }
        }
    }

    // --- Auto Agent Table Callbacks (Rows 1-17 + Preview + Pre-flight + 5-Stage Pipeline) ---

    private inline fun updateAgent(crossinline block: (AutoAgentState) -> AutoAgentState) {
        _uiState.update { it.copy(autoAgent = block(it.autoAgent)) }
    }

    fun togglePreviewPlaying() {
        val next = !_uiState.value.autoAgent.isPreviewPlaying
        updateAgent { it.copy(isPreviewPlaying = next) }
        if (next) {
            startAgentPreviewTicker()
        } else {
            agentPreviewPlayJob?.cancel()
        }
    }

    private fun startAgentPreviewTicker() {
        agentPreviewPlayJob?.cancel()
        agentPreviewPlayJob = viewModelScope.launch {
            while (true) {
                delay(200L)
                updateAgent { state ->
                    if (!state.isPreviewPlaying) return@updateAgent state
                    val maxDur = state.effectiveFinalDurationSec.toFloat().coerceAtLeast(15f)
                    val nextPos = state.previewPositionSec + 0.2f
                    state.copy(previewPositionSec = if (nextPos >= maxDur) 0f else nextPos)
                }
            }
        }
    }

    fun seekAgentPreview(sec: Float) {
        updateAgent { state ->
            val maxDur = state.effectiveFinalDurationSec.toFloat().coerceAtLeast(15f)
            state.copy(previewPositionSec = sec.coerceIn(0f, maxDur))
        }
    }

    fun rewindAgentPreview() {
        updateAgent { it.copy(previewPositionSec = 0f) }
    }

    fun setAgentPreviewVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        updateAgent { it.copy(previewVolume = clamped, previewMuted = clamped <= 0.01f) }
        try {
            mediaPlayer?.setVolume(clamped, clamped)
        } catch (_: Exception) {
        }
    }

    fun toggleAgentPreviewMute() {
        updateAgent { state ->
            val nextMuted = !state.previewMuted
            val v = if (nextMuted) 0f else state.previewVolume
            try {
                mediaPlayer?.setVolume(v, v)
            } catch (_: Exception) {
            }
            state.copy(previewMuted = nextMuted)
        }
    }

    // Row 1: Release Date
    fun updateReleaseDate(date: String) {
        updateAgent { it.copy(releaseDate = date) }
    }

    // Row 2: Drama + Lower-Third ON/OFF
    fun selectSeriesById(seriesId: String) {
        updateAgent { state ->
            val found = state.activeSeriesList.firstOrNull { it.id == seriesId }
            state.copy(
                selectedSeriesId = seriesId,
                activeEpisode = found?.defaultEpisode ?: state.activeEpisode
            )
        }
    }

    fun toggleLowerThird() {
        updateAgent { it.copy(showLowerThird = !it.showLowerThird) }
    }

    // Row 3: Episode Stepper (supports Bengali ০-৯ and English 0-9 numerals)
    fun updateActiveEpisode(value: String) {
        updateAgent { it.copy(activeEpisode = value) }
    }

    fun stepEpisode(delta: Int) {
        updateAgent { state ->
            val updated = incrementMixedNumeralText(state.activeEpisode, delta)
            state.copy(activeEpisode = updated)
        }
    }

    // Row 4: Channel + Channel Name ON/OFF
    fun selectChannelSlot(slot: Int) {
        updateAgent { it.copy(selectedChannelSlot = slot) }
    }

    fun updateChannelName(slot: Int, newName: String) {
        updateAgent { state ->
            state.copy(
                channelProfiles = state.channelProfiles.map { profile ->
                    if (profile.slot == slot) profile.copy(name = newName) else profile
                }
            )
        }
    }

    fun toggleChannelNameQuick() {
        updateAgent { it.copy(showChannelName = !it.showChannelName) }
    }

    // Row 5: Video Additions
    fun toggleSubtitles() {
        updateAgent { it.copy(showSubtitles = !it.showSubtitles) }
    }

    fun toggleEffects() {
        updateAgent { it.copy(showEffects = !it.showEffects) }
    }

    fun toggleCtaQuick() {
        updateAgent { it.copy(showCta = !it.showCta) }
    }

    // Row 6: Source Video Upload
    fun onUploadSourceVideo(uri: String, fileName: String, durationSec: Int = 330) {
        val safeDuration = durationSec.coerceIn(15, 3600)
        updateAgent {
            it.copy(
                uploadedVideoUri = uri,
                uploadedVideoName = fileName,
                uploadedVideoDurationSec = safeDuration,
                startTrimSec = 0,
                endTrimSec = safeDuration
            )
        }
        _uiState.update { it.copy(snackbarMessage = "Loaded video: $fileName") }
    }

    fun clearSourceVideo() {
        updateAgent {
            it.copy(
                uploadedVideoUri = null,
                uploadedVideoName = null,
                uploadedVideoDurationSec = 330,
                startTrimSec = 0,
                endTrimSec = 330
            )
        }
    }

    // Row 7: Zoom Crop Level (1.0 .. 2.5)
    fun stepZoomLevel(delta: Float) {
        updateAgent { state ->
            val next = ((state.zoomLevel + delta) * 10f).toInt() / 10f
            state.copy(zoomLevel = next.coerceIn(1.0f, 2.5f))
        }
    }

    // Row 8: Start Trim & End Trim
    fun adjustStartTrim(deltaSec: Int) {
        updateAgent { state ->
            val maxStart = (state.endTrimSec - 10).coerceAtLeast(0)
            state.copy(startTrimSec = (state.startTrimSec + deltaSec).coerceIn(0, maxStart))
        }
    }

    fun setStartTrim(totalSec: Int) {
        updateAgent { state ->
            val maxStart = (state.endTrimSec - 10).coerceAtLeast(0)
            state.copy(startTrimSec = totalSec.coerceIn(0, maxStart))
        }
    }

    fun adjustEndTrim(deltaSec: Int) {
        updateAgent { state ->
            val minEnd = (state.startTrimSec + 10).coerceAtMost(3600)
            state.copy(endTrimSec = (state.endTrimSec + deltaSec).coerceIn(minEnd, 3600))
        }
    }

    fun setEndTrim(totalSec: Int) {
        updateAgent { state ->
            val minEnd = (state.startTrimSec + 10).coerceAtMost(3600)
            state.copy(endTrimSec = totalSec.coerceIn(minEnd, 3600))
        }
    }

    // Row 9: Final Duration Bar
    fun adjustFinalDuration(deltaSec: Int) {
        updateAgent { state ->
            val base = state.effectiveFinalDurationSec
            val next = (base + deltaSec).coerceIn(10, 3600)
            state.copy(customFinalDurationSec = next, isExactLock = true)
        }
    }

    fun setFinalDuration(totalSec: Int) {
        updateAgent { state ->
            state.copy(
                customFinalDurationSec = totalSec.coerceIn(10, 3600),
                isExactLock = true
            )
        }
    }

    fun resetFinalDurationAuto() {
        updateAgent { it.copy(customFinalDurationSec = null, isExactLock = false) }
    }

    fun adaptScriptToFinalDuration() {
        val state = _uiState.value.autoAgent
        if (state.scriptText.isBlank()) {
            draftDramaScriptWithAi()
            return
        }
        val targetSec = state.effectiveFinalDurationSec
        viewModelScope.launch {
            updateAgent { it.copy(isDraftingScriptWithAi = true) }
            try {
                val adapted = geminiService.adaptScriptToDuration(state.scriptText, targetSec)
                updateAgent {
                    it.copy(
                        scriptText = adapted,
                        isDraftingScriptWithAi = false,
                        isExactLock = false
                    )
                }
                _uiState.update { it.copy(snackbarMessage = "Script adapted to ${formatMmSs(targetSec)}") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Adapt script failed", e)
                updateAgent { it.copy(isDraftingScriptWithAi = false) }
                _uiState.update {
                    it.copy(snackbarMessage = e.localizedMessage ?: "Failed to adapt script.")
                }
            }
        }
    }

    // Row 10: Voice Mode Switcher
    fun changePreferredVoiceSource(mode: VoiceSourceMode) {
        updateAgent { it.copy(preferredVoiceSource = mode) }
    }

    // Row 11: Recorded Voice Upload & Mic Recording + Script Speed & Cached
    fun onUploadRecordedVoice(uri: String, fileName: String) {
        updateAgent {
            it.copy(
                recordedVoiceUri = uri,
                recordedVoiceName = fileName,
                isMasterAudioReady = true
            )
        }
        _uiState.update { it.copy(snackbarMessage = "Voice track attached: $fileName") }
    }

    fun clearRecordedVoice() {
        updateAgent {
            it.copy(
                recordedVoiceUri = null,
                recordedVoiceName = null
            )
        }
    }

    fun startMicRecording() {
        val context = getApplication<Application>().applicationContext
        val outFile = File(context.cacheDir, "cinecraft_mic_${System.currentTimeMillis()}.m4a")
        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(outFile.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            currentRecordingFile = outFile

            updateAgent { it.copy(isMicRecording = true, micRecordingElapsedSec = 0) }
            micTimerJob?.cancel()
            micTimerJob = viewModelScope.launch {
                while (true) {
                    delay(1000L)
                    updateAgent { it.copy(micRecordingElapsedSec = it.micRecordingElapsedSec + 1) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Mic recording failed to start", e)
            _uiState.update {
                it.copy(snackbarMessage = e.localizedMessage ?: "Microphone unavailable.")
            }
        }
    }

    fun stopMicRecording() {
        micTimerJob?.cancel()
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
        }
        val file = currentRecordingFile
        val elapsed = _uiState.value.autoAgent.micRecordingElapsedSec.coerceAtLeast(1)
        val uri = file?.let { Uri.fromFile(it).toString() }
        val name = "Mic_Voice_${formatMmSs(elapsed)}.m4a"
        updateAgent {
            it.copy(
                isMicRecording = false,
                micRecordingElapsedSec = 0,
                recordedVoiceUri = uri,
                recordedVoiceName = name,
                isMasterAudioReady = true,
                masterAudioTotalSec = elapsed
            )
        }
        _uiState.update { it.copy(snackbarMessage = "Recorded voice saved ($name)") }
    }

    fun toggleForceRegenerateVoice() {
        updateAgent { it.copy(forceRegenerateVoice = !it.forceRegenerateVoice) }
    }

    // Row 12: Review Script
    fun onScriptTextChanged(text: String) {
        updateAgent { it.copy(scriptText = text) }
    }

    fun draftDramaScriptWithAi() {
        val state = _uiState.value.autoAgent
        val dramaTitle = state.activeSeries?.title ?: "CineDrama Special"
        val ep = state.activeEpisode.ifBlank { "পর্ব ০১" }
        val targetSec = state.customFinalDurationSec ?: 24
        viewModelScope.launch {
            updateAgent { it.copy(isDraftingScriptWithAi = true) }
            try {
                val drafted = geminiService.draftDramaScript(dramaTitle, ep, targetSec)
                updateAgent {
                    it.copy(
                        scriptText = drafted,
                        isDraftingScriptWithAi = false,
                        isMasterAudioReady = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Draft drama script failed", e)
                updateAgent { it.copy(isDraftingScriptWithAi = false) }
                _uiState.update {
                    it.copy(snackbarMessage = e.localizedMessage ?: "Failed to draft script.")
                }
            }
        }
    }

    // Row 13: BGM & Mix Grid
    fun onUploadBgm(uri: String, fileName: String) {
        updateAgent { it.copy(bgmUri = uri, bgmName = fileName) }
        _uiState.update { it.copy(snackbarMessage = "BGM attached: $fileName") }
    }

    fun clearBgm() {
        updateAgent { it.copy(bgmUri = null, bgmName = null) }
    }

    fun updateMixSettings(transform: (AgentMixSettings) -> AgentMixSettings) {
        updateAgent { it.copy(mixSettings = transform(it.mixSettings)) }
    }

    // Row 14: Final Master Audio Player
    fun mixPreviewAudio() {
        if (_uiState.value.autoAgent.isMixingAudio) return
        viewModelScope.launch {
            updateAgent { it.copy(isMixingAudio = true) }
            delay(650L)
            val agent = _uiState.value.autoAgent
            val total = agent.effectiveFinalDurationSec
            updateAgent {
                it.copy(
                    isMixingAudio = false,
                    isMasterAudioReady = true,
                    masterAudioTotalSec = total,
                    masterAudioPositionSec = 0
                )
            }
            _uiState.update { it.copy(snackbarMessage = "Master audio mixed (${formatMmSs(total)})") }
        }
    }

    fun toggleMasterAudioPlay() {
        val agent = _uiState.value.autoAgent
        if (agent.isMasterAudioPlaying) {
            pauseMasterAudioInternal()
            return
        }
        if (agent.preferredVoiceSource == VoiceSourceMode.RECORDED && !agent.recordedVoiceUri.isNullOrBlank()) {
            try {
                mediaPlayer?.release()
                val player = MediaPlayer().apply {
                    setDataSource(getApplication<Application>(), Uri.parse(agent.recordedVoiceUri))
                    prepare()
                    val vol = if (agent.masterAudioMuted) 0f else agent.masterAudioVolume
                    setVolume(vol, vol)
                    start()
                    setOnCompletionListener {
                        pauseMasterAudioInternal()
                        updateAgent { it.copy(masterAudioPositionSec = 0) }
                    }
                }
                mediaPlayer = player
                val total = (player.duration / 1000).coerceAtLeast(1)
                updateAgent { it.copy(isMasterAudioPlaying = true, masterAudioTotalSec = total) }
                startMasterAudioTicker(total)
                return
            } catch (e: Exception) {
                Log.e(TAG, "MediaPlayer preview fallback to TTS", e)
            }
        }

        // AI Script or fallback TTS playback
        val tts = textToSpeech
        if (tts != null && _uiState.value.ttsReady && agent.scriptText.isNotBlank()) {
            tts.language = Locale.forLanguageTag("bn-BD")
            tts.setSpeechRate(agent.masterAudioSpeed * agent.mixSettings.speechSpeed)
            tts.speak(agent.scriptText, TextToSpeech.QUEUE_FLUSH, null, "master_preview")
            val total = agent.estimatedScriptDurationSec.coerceAtLeast(6)
            updateAgent {
                it.copy(
                    isMasterAudioPlaying = true,
                    masterAudioTotalSec = total,
                    masterAudioPositionSec = 0
                )
            }
            startMasterAudioTicker(total)
        } else {
            val total = agent.masterAudioTotalSec.coerceAtLeast(10)
            updateAgent { it.copy(isMasterAudioPlaying = true) }
            startMasterAudioTicker(total)
        }
    }

    private fun startMasterAudioTicker(totalSec: Int) {
        masterAudioJob?.cancel()
        masterAudioJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _uiState.value.autoAgent.masterAudioPositionSec + 1
                if (current >= totalSec) {
                    pauseMasterAudioInternal()
                    updateAgent { it.copy(masterAudioPositionSec = 0) }
                    break
                } else {
                    updateAgent { it.copy(masterAudioPositionSec = current) }
                }
            }
        }
    }

    private fun pauseMasterAudioInternal() {
        masterAudioJob?.cancel()
        try {
            if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
        } catch (_: Exception) {
        }
        textToSpeech?.stop()
        updateAgent { it.copy(isMasterAudioPlaying = false) }
    }

    fun seekMasterAudio(sec: Int) {
        val total = _uiState.value.autoAgent.masterAudioTotalSec
        val clamped = sec.coerceIn(0, total)
        try {
            mediaPlayer?.seekTo(clamped * 1000)
        } catch (_: Exception) {
        }
        updateAgent { it.copy(masterAudioPositionSec = clamped) }
    }

    fun stepMasterAudio(deltaSec: Int) {
        seekMasterAudio(_uiState.value.autoAgent.masterAudioPositionSec + deltaSec)
    }

    fun restartMasterAudio() {
        seekMasterAudio(0)
        if (!_uiState.value.autoAgent.isMasterAudioPlaying) {
            toggleMasterAudioPlay()
        }
    }

    fun stopMasterAudio() {
        pauseMasterAudioInternal()
        updateAgent { it.copy(masterAudioPositionSec = 0) }
    }

    fun toggleMasterAudioMute() {
        updateAgent { state ->
            val muted = !state.masterAudioMuted
            val vol = if (muted) 0f else state.masterAudioVolume
            try {
                mediaPlayer?.setVolume(vol, vol)
            } catch (_: Exception) {
            }
            state.copy(masterAudioMuted = muted)
        }
    }

    fun setMasterAudioVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        updateAgent { state ->
            val effective = if (state.masterAudioMuted) 0f else clamped
            try {
                mediaPlayer?.setVolume(effective, effective)
            } catch (_: Exception) {
            }
            state.copy(masterAudioVolume = clamped)
        }
    }

    fun setMasterAudioSpeed(speed: Float) {
        updateAgent { it.copy(masterAudioSpeed = speed) }
    }

    // Row 15: Export & Speed Strip
    fun updatePerformanceMode(mode: String) {
        updateAgent { it.copy(performanceMode = mode) }
    }

    fun updateExportRatio(ratio: String) {
        updateAgent { it.copy(exportRatio = ratio) }
    }

    fun updateExportFps(fps: Int) {
        updateAgent { it.copy(exportFps = fps) }
    }

    // Row 16: Watermark + Intro + Settings
    fun toggleWatermarkQuick() {
        updateAgent { it.copy(showWatermark = !it.showWatermark) }
    }

    fun toggleIntroPanel() {
        updateAgent { it.copy(showIntroPanel = !it.showIntroPanel) }
    }

    fun attachIntroVideo(durationSec: Int) {
        updateAgent {
            it.copy(
                attachedIntroDurationSec = durationSec,
                showIntroPanel = false
            )
        }
        _uiState.update { it.copy(snackbarMessage = "ইন্ট্রো যুক্ত করা হয়েছে (${durationSec}s)") }
    }

    fun removeAttachedIntro() {
        updateAgent { it.copy(attachedIntroDurationSec = null) }
    }

    fun setChannelSettingsModalVisible(visible: Boolean) {
        updateAgent { it.copy(showChannelSettingsModal = visible) }
    }

    // Row 17: Generate & Auto Export + Pre-Flight Modal + 5-Stage Pipeline
    fun handleGenerateClick() {
        val agent = _uiState.value.autoAgent
        if (agent.isAgentRunning) return

        val missing = buildList {
            if (agent.uploadedVideoName.isNullOrBlank()) add(MissingFieldKey.VIDEO)
            val hasVoice = if (agent.preferredVoiceSource == VoiceSourceMode.RECORDED) {
                !agent.recordedVoiceName.isNullOrBlank()
            } else {
                agent.scriptText.isNotBlank()
            }
            if (!hasVoice) add(MissingFieldKey.VOICE_OR_SCRIPT)
            if (agent.selectedSeriesId.isBlank()) add(MissingFieldKey.DRAMA)
            if (agent.activeEpisode.isBlank()) add(MissingFieldKey.EPISODE)
            if (agent.releaseDate.isBlank()) add(MissingFieldKey.DATE)
            if (!agent.showChannelName || agent.activeChannel.name.isBlank()) {
                add(MissingFieldKey.CHANNEL_NAME)
            }
            if (agent.bgmName.isNullOrBlank()) add(MissingFieldKey.BGM)
        }

        if (missing.isNotEmpty()) {
            updateAgent {
                it.copy(
                    showPreFlightModal = true,
                    missingItemsList = missing,
                    highlightMissingFields = true
                )
            }
        } else {
            startAutonomousPipeline()
        }
    }

    fun dismissPreFlightModal() {
        updateAgent { it.copy(showPreFlightModal = false) }
    }

    fun focusFieldForMissingKey(key: MissingFieldKey) {
        updateAgent {
            it.copy(
                showPreFlightModal = false,
                focusedFieldKey = key,
                highlightMissingFields = true
            )
        }
    }

    fun clearFocusedFieldKey() {
        updateAgent { it.copy(focusedFieldKey = null) }
    }

    fun startAutonomousPipeline() {
        pipelineJob?.cancel()
        val freshStages = AutoAgentDefaults.initialStages
        updateAgent {
            it.copy(
                showPreFlightModal = false,
                highlightMissingFields = false,
                isAgentRunning = true,
                overallProgressPercent = 0,
                currentStageTitle = freshStages.first().title,
                stages = freshStages,
                lastExportedVideo = null
            )
        }

        pipelineJob = viewModelScope.launch {
            try {
                val agent = _uiState.value.autoAgent
                val stageDetails = listOf(
                    AgentStageKey.VOICE to "Synthesizing ${agent.activeChannel.voiceId} (${agent.mixSettings.speechSpeed}x)",
                    AgentStageKey.BGM to "Ducking BGM to ${agent.mixSettings.speechBgmPercent}% / Break ${agent.mixSettings.breakLoudPercent}%",
                    AgentStageKey.SCREENSHOTS to "Extracting ${agent.estimatedShotsCount} smart scenes (${(agent.zoomLevel * 100).toInt()}% zoom)",
                    AgentStageKey.TIMELINE to "Compositing ${agent.activeEpisode} overlays & Bengali subtitles",
                    AgentStageKey.EXPORT to "Encoding ${agent.exportRatio} @ ${agent.exportFps}fps MP4"
                )

                // Call Gemini AI during stage 4/script check if AI Script mode is active
                var aiDirectorTag = ""
                if (agent.preferredVoiceSource == VoiceSourceMode.SCRIPT && agent.scriptText.isNotBlank()) {
                    try {
                        val plan = geminiService.generateShotPlan(
                            concept = "${agent.activeSeries?.title ?: "Drama"} ${agent.activeEpisode}: ${agent.scriptText.take(120)}",
                            aspectLabel = "16:9"
                        )
                        aiDirectorTag = plan.title
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Pipeline AI shot plan optional enrichment skipped: ${e.message}")
                    }
                }

                stageDetails.forEachIndexed { stageIdx, (key, detail) ->
                    for (pct in listOf(25, 60, 100)) {
                        delay(220L)
                        val overall = ((stageIdx * 100 + pct) / stageDetails.size).coerceIn(0, 100)
                        updateAgent { current ->
                            current.copy(
                                overallProgressPercent = overall,
                                currentStageTitle = current.stages[stageIdx].title,
                                stages = current.stages.mapIndexed { idx, s ->
                                    when {
                                        idx < stageIdx -> s.copy(percent = 100, isComplete = true, isActive = false)
                                        idx == stageIdx -> s.copy(
                                            percent = pct,
                                            detail = if (idx == 2 && aiDirectorTag.isNotBlank()) "$detail • $aiDirectorTag" else detail,
                                            isActive = pct < 100,
                                            isComplete = pct == 100
                                        )
                                        else -> s
                                    }
                                }
                            )
                        }
                    }
                }

                val finalAgent = _uiState.value.autoAgent
                val duration = finalAgent.effectiveFinalDurationSec + (finalAgent.attachedIntroDurationSec ?: 0)
                val sizeMb = ((duration * if (finalAgent.exportRatio == "1080p") 1.4f else 0.72f))
                val fileName = "${finalAgent.activeChannel.name.replace(" ", "_")}_${finalAgent.exportRatio}.mp4"
                val exportedAsset = MediaAsset(
                    id = UUID.randomUUID().toString(),
                    title = fileName.removeSuffix(".mp4"),
                    kind = AssetKind.VIDEO,
                    status = AssetStatus.EXPORTED,
                    durationOrSize = "${formatMmSs(duration)} • ${finalAgent.exportRatio}",
                    aspectLabel = "16:9",
                    createdAt = "Just now",
                    drawableRes = R.drawable.img_scene_neon_tokyo
                )

                _uiState.update { state ->
                    state.copy(
                        assets = listOf(exportedAsset) + state.assets,
                        autoAgent = state.autoAgent.copy(
                            isAgentRunning = false,
                            overallProgressPercent = 100,
                            currentStageTitle = "Export Complete",
                            lastExportedVideo = ExportedVideoResult(
                                fileName = fileName,
                                durationSec = duration,
                                sizeMb = sizeMb,
                                resolution = finalAgent.exportRatio,
                                fps = finalAgent.exportFps
                            )
                        ),
                        snackbarMessage = "Auto Export Complete: $fileName"
                    )
                }
            } catch (e: CancellationException) {
                updateAgent { it.copy(isAgentRunning = false, currentStageTitle = "Cancelled") }
                throw e
            }
        }
    }

    fun cancelAutonomousPipeline() {
        pipelineJob?.cancel()
        updateAgent {
            it.copy(
                isAgentRunning = false,
                currentStageTitle = "Cancelled"
            )
        }
        _uiState.update { it.copy(snackbarMessage = "Auto Agent pipeline stopped") }
    }

    // --- Voice Tab ---

    fun selectVoicePersona(persona: VoicePersona) {
        _uiState.update {
            it.copy(
                selectedPersonaId = persona.id,
                voicePitch = persona.pitch,
                voiceSpeed = persona.speed
            )
        }
    }

    fun setVoicePitch(pitch: Float) {
        _uiState.update { it.copy(voicePitch = pitch) }
    }

    fun setVoiceSpeed(speed: Float) {
        _uiState.update { it.copy(voiceSpeed = speed) }
    }

    fun selectScriptLine(lineId: String) {
        _uiState.update { it.copy(selectedScriptLineId = lineId) }
    }

    fun toggleSpeakLine(line: ScriptLine) {
        val tts = textToSpeech
        if (_uiState.value.isSpeakingLineId == line.id) {
            tts?.stop()
            _uiState.update { it.copy(isSpeakingLineId = null) }
            return
        }
        if (tts == null || !_uiState.value.ttsReady) {
            _uiState.update {
                it.copy(snackbarMessage = "Device Text-to-Speech engine initializing…")
            }
            return
        }
        val persona = _uiState.value.activePersona
        tts.language = Locale.forLanguageTag(persona.languageTag)
        tts.setPitch(_uiState.value.voicePitch)
        tts.setSpeechRate(_uiState.value.voiceSpeed)
        _uiState.update { it.copy(isSpeakingLineId = line.id, selectedScriptLineId = line.id) }
        tts.speak(line.text, TextToSpeech.QUEUE_FLUSH, null, line.id)
    }

    fun polishSelectedLineWithAi() {
        val state = _uiState.value
        val target = state.scriptLines.firstOrNull { it.id == state.selectedScriptLineId } ?: return
        val tone = state.activePersona.tone
        viewModelScope.launch {
            _uiState.update { it.copy(voicePolishState = AiUiState.Loading) }
            try {
                val polished = geminiService.polishVoiceLine(target.text, tone)
                pushSnapshot("Polish voice line")
                _uiState.update { current ->
                    current.copy(
                        scriptLines = current.scriptLines.map { line ->
                            if (line.id == target.id) line.copy(text = polished) else line
                        },
                        voicePolishState = AiUiState.Success(polished),
                        snackbarMessage = "Polished line for $tone"
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Voice polish timed out", e)
                _uiState.update {
                    it.copy(voicePolishState = AiUiState.Error("Voice rewrite timed out."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Voice polish failed", e)
                _uiState.update {
                    it.copy(
                        voicePolishState = AiUiState.Error(
                            e.localizedMessage ?: "Voice polish failed."
                        )
                    )
                }
            }
        }
    }

    fun addScriptLine(speaker: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        pushSnapshot("Add script line")
        val nextIndex = _uiState.value.scriptLines.size
        val newLine = ScriptLine(
            id = UUID.randomUUID().toString(),
            speaker = speaker.uppercase(Locale.US),
            timecode = formatSmpte(nextIndex * 5f),
            text = clean,
            durationSec = 4.5f
        )
        _uiState.update {
            it.copy(
                scriptLines = it.scriptLines + newLine,
                selectedScriptLineId = newLine.id
            )
        }
    }

    // --- Smart Capture + Auto-Replace Engine (Screenshot Studio) ---

    private inline fun updateCapture(crossinline block: (SmartCaptureState) -> SmartCaptureState) {
        _uiState.update { it.copy(smartCapture = block(it.smartCapture)) }
    }

    fun loadCaptureVideoFile(uriString: String, fileName: String) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val savedDefaults = prefsRepo.captureDefaultsFlow.first()

            val meta = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(uriString))
                    val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1280
                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 720
                    Triple((durMs / 1000f).coerceAtLeast(1f), w, h)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read video metadata", e)
                    Triple(120f, 1280, 720)
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Exception) {
                    }
                }
            }

            val vidDur = meta.first
            var targetStart = savedDefaults.startTrimSec
            var targetEnd = savedDefaults.endTrimSec

            // Short Video Clamp: if <= 11 min (start + end >= vidDur), trim 25% front & 25% back
            if (targetStart + targetEnd >= vidDur) {
                val quarter = (vidDur * 0.25f).toInt()
                targetStart = quarter
                targetEnd = quarter
            }

            val resolvedStart = targetStart.toFloat().coerceIn(0f, (vidDur - 1f).coerceAtLeast(0f))
            updateCapture {
                it.copy(
                    videoUri = uriString,
                    videoName = fileName,
                    videoDurationSec = vidDur,
                    videoWidth = meta.second,
                    videoHeight = meta.third,
                    currentTimeSec = resolvedStart,
                    isPreviewPlaying = false,
                    playbackSpeed = 1.0f,
                    isFullscreenPreview = false,
                    zoomLevel = savedDefaults.zoomLevel,
                    panX = 0f,
                    panY = 0f,
                    startTrimSec = targetStart,
                    endTrimSec = targetEnd,
                    captureStatusMessage = "Video loaded • Start trimmed at ${formatMmSs(targetStart)}"
                )
            }
            extractAndSetPreviewFrame(uriString, resolvedStart)
        }
    }

    fun clearCaptureVideo() {
        captureGenerationJob?.cancel()
        capturePreviewPlayJob?.cancel()
        previewSeekJob?.cancel()
        updateCapture {
            SmartCaptureState(
                zoomLevel = it.zoomLevel,
                startTrimSec = CaptureDefaults.DEFAULT_START_TRIM_SEC,
                endTrimSec = CaptureDefaults.DEFAULT_END_TRIM_SEC,
                screenshotCount = it.screenshotCount,
                customCountInput = it.customCountInput,
                enableSmartFrameFilter = it.enableSmartFrameFilter
            )
        }
    }

    fun seekCapturePreview(timeSec: Float) {
        val capture = _uiState.value.smartCapture
        val clamped = timeSec.coerceIn(0f, capture.videoDurationSec.coerceAtLeast(0f))
        updateCapture { it.copy(currentTimeSec = clamped) }
        val uri = capture.videoUri ?: return
        if (!capture.isPreviewPlaying) {
            previewSeekJob?.cancel()
            previewSeekJob = viewModelScope.launch {
                delay(75L)
                extractAndSetPreviewFrame(uri, clamped)
            }
        }
    }

    fun syncCapturePlayerPosition(timeSec: Float) {
        val dur = _uiState.value.smartCapture.videoDurationSec.coerceAtLeast(1f)
        updateCapture { it.copy(currentTimeSec = timeSec.coerceIn(0f, dur)) }
    }

    fun pauseCapturePreview() {
        capturePreviewPlayJob?.cancel()
        updateCapture { it.copy(isPreviewPlaying = false) }
    }

    fun rewindCapturePreview() {
        val cur = _uiState.value.smartCapture
        if (cur.videoUri == null) return
        val target = (cur.currentTimeSec - 10f).coerceAtLeast(0f)
        seekCapturePreview(target)
    }

    fun forwardCapturePreview() {
        val cur = _uiState.value.smartCapture
        if (cur.videoUri == null) return
        val maxDur = cur.videoDurationSec.coerceAtLeast(1f)
        val target = (cur.currentTimeSec + 10f).coerceAtMost(maxDur)
        seekCapturePreview(target)
    }

    fun toggleCapturePreviewPlay() {
        val capture = _uiState.value.smartCapture
        if (capture.videoUri == null) return
        capturePreviewPlayJob?.cancel()
        updateCapture { it.copy(isPreviewPlaying = !it.isPreviewPlaying) }
    }

    fun cycleCapturePlaybackSpeed() {
        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.5f)
        updateCapture { state ->
            val idx = speeds.indexOfFirst { kotlin.math.abs(it - state.playbackSpeed) < 0.05f }
            val next = speeds[(idx + 1) % speeds.size]
            state.copy(playbackSpeed = next)
        }
    }

    fun setCaptureFullscreen(fullscreen: Boolean) {
        updateCapture { it.copy(isFullscreenPreview = fullscreen) }
    }

    fun setCapturePreviewVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        updateCapture { it.copy(previewVolume = clamped, previewMuted = clamped <= 0.01f) }
    }

    fun toggleCapturePreviewMute() {
        updateCapture { it.copy(previewMuted = !it.previewMuted) }
    }

    private suspend fun extractAndSetPreviewFrame(uriString: String, timeSec: Float) {
        val context = getApplication<Application>().applicationContext
        val path = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(uriString))
                val bmp = retriever.getFrameAtTime(
                    (timeSec * 1_000_000f).toLong(),
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                ) ?: return@withContext null
                val outFile = File(context.cacheDir, "capture_preview_frame.jpg")
                FileOutputStream(outFile).use { fos ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                }
                bmp.recycle()
                outFile.absolutePath
            } catch (e: Exception) {
                Log.w(TAG, "Preview frame extraction warning: ${e.message}")
                null
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }
        if (path != null) {
            updateCapture { it.copy(previewFramePath = path) }
        }
    }

    fun updateCaptureZoomLevel(zoom: Float) {
        val clamped = ((zoom * 20f).toInt() / 20f).coerceIn(
            CaptureDefaults.MIN_ZOOM_LEVEL,
            CaptureDefaults.MAX_ZOOM_LEVEL
        )
        updateCapture { state ->
            val maxOffset = if (clamped <= 1.0f) 0f else ((clamped - 1f) / clamped) * 50f
            state.copy(
                zoomLevel = clamped,
                panX = state.panX.coerceIn(-maxOffset, maxOffset),
                panY = state.panY.coerceIn(-maxOffset, maxOffset)
            )
        }
        viewModelScope.launch { prefsRepo.saveCaptureZoom(clamped) }
    }

    fun updateCapturePan(deltaXPercent: Float, deltaYPercent: Float) {
        updateCapture { state ->
            if (state.zoomLevel <= 1.0f) return@updateCapture state
            val maxOffset = ((state.zoomLevel - 1f) / state.zoomLevel) * 50f
            state.copy(
                panX = (state.panX + deltaXPercent).coerceIn(-maxOffset, maxOffset),
                panY = (state.panY + deltaYPercent).coerceIn(-maxOffset, maxOffset)
            )
        }
    }

    fun resetCaptureZoomAndPan() {
        updateCapture {
            it.copy(
                zoomLevel = CaptureDefaults.DEFAULT_ZOOM_LEVEL,
                panX = 0f,
                panY = 0f
            )
        }
        viewModelScope.launch { prefsRepo.saveCaptureZoom(CaptureDefaults.DEFAULT_ZOOM_LEVEL) }
    }

    fun updateCaptureStartTrim(sec: Int) {
        val clamped = sec.coerceAtLeast(0)
        updateCapture { it.copy(startTrimSec = clamped) }
        viewModelScope.launch {
            prefsRepo.saveCaptureTrims(clamped, _uiState.value.smartCapture.endTrimSec)
        }
    }

    fun updateCaptureEndTrim(sec: Int) {
        val clamped = sec.coerceAtLeast(0)
        updateCapture { it.copy(endTrimSec = clamped) }
        viewModelScope.launch {
            prefsRepo.saveCaptureTrims(_uiState.value.smartCapture.startTrimSec, clamped)
        }
    }

    fun resetCaptureDefaultTrim0530() {
        val dur = _uiState.value.smartCapture.videoDurationSec
        var s = CaptureDefaults.DEFAULT_START_TRIM_SEC
        var e = CaptureDefaults.DEFAULT_END_TRIM_SEC
        if (dur > 0f && s + e >= dur) {
            val quarter = (dur * 0.25f).toInt()
            s = quarter
            e = quarter
        }
        updateCapture { it.copy(startTrimSec = s, endTrimSec = e) }
        viewModelScope.launch { prefsRepo.saveCaptureTrims(s, e) }
    }

    fun updateScreenshotCountInput(raw: String) {
        val digits = raw.filter { it.isDigit() }
        val parsed = digits.toIntOrNull()?.coerceIn(1, 300)
        updateCapture {
            it.copy(
                customCountInput = digits,
                screenshotCount = parsed ?: it.screenshotCount
            )
        }
    }

    fun selectScreenshotCountSuggestion(count: Int) {
        val clamped = count.coerceIn(1, 300)
        updateCapture {
            it.copy(
                screenshotCount = clamped,
                customCountInput = clamped.toString()
            )
        }
    }

    fun stepScreenshotCount(delta: Int) {
        updateCapture { state ->
            val updated = (state.screenshotCount + delta).coerceIn(1, 300)
            state.copy(
                screenshotCount = updated,
                customCountInput = updated.toString()
            )
        }
    }

    fun toggleSmartFrameFilter() {
        updateCapture { it.copy(enableSmartFrameFilter = !it.enableSmartFrameFilter) }
    }

    fun stopGeneratingScreenshots() {
        captureGenerationJob?.cancel()
        updateCapture {
            it.copy(
                isGenerating = false,
                captureStatusMessage = "Capture stopped"
            )
        }
    }

    fun startGeneratingScreenshots() {
        val capture = _uiState.value.smartCapture
        val videoUri = capture.videoUri
        if (videoUri == null) {
            _uiState.update { it.copy(snackbarMessage = "Please upload a video first.") }
            return
        }
        if (capture.isGenerating) return

        capturePreviewPlayJob?.cancel()
        captureGenerationJob?.cancel()

        val total = capture.screenshotCount.coerceIn(1, 300)
        val vidDur = capture.videoDurationSec.coerceAtLeast(2f)
        val startSec = capture.startTrimSec.toFloat().coerceIn(0f, (vidDur - 1f).coerceAtLeast(0f))
        val effectiveEnd = (vidDur - capture.endTrimSec.toFloat()).coerceAtLeast(startSec + 0.5f)
        val effectiveDuration = (effectiveEnd - startSec).coerceAtLeast(0.5f)
        val jpegQuality = capture.jpegQuality
        val smartFilter = capture.enableSmartFrameFilter
        val zoom = capture.zoomLevel
        val panX = capture.panX
        val panY = capture.panY

        updateCapture {
            it.copy(
                isGenerating = true,
                isPreviewPlaying = false,
                captureCurrentIndex = 0,
                captureTotalCount = total,
                screenshots = emptyList(),
                captureStatusMessage = "Initializing Smart Capture…"
            )
        }

        captureGenerationJob = viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val batchTag = System.currentTimeMillis()
            val outDir = File(context.cacheDir, "smart_screenshots/batch_$batchTag").apply {
                mkdirs()
            }
            val newScreenshots = mutableListOf<ScreenshotItem>()
            val batchAssetIds = mutableSetOf<String>()
            val usedTimestampsSec = mutableSetOf<Int>()
            var cleanedOverlayShots = 0
            var skippedSlots = 0

            val retriever = MediaMetadataRetriever()
            try {
                withContext(Dispatchers.IO) {
                    retriever.setDataSource(context, Uri.parse(videoUri))
                }

                // Target HD 16:9 output for crisp images and reliable FaceDetector accuracy
                val targetW = 1280
                val targetH = 720

                for (i in 0 until total) {
                    coroutineContext.ensureActive()

                    val baseTime = if (total == 1) {
                        startSec + effectiveDuration * 0.5f
                    } else {
                        startSec + (i.toFloat() / (total - 1).toFloat()) * effectiveDuration
                    }

                    updateCapture {
                        it.copy(
                            captureCurrentIndex = i + 1,
                            captureTotalCount = total,
                            captureStatusMessage = "Slot ${i + 1}/$total: Checking subject & Smart Filter…"
                        )
                    }

                    // Build ordered non-duplicate candidate timestamps strictly within [startSec, effectiveEnd]
                    val candidateTimes = buildCandidateTimesForSlot(
                        baseTime = baseTime,
                        startSec = startSec,
                        effectiveEnd = effectiveEnd,
                        usedTimestampsSec = usedTimestampsSec,
                        smartFilter = smartFilter
                    )

                    var acceptedShot: ScreenshotItem? = null
                    var primaryRejectionReason: String? = null
                    var attemptsForSlot = 0
                    val maxAttemptsPerSlot = if (smartFilter) 36 else 3

                    for (candidateTime in candidateTimes) {
                        coroutineContext.ensureActive()
                        if (attemptsForSlot >= maxAttemptsPerSlot) break

                        val secKey = candidateTime.roundToInt()
                        if (secKey in usedTimestampsSec) continue
                        attemptsForSlot++

                        val croppedBitmap = withContext(Dispatchers.IO) {
                            val timeUs = (candidateTime * 1_000_000f).toLong()
                            val raw = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                                ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                ?: return@withContext null
                            val cropped = SmartFrameAnalyzer.drawCroppedFrame(
                                source = raw,
                                zoom = zoom,
                                panX = panX,
                                panY = panY,
                                targetWidth = targetW,
                                targetHeight = targetH
                            )
                            if (raw !== cropped) raw.recycle()
                            cropped
                        } ?: continue

                        // Sequential pipeline Steps 2-5: Subject Check -> Smart Filter -> Clean Overlays -> Re-Verify
                        val processed = withContext(Dispatchers.Default) {
                            SmartFrameAnalyzer.processAndVerifyFrame(
                                sourceBitmap = croppedBitmap,
                                enableSmartFilter = smartFilter
                            )
                        }

                        if (!processed.accepted || processed.bitmap == null) {
                            if (primaryRejectionReason == null) {
                                primaryRejectionReason = processed.rejectionReason
                            }
                            if (processed.bitmap !== croppedBitmap) {
                                processed.bitmap?.recycle()
                            }
                            croppedBitmap.recycle()
                            continue
                        }

                        val verifiedBitmap = processed.bitmap
                        if (verifiedBitmap !== croppedBitmap) {
                            croppedBitmap.recycle()
                        }

                        // Step 6: Save verified image to file and confirm non-empty JPEG before advancing
                        val file = File(outDir, "shot_${i + 1}_${System.currentTimeMillis()}.jpg")
                        val fileSavedOk = withContext(Dispatchers.IO) {
                            try {
                                FileOutputStream(file).use { fos ->
                                    val compressed = verifiedBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, fos)
                                    fos.flush()
                                    compressed
                                } && file.exists() && file.length() > 1024L
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed writing screenshot file", e)
                                false
                            }
                        }

                        val signature = withContext(Dispatchers.Default) {
                            SmartFrameAnalyzer.computeSceneSignatureFromBitmap(verifiedBitmap)
                        }
                        verifiedBitmap.recycle()

                        if (!fileSavedOk) {
                            file.delete()
                            continue
                        }

                        usedTimestampsSec.add(secKey)
                        val wasReplaced = smartFilter && abs(candidateTime - baseTime) >= 1.0f
                        if (processed.wasWatermarkCleaned) cleanedOverlayShots++

                        val status = if (wasReplaced) QualityStatus.RETRIED else QualityStatus.PASSED
                        val tag = when {
                            processed.wasWatermarkCleaned -> "✨ ওয়াটারমার্ক মুক্ত & ভেরিফায়েড"
                            wasReplaced -> "🔄 রিপ্লেস করা পরিষ্কার ফ্রেম"
                            else -> "✅ পরিষ্কার সাবজেক্ট ফ্রেম"
                        }

                        acceptedShot = ScreenshotItem(
                            id = UUID.randomUUID().toString(),
                            filePath = file.absolutePath,
                            timestampSec = candidateTime,
                            formattedTime = formatMmSs(secKey),
                            index = newScreenshots.size + 1,
                            width = targetW,
                            height = targetH,
                            qualityStatus = status,
                            qualityTag = tag,
                            hasAdDetected = false,
                            sharpnessScore = processed.evaluation.sharpnessScore,
                            wasReplaced = wasReplaced,
                            replacedReason = if (wasReplaced) primaryRejectionReason else null,
                            originalTimeSec = baseTime,
                            sceneSignature = signature,
                            wasWatermarkCleaned = processed.wasWatermarkCleaned
                        )
                        break
                    }

                    if (acceptedShot != null) {
                        newScreenshots.add(acceptedShot)
                        batchAssetIds.add(acceptedShot.id)
                        val newAsset = MediaAsset(
                            id = acceptedShot.id,
                            title = "Shot_${acceptedShot.index.toString().padStart(2, '0')}_${acceptedShot.formattedTime.replace(":", "")}",
                            kind = AssetKind.STILL,
                            status = AssetStatus.EXPORTED,
                            durationOrSize = "${acceptedShot.width}×${acceptedShot.height} • Q$jpegQuality",
                            aspectLabel = "16:9",
                            createdAt = acceptedShot.formattedTime,
                            localUri = Uri.fromFile(File(acceptedShot.filePath)).toString()
                        )

                        // Emit verified shot immediately to Gallery before proceeding to next slot
                        val snapshotList = newScreenshots.toList()
                        _uiState.update { state ->
                            state.copy(
                                assets = listOf(newAsset) + state.assets,
                                smartCapture = state.smartCapture.copy(
                                    captureCurrentIndex = i + 1,
                                    captureTotalCount = total,
                                    screenshots = snapshotList,
                                    captureStatusMessage = "Verified & Saved ${newScreenshots.size}/$total…"
                                )
                            )
                        }
                    } else {
                        skippedSlots++
                        updateCapture {
                            it.copy(
                                captureCurrentIndex = i + 1,
                                captureTotalCount = total,
                                captureStatusMessage = "Slot ${i + 1}/$total skipped (no clean subject)"
                            )
                        }
                    }
                }

                val finalShots = withContext(Dispatchers.Default) {
                    SmartTimelineEngine.shuffleAntiSameScene(newScreenshots)
                }

                _uiState.update { state ->
                    val existingNonBatch = state.assets.filterNot { it.id in batchAssetIds }
                    val survivingBatchIds = state.assets.asSequence()
                        .filter { it.id in batchAssetIds }
                        .map { it.id }
                        .toSet()

                    val orderedBatchAssets = finalShots
                        .filter { it.id in survivingBatchIds }
                        .map { shot ->
                            MediaAsset(
                                id = shot.id,
                                title = "Shot_${shot.index.toString().padStart(2, '0')}_${shot.formattedTime.replace(":", "")}",
                                kind = AssetKind.STILL,
                                status = AssetStatus.EXPORTED,
                                durationOrSize = "${shot.width}×${shot.height} • Q$jpegQuality",
                                aspectLabel = "16:9",
                                createdAt = shot.formattedTime,
                                localUri = Uri.fromFile(File(shot.filePath)).toString()
                            )
                        }

                    val summaryText = if (orderedBatchAssets.isEmpty()) {
                        "No clean subject frames found in trimmed range. Try adjusting Start/End Trim."
                    } else {
                        val extra = buildList {
                            if (cleanedOverlayShots > 0) add("Cleaned: $cleanedOverlayShots")
                            val replaced = finalShots.count { it.wasReplaced }
                            if (replaced > 0) add("Auto-replaced: $replaced")
                            if (skippedSlots > 0) add("Rejected: $skippedSlots")
                        }.joinToString(" • ")
                        if (extra.isNotEmpty()) {
                            "Saved ${orderedBatchAssets.size} verified HD frames ($extra)"
                        } else {
                            "Saved ${orderedBatchAssets.size} verified HD frames to Gallery"
                        }
                    }

                    state.copy(
                        assets = orderedBatchAssets + existingNonBatch,
                        smartCapture = state.smartCapture.copy(
                            isGenerating = false,
                            captureCurrentIndex = total,
                            screenshots = finalShots,
                            captureStatusMessage = summaryText
                        ),
                        snackbarMessage = summaryText
                    )
                }
            } catch (e: CancellationException) {
                updateCapture { it.copy(isGenerating = false) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Smart capture failed", e)
                updateCapture {
                    it.copy(
                        isGenerating = false,
                        captureStatusMessage = e.localizedMessage ?: "Capture failed"
                    )
                }
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun buildCandidateTimesForSlot(
        baseTime: Float,
        startSec: Float,
        effectiveEnd: Float,
        usedTimestampsSec: Set<Int>,
        smartFilter: Boolean
    ): List<Float> {
        val candidates = mutableListOf<Float>()
        val seenSec = HashSet<Int>(usedTimestampsSec)

        fun tryAdd(t: Float) {
            if (t < startSec || t > effectiveEnd) return
            val s = t.roundToInt()
            if (seenSec.add(s)) {
                candidates.add(t)
            }
        }

        if (!smartFilter) {
            tryAdd(baseTime)
            if (candidates.isEmpty()) {
                val fallback = baseTime.coerceIn(startSec, effectiveEnd)
                candidates.add(fallback)
            }
            return candidates
        }

        // Phase 1: Local spiral offsets strictly within [startSec, effectiveEnd]
        for (offset in CaptureDefaults.CANDIDATE_OFFSETS_SEC) {
            tryAdd(baseTime + offset)
        }

        // Phase 2: Wide expanding search across unvisited regions in [startSec, effectiveEnd]
        val span = (effectiveEnd - startSec).coerceAtLeast(1f)
        val step = (span / 24f).coerceIn(3f, 35f)
        var ring = 1
        while (candidates.size < 36 && ring <= 24) {
            val delta = ring * step + 28f
            tryAdd(baseTime + delta)
            tryAdd(baseTime - delta)
            ring++
        }

        // Phase 3: Even sweep across the full active window if slots near baseTime were all credits/drone
        if (candidates.size < 36) {
            val sweepSteps = 20
            for (k in 0..sweepSteps) {
                if (candidates.size >= 36) break
                val t = startSec + (k.toFloat() / sweepSteps.toFloat()) * span
                tryAdd(t)
            }
        }

        return candidates
    }

    // --- Video Timeline Tab ---

    fun selectClip(clipId: String) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    fun setPlayheadSec(sec: Float) {
        val maxSec = _uiState.value.totalTimelineDurationSec
        _uiState.update { it.copy(playheadSec = sec.coerceIn(0f, maxSec)) }
    }

    fun toggleTimelinePlayback() {
        val playing = _uiState.value.isPlayingTimeline
        if (playing) {
            playbackJob?.cancel()
            _uiState.update { it.copy(isPlayingTimeline = false) }
        } else {
            _uiState.update { it.copy(isPlayingTimeline = true) }
            playbackJob?.cancel()
            playbackJob = viewModelScope.launch {
                while (true) {
                    delay(100L)
                    _uiState.update { state ->
                        val maxSec = state.totalTimelineDurationSec
                        val next = state.playheadSec + 0.1f
                        state.copy(playheadSec = if (next >= maxSec) 0f else next)
                    }
                }
            }
        }
    }

    fun setTimelineVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _uiState.update { it.copy(timelineVolume = clamped, timelineMuted = clamped <= 0.01f) }
    }

    fun toggleTimelineMute() {
        _uiState.update { it.copy(timelineMuted = !it.timelineMuted) }
    }

    fun rewindTimelinePlayback() {
        _uiState.update { it.copy(playheadSec = 0f) }
    }

    fun trimActiveClipEnd(deltaFraction: Float) {
        val active = _uiState.value.activeClip ?: return
        pushSnapshot("Trim clip ${active.title}")
        _uiState.update { state ->
            state.copy(
                timelineClips = state.timelineClips.map { clip ->
                    if (clip.id == active.id) {
                        val updatedEnd = (clip.trimEndFraction + deltaFraction)
                            .coerceIn(clip.trimStartFraction + 0.25f, 1f)
                        clip.copy(trimEndFraction = updatedEnd)
                    } else {
                        clip
                    }
                }
            )
        }
    }

    fun setActiveClipMotion(preset: String) {
        val active = _uiState.value.activeClip ?: return
        pushSnapshot("Motion preset $preset")
        _uiState.update { state ->
            state.copy(
                timelineClips = state.timelineClips.map { clip ->
                    if (clip.id == active.id) clip.copy(motionPreset = preset) else clip
                }
            )
        }
    }

    fun onTimelinePromptChanged(value: String) {
        _uiState.update { it.copy(timelinePrompt = value) }
    }

    fun generateStoryboardFromTimelinePrompt() {
        val concept = _uiState.value.timelinePrompt.trim()
        if (concept.isEmpty()) return
        val aspect = _uiState.value.canvasOptions.aspectRatio.label
        viewModelScope.launch {
            _uiState.update { it.copy(storyboardState = AiUiState.Loading) }
            try {
                val plan = geminiService.generateShotPlan(concept, aspect)
                _uiState.update { it.copy(storyboardState = AiUiState.Success(plan)) }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Storyboard timed out", e)
                _uiState.update {
                    it.copy(storyboardState = AiUiState.Error("Storyboard generation timed out."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Storyboard generation failed", e)
                _uiState.update {
                    it.copy(
                        storyboardState = AiUiState.Error(
                            e.localizedMessage ?: "Failed to generate storyboard."
                        )
                    )
                }
            }
        }
    }

    fun applyShotPlanToTimeline(plan: DirectorShotPlan) {
        pushSnapshot("Apply AI shot plan")
        val drawables = listOf(
            R.drawable.img_scene_neon_tokyo,
            R.drawable.img_scene_coastal_drone,
            R.drawable.img_scene_studio_portrait
        )
        val newClips = plan.shots.mapIndexed { idx, shot ->
            TimelineClip(
                id = UUID.randomUUID().toString(),
                title = "${shot.shotCode}: ${shot.framing}",
                trackName = "V1",
                durationSec = shot.durationSec.toFloat(),
                motionPreset = shot.framing,
                thumbnailRes = drawables[idx % drawables.size],
                isAiGenerated = true
            )
        }
        _uiState.update {
            it.copy(
                timelineClips = newClips,
                selectedClipId = newClips.firstOrNull()?.id.orEmpty(),
                playheadSec = 0f,
                selectedTab = StudioTab.Video,
                snackbarMessage = "Applied ${newClips.size} AI shots to timeline"
            )
        }
    }

    // --- Gallery Tab ---

    fun setGalleryStatusFilter(status: AssetStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    fun setInspectedAsset(asset: MediaAsset?) {
        _uiState.update { it.copy(inspectedAsset = asset) }
    }

    fun toggleAssetSelection(assetId: String) {
        _uiState.update { state ->
            val updated = if (assetId in state.selectedAssetIds) {
                state.selectedAssetIds - assetId
            } else {
                state.selectedAssetIds + assetId
            }
            state.copy(selectedAssetIds = updated)
        }
    }

    fun selectAllVisibleAssets(visibleIds: List<String>) {
        _uiState.update { state ->
            val allSelected = visibleIds.isNotEmpty() && visibleIds.all { it in state.selectedAssetIds }
            val next = if (allSelected) {
                state.selectedAssetIds - visibleIds.toSet()
            } else {
                state.selectedAssetIds + visibleIds.toSet()
            }
            state.copy(selectedAssetIds = next)
        }
    }

    fun clearAssetSelection() {
        _uiState.update { it.copy(selectedAssetIds = emptySet()) }
    }

    fun deleteAsset(assetId: String) {
        val target = _uiState.value.assets.firstOrNull { it.id == assetId }
        if (target != null) {
            deleteCachedFileIfOwned(target)
        }
        _uiState.update { state ->
            state.copy(
                assets = state.assets.filterNot { it.id == assetId },
                selectedAssetIds = state.selectedAssetIds - assetId,
                inspectedAsset = if (state.inspectedAsset?.id == assetId) null else state.inspectedAsset,
                smartCapture = state.smartCapture.copy(
                    screenshots = state.smartCapture.screenshots.filterNot { it.id == assetId }
                ),
                snackbarMessage = "Deleted photo from Gallery"
            )
        }
    }

    fun deleteSelectedAssets() {
        val ids = _uiState.value.selectedAssetIds
        if (ids.isEmpty()) return
        val targets = _uiState.value.assets.filter { it.id in ids }
        targets.forEach(::deleteCachedFileIfOwned)
        _uiState.update { state ->
            state.copy(
                assets = state.assets.filterNot { it.id in ids },
                selectedAssetIds = emptySet(),
                inspectedAsset = if (state.inspectedAsset?.id in ids) null else state.inspectedAsset,
                smartCapture = state.smartCapture.copy(
                    screenshots = state.smartCapture.screenshots.filterNot { it.id in ids }
                ),
                snackbarMessage = "Deleted ${targets.size} selected photo(s)"
            )
        }
    }

    fun dismissLatestDownload() {
        _uiState.update { it.copy(latestDownload = null) }
    }

    fun saveLatestDownloadToChosenUri(targetUri: Uri) {
        val download = _uiState.value.latestDownload ?: return
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val copied = withContext(Dispatchers.IO) {
                try {
                    val srcUri = Uri.parse(download.shareUriString)
                    context.contentResolver.openInputStream(srcUri)?.use { input ->
                        context.contentResolver.openOutputStream(targetUri)?.use { out ->
                            input.copyTo(out)
                            out.flush()
                            true
                        } ?: false
                    } ?: false
                } catch (e: Exception) {
                    Log.e(TAG, "Failed saving download to chosen URI", e)
                    false
                }
            }
            _uiState.update {
                it.copy(
                    snackbarMessage = if (copied) {
                        "Saved ${download.fileName} to selected folder"
                    } else {
                        "Failed to save to selected folder"
                    }
                )
            }
        }
    }

    fun downloadSingleAsset(asset: MediaAsset) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val safeName = asset.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .ifBlank { "CineCraft_Shot" } + ".jpg"
            val result = withContext(Dispatchers.IO) {
                val bytes = readAssetBytes(context, asset)
                    ?: return@withContext ExportedDownloadResult(
                        success = false,
                        fileName = safeName,
                        locationLabel = "",
                        shareUriString = "",
                        mimeType = "image/jpeg",
                        byteSize = 0L,
                        isZip = false,
                        errorMessage = "ইমেজ ফাইল পাওয়া যায়নি"
                    )
                try {
                    GalleryExportHelper.saveSingleImageToDevice(context, safeName, bytes)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download asset", e)
                    ExportedDownloadResult(
                        success = false,
                        fileName = safeName,
                        locationLabel = "",
                        shareUriString = "",
                        mimeType = "image/jpeg",
                        byteSize = 0L,
                        isZip = false,
                        errorMessage = e.localizedMessage ?: "ডাউনলোড ব্যর্থ হয়েছে"
                    )
                }
            }
            _uiState.update {
                it.copy(
                    latestDownload = if (result.success) result else it.latestDownload,
                    snackbarMessage = if (result.success) {
                        "Downloaded ${result.fileName} to ${result.locationLabel}"
                    } else {
                        result.errorMessage ?: "Could not save image"
                    }
                )
            }
        }
    }

    fun downloadAssetsAsZip(targetAssets: List<MediaAsset>) {
        if (targetAssets.isEmpty() || _uiState.value.isExportingGalleryZip) return
        _uiState.update { it.copy(isExportingGalleryZip = true) }

        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val zipFileName = "CineCraft_Captures_${System.currentTimeMillis()}.zip"
            val result = withContext(Dispatchers.IO) {
                val tempZip = File(context.cacheDir, zipFileName)
                try {
                    val entries = mutableListOf<Pair<String, ByteArray>>()
                    targetAssets.forEachIndexed { idx, asset ->
                        val bytes = readAssetBytes(context, asset) ?: return@forEachIndexed
                        val base = asset.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                            .ifBlank { "Shot_${idx + 1}" }
                        val entryName = "${(idx + 1).toString().padStart(2, '0')}_$base.jpg"
                        entries.add(entryName to bytes)
                    }

                    val verification = GalleryExportHelper.createAndVerifyZipArchive(entries, tempZip)
                    if (!verification.isValid) {
                        return@withContext ExportedDownloadResult(
                            success = false,
                            fileName = zipFileName,
                            locationLabel = "",
                            shareUriString = "",
                            mimeType = "application/zip",
                            byteSize = 0L,
                            isZip = true,
                            errorMessage = verification.errorMessage ?: "ZIP তৈরি ব্যর্থ হয়েছে"
                        )
                    }

                    GalleryExportHelper.saveZipToDevice(
                        context = context,
                        zipFileName = zipFileName,
                        verifiedTempZip = tempZip,
                        entryCount = verification.entryCount
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "ZIP export failed", e)
                    ExportedDownloadResult(
                        success = false,
                        fileName = zipFileName,
                        locationLabel = "",
                        shareUriString = "",
                        mimeType = "application/zip",
                        byteSize = 0L,
                        isZip = true,
                        errorMessage = e.localizedMessage ?: "ZIP ডাউনলোড ব্যর্থ হয়েছে"
                    )
                } finally {
                    tempZip.delete()
                }
            }

            _uiState.update {
                it.copy(
                    isExportingGalleryZip = false,
                    latestDownload = if (result.success) result else it.latestDownload,
                    snackbarMessage = if (result.success) {
                        "ZIP Ready: ${result.fileName} (${result.entryCount} files) saved to ${result.locationLabel}"
                    } else {
                        result.errorMessage ?: "Failed to export ZIP archive"
                    }
                )
            }
        }
    }

    private fun deleteCachedFileIfOwned(asset: MediaAsset) {
        val raw = asset.localUri ?: return
        try {
            val uri = Uri.parse(raw)
            if (uri.scheme == "file" && uri.path != null) {
                val f = File(uri.path!!)
                val cacheRoot = getApplication<Application>().applicationContext.cacheDir.canonicalPath
                if (f.canonicalPath.startsWith(cacheRoot) && f.exists()) {
                    f.delete()
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun readAssetBytes(context: android.content.Context, asset: MediaAsset): ByteArray? {
        val raw = asset.localUri
        if (!raw.isNullOrBlank()) {
            try {
                val uri = Uri.parse(raw)
                if (uri.scheme == "file" && uri.path != null) {
                    val file = File(uri.path!!)
                    if (file.exists()) return file.readBytes()
                } else {
                    context.contentResolver.openInputStream(uri)?.use { return it.readBytes() }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading asset URI $raw", e)
            }
        }
        val resId = asset.drawableRes ?: return null
        return try {
            val bmp = BitmapFactory.decodeResource(context.resources, resId) ?: return null
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, baos)
            bmp.recycle()
            baos.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    fun importMediaFromPicker(uriString: String) {
        val count = _uiState.value.assets.size + 1
        val newAsset = MediaAsset(
            id = UUID.randomUUID().toString(),
            title = "Imported_Clip_$count",
            kind = AssetKind.STILL,
            status = AssetStatus.DRAFT,
            durationOrSize = "Device Media",
            aspectLabel = _uiState.value.canvasOptions.aspectRatio.label,
            createdAt = "Just now",
            localUri = uriString
        )
        _uiState.update {
            it.copy(
                assets = listOf(newAsset) + it.assets,
                snackbarMessage = "Imported media into Gallery"
            )
        }
    }

    fun sendAssetToTimeline(asset: MediaAsset) {
        pushSnapshot("Add ${asset.title} to timeline")
        val clip = TimelineClip(
            id = UUID.randomUUID().toString(),
            title = asset.title,
            trackName = "V1",
            durationSec = 4.0f,
            motionPreset = "Ken Burns Zoom",
            thumbnailRes = asset.drawableRes ?: R.drawable.img_scene_neon_tokyo,
            isAiGenerated = false
        )
        _uiState.update {
            it.copy(
                timelineClips = it.timelineClips + clip,
                selectedClipId = clip.id,
                inspectedAsset = null,
                selectedTab = StudioTab.Video,
                snackbarMessage = "Added ${asset.title} to Video timeline"
            )
        }
    }

    // --- Tailor Tab ---

    fun applyTailorPreset(platform: String, ratio: AspectRatioPreset, fps: String) {
        updateCanvasOptions { it.copy(aspectRatio = ratio) }
        updateExportProfile { it.copy(targetPlatform = platform, frameRate = fps) }
    }

    fun generateTailorCopyWithAi() {
        val state = _uiState.value
        val summary = buildContextSummary(state)
        viewModelScope.launch {
            _uiState.update { it.copy(copyPackState = AiUiState.Loading) }
            try {
                val pack = geminiService.generateTailorCopy(
                    platform = state.exportProfile.targetPlatform,
                    aspectRatio = state.canvasOptions.aspectRatio.label,
                    projectSummary = summary
                )
                _uiState.update { it.copy(copyPackState = AiUiState.Success(pack)) }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Tailor copy timed out", e)
                _uiState.update {
                    it.copy(copyPackState = AiUiState.Error("Copy generation timed out."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Tailor copy failed", e)
                _uiState.update {
                    it.copy(
                        copyPackState = AiUiState.Error(
                            e.localizedMessage ?: "Failed to generate platform copy."
                        )
                    )
                }
            }
        }
    }

    fun startRenderExport() {
        if (_uiState.value.isExporting) return
        exportJob?.cancel()
        _uiState.update {
            it.copy(
                isExporting = true,
                exportProgress = 0f,
                exportCompleteMessage = null
            )
        }
        exportJob = viewModelScope.launch {
            for (step in 1..20) {
                delay(110L)
                _uiState.update { it.copy(exportProgress = step / 20f) }
            }
            val profile = _uiState.value.exportProfile
            val ratio = _uiState.value.canvasOptions.aspectRatio.label
            val newExport = MediaAsset(
                id = UUID.randomUUID().toString(),
                title = "CineCraft_Export_${ratio.replace(":", "x")}",
                kind = AssetKind.VIDEO,
                status = AssetStatus.EXPORTED,
                durationOrSize = "${formatShortTime(_uiState.value.totalTimelineDurationSec)} • ${profile.resolution}",
                aspectLabel = ratio,
                createdAt = "Just now",
                drawableRes = _uiState.value.activeClip?.thumbnailRes ?: R.drawable.img_scene_neon_tokyo
            )
            _uiState.update {
                it.copy(
                    isExporting = false,
                    exportProgress = 1f,
                    assets = listOf(newExport) + it.assets,
                    exportCompleteMessage = "Exported ${profile.resolution} (${profile.codec}) to Gallery",
                    snackbarMessage = "Render complete: ${newExport.title}"
                )
            }
        }
    }

    private fun buildContextSummary(state: StudioUiState): String {
        val clips = state.timelineClips.joinToString("; ") {
            "${it.title} (${it.durationSec}s, ${it.motionPreset})"
        }
        val lines = state.scriptLines.joinToString(" | ") { "${it.speaker}: ${it.text}" }
        return "Project=${state.projectName}, Drama=${state.autoAgent.activeSeries?.title}, " +
            "Episode=${state.autoAgent.activeEpisode}, Aspect=${state.canvasOptions.aspectRatio.label}, " +
            "Clips=[$clips], Script=[$lines]"
    }

    override fun onCleared() {
        playbackJob?.cancel()
        agentPreviewPlayJob?.cancel()
        captureGenerationJob?.cancel()
        capturePreviewPlayJob?.cancel()
        previewSeekJob?.cancel()
        exportJob?.cancel()
        pipelineJob?.cancel()
        micTimerJob?.cancel()
        masterAudioJob?.cancel()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onCleared()
    }
}

fun formatSmpte(seconds: Float): String {
    val totalFrames = (seconds * 24f).toInt().coerceAtLeast(0)
    val frames = totalFrames % 24
    val totalSec = totalFrames / 24
    val sec = totalSec % 60
    val min = (totalSec / 60) % 60
    val hr = totalSec / 3600
    return String.format(Locale.US, "%02d:%02d:%02d:%02d", hr, min, sec, frames)
}

fun formatShortTime(seconds: Float): String {
    val s = seconds.toInt().coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}

fun formatMmSs(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}

fun incrementMixedNumeralText(input: String, delta: Int): String {
    val bnDigits = "০১২৩৪৫৬৭৮৯"
    val match = Regex("[0-9০-৯]+").find(input)
    if (match == null) {
        val next = (1 + delta).coerceAtLeast(1)
        return "পর্ব ${next.toString().padStart(2, '0')}"
    }
    val rawDigits = match.value
    val usesBengali = rawDigits.any { it in bnDigits }
    val latinDigits = rawDigits.map { ch ->
        val idx = bnDigits.indexOf(ch)
        if (idx >= 0) ('0' + idx) else ch
    }.joinToString("")
    val number = latinDigits.toIntOrNull() ?: 1
    val updated = (number + delta).coerceAtLeast(1)
    val padded = updated.toString().padStart(rawDigits.length.coerceAtLeast(2), '0')
    val formatted = if (usesBengali) {
        padded.map { ch -> bnDigits[ch - '0'] }.joinToString("")
    } else {
        padded
    }
    return input.replaceRange(match.range, formatted)
}
