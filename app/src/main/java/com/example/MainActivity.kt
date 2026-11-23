package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.configureAppCheck
import com.example.model.StudioTab
import com.example.ui.components.CineCraftBottomDock
import com.example.ui.components.CineCraftTopBar
import com.example.ui.screens.AgentScreen
import com.example.ui.screens.AiInspectorSheet
import com.example.ui.screens.CanvasOptionsSheet
import com.example.ui.screens.CaptureScreen
import com.example.ui.screens.ExportProfileSheet
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.TailorScreen
import com.example.ui.screens.VideoScreen
import com.example.ui.screens.VoiceScreen
import com.example.ui.theme.CineCraftTheme
import com.example.ui.theme.StudioAppCanvas
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioOnSurface
import com.example.ui.theme.StudioOnSurfaceVariant
import com.example.ui.theme.StudioPrimaryContainer
import com.example.ui.theme.StudioSecondaryInteractive
import com.example.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            CineCraftTheme {
                CineCraftStudioRoot()
            }
        }
    }
}

@Composable
fun CineCraftStudioRoot(
    studioViewModel: StudioViewModel = viewModel()
) {
    val uiState by studioViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = uiState.selectedTab != StudioTab.Agent) {
        studioViewModel.selectTab(StudioTab.Agent)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = if (uiState.canRedo) "Redo" else null,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            studioViewModel.redo()
        }
        studioViewModel.clearSnackbar()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioAppCanvas)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = StudioAppCanvas,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CineCraftTopBar(
                    projectName = uiState.projectName,
                    canUndo = uiState.canUndo,
                    onUndoClick = studioViewModel::undo,
                    onInspectorClick = { studioViewModel.setInspectorSheetVisible(true) },
                    onCanvasOptionsClick = { studioViewModel.setCanvasSheetVisible(true) },
                    onExportProfileClick = { studioViewModel.setExportSheetVisible(true) }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    CineCraftBottomDock(
                        selectedTab = uiState.selectedTab,
                        isRecording = uiState.smartCapture.isGenerating || uiState.autoAgent.isMicRecording,
                        galleryCount = uiState.assets.size,
                        onTabSelected = studioViewModel::selectTab
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = StudioOnSurface,
                        contentColor = Color.White,
                        actionColor = Color(0xFF93C5FD)
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    )
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding(),
                        containerColor = StudioCardSurface
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val icon = when (tab) {
                                StudioTab.Agent -> Icons.Filled.AutoAwesome
                                StudioTab.Voice -> Icons.Filled.GraphicEq
                                StudioTab.Capture -> Icons.Filled.Videocam
                                StudioTab.Video -> Icons.Filled.Movie
                                StudioTab.Gallery -> Icons.Filled.PhotoLibrary
                                StudioTab.Tailor -> Icons.Filled.Tune
                            }
                            NavigationRailItem(
                                selected = tab == uiState.selectedTab,
                                onClick = { studioViewModel.selectTab(tab) },
                                icon = {
                                    Icon(imageVector = icon, contentDescription = "${tab.label} workspace")
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = StudioPrimaryContainer,
                                    selectedTextColor = StudioPrimaryContainer,
                                    indicatorColor = StudioSecondaryInteractive.copy(alpha = 0.12f),
                                    unselectedIconColor = StudioOnSurfaceVariant,
                                    unselectedTextColor = StudioOnSurfaceVariant
                                ),
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    AnimatedContent(
                        targetState = uiState.selectedTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180)) togetherWith
                                fadeOut(animationSpec = tween(140))
                        },
                        label = "studioTabContent"
                    ) { tab ->
                        when (tab) {
                            StudioTab.Agent -> AgentScreen(
                                uiState = uiState,
                                onTogglePreviewPlaying = studioViewModel::togglePreviewPlaying,
                                onSeekPreview = studioViewModel::seekAgentPreview,
                                onRewindPreview = studioViewModel::rewindAgentPreview,
                                onPreviewVolumeChanged = studioViewModel::setAgentPreviewVolume,
                                onTogglePreviewMute = studioViewModel::toggleAgentPreviewMute,
                                onUpdateReleaseDate = studioViewModel::updateReleaseDate,
                                onSelectSeriesById = studioViewModel::selectSeriesById,
                                onToggleLowerThird = studioViewModel::toggleLowerThird,
                                onUpdateActiveEpisode = studioViewModel::updateActiveEpisode,
                                onStepEpisode = studioViewModel::stepEpisode,
                                onSelectChannelSlot = studioViewModel::selectChannelSlot,
                                onRenameChannel = studioViewModel::updateChannelName,
                                onToggleChannelNameQuick = studioViewModel::toggleChannelNameQuick,
                                onToggleSubtitles = studioViewModel::toggleSubtitles,
                                onToggleEffects = studioViewModel::toggleEffects,
                                onToggleCtaQuick = studioViewModel::toggleCtaQuick,
                                onUploadVideo = studioViewModel::onUploadSourceVideo,
                                onClearVideo = studioViewModel::clearSourceVideo,
                                onStepZoomLevel = studioViewModel::stepZoomLevel,
                                onAdjustStartTrim = studioViewModel::adjustStartTrim,
                                onAdjustEndTrim = studioViewModel::adjustEndTrim,
                                onAdjustFinalDuration = studioViewModel::adjustFinalDuration,
                                onResetFinalDurationAuto = studioViewModel::resetFinalDurationAuto,
                                onAdaptScriptToFinalDuration = studioViewModel::adaptScriptToFinalDuration,
                                onChangePreferredVoiceSource = studioViewModel::changePreferredVoiceSource,
                                onUploadRecordedVoice = studioViewModel::onUploadRecordedVoice,
                                onClearRecordedVoice = studioViewModel::clearRecordedVoice,
                                onStartMicRecording = studioViewModel::startMicRecording,
                                onStopMicRecording = studioViewModel::stopMicRecording,
                                onToggleForceRegenerateVoice = studioViewModel::toggleForceRegenerateVoice,
                                onScriptTextChange = studioViewModel::onScriptTextChanged,
                                onDraftScriptWithAi = studioViewModel::draftDramaScriptWithAi,
                                onUploadBgm = studioViewModel::onUploadBgm,
                                onClearBgm = studioViewModel::clearBgm,
                                onUpdateMixSettings = studioViewModel::updateMixSettings,
                                onMixPreviewAudio = studioViewModel::mixPreviewAudio,
                                onToggleMasterAudioPlay = studioViewModel::toggleMasterAudioPlay,
                                onSeekMasterAudio = studioViewModel::seekMasterAudio,
                                onStepMasterAudio = studioViewModel::stepMasterAudio,
                                onRestartMasterAudio = studioViewModel::restartMasterAudio,
                                onStopMasterAudio = studioViewModel::stopMasterAudio,
                                onToggleMasterAudioMute = studioViewModel::toggleMasterAudioMute,
                                onMasterAudioVolumeChanged = studioViewModel::setMasterAudioVolume,
                                onMasterAudioSpeedChanged = studioViewModel::setMasterAudioSpeed,
                                onUpdatePerformanceMode = studioViewModel::updatePerformanceMode,
                                onUpdateExportRatio = studioViewModel::updateExportRatio,
                                onUpdateExportFps = studioViewModel::updateExportFps,
                                onToggleWatermarkQuick = studioViewModel::toggleWatermarkQuick,
                                onToggleIntroPanel = studioViewModel::toggleIntroPanel,
                                onAttachIntroVideo = studioViewModel::attachIntroVideo,
                                onRemoveAttachedIntro = studioViewModel::removeAttachedIntro,
                                onSetChannelSettingsVisible = studioViewModel::setChannelSettingsModalVisible,
                                onGenerateClick = studioViewModel::handleGenerateClick,
                                onStopPipeline = studioViewModel::cancelAutonomousPipeline,
                                onDismissPreFlight = studioViewModel::dismissPreFlightModal,
                                onFocusMissingField = studioViewModel::focusFieldForMissingKey,
                                onProceedPastPreFlight = studioViewModel::startAutonomousPipeline,
                                onOpenVideoTab = { studioViewModel.selectTab(StudioTab.Video) },
                                onDownloadExportedMp4 = {
                                    studioViewModel.selectTab(StudioTab.Gallery)
                                }
                            )

                            StudioTab.Voice -> VoiceScreen(
                                uiState = uiState,
                                onSelectPersona = studioViewModel::selectVoicePersona,
                                onPitchChanged = studioViewModel::setVoicePitch,
                                onSpeedChanged = studioViewModel::setVoiceSpeed,
                                onSelectScriptLine = studioViewModel::selectScriptLine,
                                onToggleSpeakLine = studioViewModel::toggleSpeakLine,
                                onPolishSelectedLine = studioViewModel::polishSelectedLineWithAi,
                                onAddScriptLine = studioViewModel::addScriptLine
                            )

                            StudioTab.Capture -> CaptureScreen(
                                state = uiState.smartCapture,
                                onLoadVideoFile = studioViewModel::loadCaptureVideoFile,
                                onClearVideo = studioViewModel::clearCaptureVideo,
                                onSeekPreview = studioViewModel::seekCapturePreview,
                                onRewindPreview = studioViewModel::rewindCapturePreview,
                                onForwardPreview = studioViewModel::forwardCapturePreview,
                                onTogglePreviewPlay = studioViewModel::toggleCapturePreviewPlay,
                                onSyncPlayerTime = studioViewModel::syncCapturePlayerPosition,
                                onPausePreview = studioViewModel::pauseCapturePreview,
                                onPreviewVolumeChanged = studioViewModel::setCapturePreviewVolume,
                                onTogglePreviewMute = studioViewModel::toggleCapturePreviewMute,
                                onCyclePlaybackSpeed = studioViewModel::cycleCapturePlaybackSpeed,
                                onToggleFullscreen = {
                                    studioViewModel.setCaptureFullscreen(!uiState.smartCapture.isFullscreenPreview)
                                },
                                onUpdateZoomLevel = studioViewModel::updateCaptureZoomLevel,
                                onUpdatePan = studioViewModel::updateCapturePan,
                                onUpdateStartTrim = studioViewModel::updateCaptureStartTrim,
                                onUpdateEndTrim = studioViewModel::updateCaptureEndTrim,
                                onStepCount = studioViewModel::stepScreenshotCount,
                                onToggleSmartFrameFilter = studioViewModel::toggleSmartFrameFilter,
                                onStartGenerating = studioViewModel::startGeneratingScreenshots,
                                onStopGenerating = studioViewModel::stopGeneratingScreenshots
                            )

                            StudioTab.Video -> VideoScreen(
                                uiState = uiState,
                                onSelectClip = studioViewModel::selectClip,
                                onPlayheadChanged = studioViewModel::setPlayheadSec,
                                onTogglePlayback = studioViewModel::toggleTimelinePlayback,
                                onRewindPlayback = studioViewModel::rewindTimelinePlayback,
                                onVolumeChanged = studioViewModel::setTimelineVolume,
                                onToggleMute = studioViewModel::toggleTimelineMute,
                                onTrimClipEnd = studioViewModel::trimActiveClipEnd,
                                onSetMotionPreset = studioViewModel::setActiveClipMotion,
                                onTimelinePromptChanged = studioViewModel::onTimelinePromptChanged,
                                onGenerateStoryboard = studioViewModel::generateStoryboardFromTimelinePrompt,
                                onApplyShotPlan = studioViewModel::applyShotPlanToTimeline
                            )

                            StudioTab.Gallery -> GalleryScreen(
                                uiState = uiState,
                                onFilterSelected = studioViewModel::setGalleryStatusFilter,
                                onImportMedia = studioViewModel::importMediaFromPicker,
                                onInspectAsset = studioViewModel::setInspectedAsset,
                                onSendToTimeline = studioViewModel::sendAssetToTimeline,
                                onToggleSelectAsset = studioViewModel::toggleAssetSelection,
                                onSelectAllVisible = studioViewModel::selectAllVisibleAssets,
                                onClearSelection = studioViewModel::clearAssetSelection,
                                onDeleteAsset = studioViewModel::deleteAsset,
                                onDeleteSelected = studioViewModel::deleteSelectedAssets,
                                onDownloadAsset = studioViewModel::downloadSingleAsset,
                                onDownloadZip = studioViewModel::downloadAssetsAsZip,
                                onSaveDownloadAsUri = studioViewModel::saveLatestDownloadToChosenUri,
                                onDismissLatestDownload = studioViewModel::dismissLatestDownload
                            )

                            StudioTab.Tailor -> TailorScreen(
                                uiState = uiState,
                                onAspectChanged = { ratio ->
                                    studioViewModel.updateCanvasOptions { it.copy(aspectRatio = ratio) }
                                },
                                onApplyPlatformPreset = studioViewModel::applyTailorPreset,
                                onUpdateExportProfile = studioViewModel::updateExportProfile,
                                onGenerateCopyPack = studioViewModel::generateTailorCopyWithAi,
                                onStartRenderExport = studioViewModel::startRenderExport
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showInspectorSheet) {
        AiInspectorSheet(
            uiState = uiState,
            onDismiss = { studioViewModel.setInspectorSheetVisible(false) },
            onRunAudit = studioViewModel::runProjectInspector
        )
    }

    if (uiState.showCanvasSheet) {
        CanvasOptionsSheet(
            options = uiState.canvasOptions,
            onDismiss = { studioViewModel.setCanvasSheetVisible(false) },
            onUpdateOptions = studioViewModel::updateCanvasOptions
        )
    }

    if (uiState.showExportSheet) {
        ExportProfileSheet(
            profile = uiState.exportProfile,
            onDismiss = { studioViewModel.setExportSheetVisible(false) },
            onUpdateProfile = studioViewModel::updateExportProfile,
            onExportNow = {
                studioViewModel.setExportSheetVisible(false)
                studioViewModel.selectTab(StudioTab.Tailor)
            }
        )
    }
}
