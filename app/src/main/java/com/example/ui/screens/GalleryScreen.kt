package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.GalleryExportHelper
import com.example.model.AssetKind
import com.example.model.AssetStatus
import com.example.model.MediaAsset
import com.example.ui.components.StudioCard
import com.example.ui.components.StudioParameterChip
import com.example.ui.components.StudioPrimaryButton
import com.example.ui.components.StudioSecondaryButton
import com.example.ui.components.StudioSectionHeader
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
import com.example.ui.theme.StudioViolet
import com.example.viewmodel.StudioUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    uiState: StudioUiState,
    onFilterSelected: (AssetStatus?) -> Unit,
    onImportMedia: (String) -> Unit,
    onInspectAsset: (MediaAsset?) -> Unit,
    onSendToTimeline: (MediaAsset) -> Unit,
    onToggleSelectAsset: (String) -> Unit,
    onSelectAllVisible: (List<String>) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteAsset: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onDownloadAsset: (MediaAsset) -> Unit,
    onDownloadZip: (List<MediaAsset>) -> Unit,
    onSaveDownloadAsUri: (Uri) -> Unit = {},
    onDismissLatestDownload: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportMedia(uri.toString())
        }
    }

    val saveAsImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri: Uri? ->
        if (uri != null) onSaveDownloadAsUri(uri)
    }

    val saveAsZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) onSaveDownloadAsUri(uri)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun ensureNotificationPermissionHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val filteredAssets = uiState.assets.filter { asset ->
        uiState.selectedStatusFilter == null || asset.status == uiState.selectedStatusFilter
    }
    val visibleIds = filteredAssets.map { it.id }
    val selectedVisible = filteredAssets.filter { it.id in uiState.selectedAssetIds }
    val allVisibleSelected = visibleIds.isNotEmpty() && visibleIds.all { it in uiState.selectedAssetIds }
    val hasSelection = uiState.selectedAssetIds.isNotEmpty()
    val zipTargetList = if (selectedVisible.isNotEmpty()) selectedVisible else filteredAssets
    val rows = filteredAssets.chunked(2)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StudioSectionHeader(
                title = "Studio Gallery",
                subtitle = if (filteredAssets.isEmpty()) {
                    "No captured photos yet"
                } else {
                    "${filteredAssets.size} captured photo(s) • ${uiState.selectedAssetIds.size} selected"
                },
                trailing = {
                    StudioSecondaryButton(
                        text = "Import",
                        icon = Icons.Rounded.AddPhotoAlternate,
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        testTag = "gallery_import_media_button"
                    )
                }
            )
        }

        // Download Complete Banner with real Open/Share + Save As actions
        val latest = uiState.latestDownload
        if (latest != null && latest.success) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gallery_download_ready_banner"),
                    shape = RoundedCornerShape(16.dp),
                    color = StudioEmerald.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (latest.isZip) {
                                        "ZIP Ready (${latest.entryCount} files) • ${GalleryExportHelper.formatByteSize(latest.byteSize)}"
                                    } else {
                                        "Image Saved • ${GalleryExportHelper.formatByteSize(latest.byteSize)}"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = StudioEmerald
                                )
                                Text(
                                    text = "${latest.fileName} -> ${latest.locationLabel}",
                                    style = StudioTextStyles.CodeSm,
                                    color = StudioOnSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = onDismissLatestDownload,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, StudioBorderSubtle),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Close", style = MaterialTheme.typography.labelSmall, color = StudioOnSurfaceVariant)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val shareUri = Uri.parse(latest.shareUriString)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = latest.mimeType
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Open or Share ${latest.fileName}")
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioEmerald,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("gallery_download_open_share_button")
                            ) {
                                Text("Open / Share", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (latest.isZip) {
                                        saveAsZipLauncher.launch(latest.fileName)
                                    } else {
                                        saveAsImageLauncher.launch(latest.fileName)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, StudioEmerald),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("gallery_download_save_as_button")
                            ) {
                                Text("Save As…", style = MaterialTheme.typography.labelMedium, color = StudioEmerald)
                            }
                        }
                    }
                }
            }
        }

        // Background Smart Capture Active Indicator (survives tab switching)
        if (uiState.smartCapture.isGenerating) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = StudioPrimaryContainer.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, StudioPrimaryContainer.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = StudioPrimaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart Capture running in background…",
                                style = MaterialTheme.typography.labelLarge,
                                color = StudioPrimaryContainer
                            )
                            Text(
                                text = "${uiState.smartCapture.captureCurrentIndex}/${uiState.smartCapture.captureTotalCount} frames arriving live",
                                style = StudioTextStyles.CodeSm,
                                color = StudioOnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Status Filter Chips
        if (uiState.assets.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioParameterChip(
                        label = "All (${uiState.assets.size})",
                        selected = uiState.selectedStatusFilter == null,
                        onClick = { onFilterSelected(null) },
                        testTag = "gallery_filter_all"
                    )
                    AssetStatus.entries.forEach { status ->
                        val accent = when (status) {
                            AssetStatus.EXPORTED -> StudioEmerald
                            AssetStatus.DRAFT -> StudioAmber
                            AssetStatus.SCHEDULED -> StudioViolet
                        }
                        StudioParameterChip(
                            label = status.label,
                            selected = uiState.selectedStatusFilter == status,
                            accentColor = accent,
                            onClick = { onFilterSelected(status) },
                            testTag = "gallery_filter_${status.name.lowercase()}"
                        )
                    }
                }
            }

            // Bulk Action Toolbar: Select All, Delete Selected, Download ZIP
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = StudioCardSurface,
                    border = BorderStroke(1.dp, StudioBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onSelectAllVisible(visibleIds) },
                            enabled = filteredAssets.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (allVisibleSelected) StudioSecondaryInteractive else StudioBorderSubtle
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("gallery_select_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SelectAll,
                                contentDescription = null,
                                tint = if (allVisibleSelected) StudioSecondaryInteractive else StudioOnSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (allVisibleSelected) "Deselect All" else "Select All (${filteredAssets.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (allVisibleSelected) StudioSecondaryInteractive else StudioOnSurface
                            )
                        }

                        if (hasSelection) {
                            Button(
                                onClick = onDeleteSelected,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioRose,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("gallery_delete_selected_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete selected",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Delete (${uiState.selectedAssetIds.size})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            OutlinedButton(
                                onClick = onClearSelection,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, StudioBorderSubtle),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("gallery_clear_selection_button")
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelMedium, color = StudioOnSurfaceVariant)
                            }
                        }

                        Button(
                            onClick = {
                                ensureNotificationPermissionHint()
                                onDownloadZip(zipTargetList)
                            },
                            enabled = zipTargetList.isNotEmpty() && !uiState.isExportingGalleryZip,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioEmerald,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("gallery_download_zip_button")
                        ) {
                            if (uiState.isExportingGalleryZip) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.FolderZip,
                                    contentDescription = "Download ZIP",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedVisible.isNotEmpty()) {
                                    "Download ZIP (${selectedVisible.size})"
                                } else {
                                    "Download All ZIP (${filteredAssets.size})"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        if (rows.isEmpty()) {
            item {
                StudioCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Photo,
                            contentDescription = null,
                            tint = StudioOnSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (uiState.assets.isEmpty()) {
                                "Gallery is empty"
                            } else {
                                "No assets match this filter"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = StudioOnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Capture screenshots in the Capture tab or tap Import to add media.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioOnSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(rows) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { asset ->
                        val isSelected = asset.id in uiState.selectedAssetIds
                        GalleryAssetCard(
                            asset = asset,
                            isSelected = isSelected,
                            onCardClick = {
                                if (hasSelection) {
                                    onToggleSelectAsset(asset.id)
                                } else {
                                    onInspectAsset(asset)
                                }
                            },
                            onToggleSelect = { onToggleSelectAsset(asset.id) },
                            onDownload = {
                                ensureNotificationPermissionHint()
                                onDownloadAsset(asset)
                            },
                            onDelete = { onDeleteAsset(asset.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    val inspected = uiState.inspectedAsset
    if (inspected != null) {
        ModalBottomSheet(
            onDismissRequest = { onInspectAsset(null) },
            containerColor = StudioCardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = inspected.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = StudioOnSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${inspected.kind.label} • ${inspected.durationOrSize} • ${inspected.aspectLabel}",
                    style = StudioTextStyles.CodeSm,
                    color = StudioPrimaryContainer
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioPreviewCanvas)
                ) {
                    AssetThumbnailImage(asset = inspected)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            ensureNotificationPermissionHint()
                            onDownloadAsset(inspected)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("gallery_sheet_download_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download")
                    }
                    Button(
                        onClick = { onDeleteAsset(inspected.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioRose,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("gallery_sheet_delete_button")
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                StudioPrimaryButton(
                    text = "Send to Video Timeline",
                    icon = Icons.Rounded.PlaylistAdd,
                    onClick = { onSendToTimeline(inspected) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "send_asset_to_timeline_button"
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun GalleryAssetCard(
    asset: MediaAsset,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (asset.status) {
        AssetStatus.EXPORTED -> StudioEmerald
        AssetStatus.DRAFT -> StudioAmber
        AssetStatus.SCHEDULED -> StudioViolet
    }
    val kindIcon = when (asset.kind) {
        AssetKind.VIDEO -> Icons.Rounded.Movie
        AssetKind.STILL -> Icons.Rounded.Photo
        AssetKind.VOICEOVER -> Icons.Rounded.RecordVoiceOver
    }

    Surface(
        modifier = modifier
            .clickable(onClick = onCardClick)
            .testTag("gallery_asset_card_${asset.id}"),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) StudioSecondaryInteractive.copy(alpha = 0.08f) else StudioCardSurface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) StudioSecondaryInteractive else StudioBorderSubtle
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioPreviewCanvas)
            ) {
                AssetThumbnailImage(asset = asset)

                // Top-left type badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = kindIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = asset.createdAt,
                            style = StudioTextStyles.CodeSm,
                            color = Color.White
                        )
                    }
                }

                // Top-right selection checkbox (48dp touch area)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .clickable(onClick = onToggleSelect)
                        .testTag("gallery_select_checkbox_${asset.id}"),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (isSelected) StudioSecondaryInteractive else Color.Black.copy(alpha = 0.55f)
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(7.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = if (isSelected) "Selected" else "Select photo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = asset.title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = StudioOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = asset.durationOrSize,
                style = StudioTextStyles.CodeSm,
                color = StudioOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Footer: Status pill + Single Download + Single Delete buttons (48dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = asset.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDownload)
                            .testTag("gallery_download_single_${asset.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Download ${asset.title}",
                            tint = StudioEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDelete)
                            .testTag("gallery_delete_single_${asset.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete ${asset.title}",
                            tint = StudioRose,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetThumbnailImage(asset: MediaAsset) {
    if (!asset.localUri.isNullOrBlank()) {
        AsyncImage(
            model = asset.localUri,
            contentDescription = asset.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Image(
            painter = painterResource(id = asset.drawableRes ?: R.drawable.img_scene_neon_tokyo),
            contentDescription = asset.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
