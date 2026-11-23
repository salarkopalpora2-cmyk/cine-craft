package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatioPreset
import com.example.model.StudioTab
import com.example.ui.theme.StudioAppCanvas
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioOnSurface
import com.example.ui.theme.StudioOnSurfaceVariant
import com.example.ui.theme.StudioPreviewCanvas
import com.example.ui.theme.StudioPrimaryContainer
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioSecondaryInteractive
import com.example.ui.theme.StudioTextStyles
import com.example.ui.theme.StudioViolet

@Composable
fun CineCraftTopBar(
    projectName: String,
    canUndo: Boolean,
    onUndoClick: () -> Unit,
    onInspectorClick: () -> Unit,
    onCanvasOptionsClick: () -> Unit,
    onExportProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StudioCardSurface
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Studio Brand Anchor
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "CineCraft Studio Mark",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CineCraft",
                                style = MaterialTheme.typography.titleMedium,
                                color = StudioOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(StudioAppCanvas)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AI",
                                    style = StudioTextStyles.CodeSm.copy(fontSize = 10.sp),
                                    color = StudioPrimaryContainer
                                )
                            }
                        }
                        Text(
                            text = projectName,
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioOnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 4 Horizontal Action Slots (40dp visual circle, 48dp accessible touch target)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopBarCircleAction(
                        icon = Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = "Undo last action",
                        tint = if (canUndo) StudioOnSurface else StudioOnSurfaceVariant.copy(alpha = 0.4f),
                        enabled = canUndo,
                        testTag = "top_undo_button",
                        onClick = onUndoClick
                    )
                    TopBarCircleAction(
                        icon = Icons.Rounded.AutoAwesome,
                        contentDescription = "Open AI Inspector",
                        tint = StudioViolet,
                        testTag = "top_inspector_button",
                        onClick = onInspectorClick
                    )
                    TopBarCircleAction(
                        icon = Icons.Rounded.AspectRatio,
                        contentDescription = "Canvas Options",
                        tint = StudioPrimaryContainer,
                        testTag = "top_canvas_options_button",
                        onClick = onCanvasOptionsClick
                    )
                    TopBarCircleAction(
                        icon = Icons.Rounded.IosShare,
                        contentDescription = "Export Profile",
                        tint = StudioOnSurface,
                        testTag = "top_export_profile_button",
                        onClick = onExportProfileClick
                    )
                }
            }
            HorizontalDivider(thickness = 1.dp, color = StudioBorderSubtle)
        }
    }
}

@Composable
private fun TopBarCircleAction(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    testTag: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .shadow(
                elevation = 2.dp,
                shape = CircleShape,
                spotColor = StudioPrimaryContainer.copy(alpha = 0.14f),
                ambientColor = StudioPrimaryContainer.copy(alpha = 0.08f)
            )
            .clip(CircleShape)
            .background(StudioCardSurface)
            .border(1.dp, StudioBorderSubtle, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun CineCraftBottomDock(
    selectedTab: StudioTab,
    isRecording: Boolean,
    galleryCount: Int,
    onTabSelected: (StudioTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                spotColor = StudioPreviewCanvas.copy(alpha = 0.08f),
                ambientColor = StudioPreviewCanvas.copy(alpha = 0.04f)
            )
            .testTag("cine_craft_bottom_dock"),
        color = StudioCardSurface
    ) {
        Column {
            HorizontalDivider(thickness = 1.dp, color = StudioBorderSubtle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(68.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val activeIcon = when (tab) {
                        StudioTab.Agent -> Icons.Filled.AutoAwesome
                        StudioTab.Voice -> Icons.Filled.GraphicEq
                        StudioTab.Capture -> Icons.Filled.Videocam
                        StudioTab.Video -> Icons.Filled.Movie
                        StudioTab.Gallery -> Icons.Filled.PhotoLibrary
                        StudioTab.Tailor -> Icons.Filled.Tune
                    }
                    val inactiveIcon = when (tab) {
                        StudioTab.Agent -> Icons.Outlined.AutoAwesome
                        StudioTab.Voice -> Icons.Outlined.GraphicEq
                        StudioTab.Capture -> Icons.Outlined.Videocam
                        StudioTab.Video -> Icons.Outlined.Movie
                        StudioTab.Gallery -> Icons.Outlined.PhotoLibrary
                        StudioTab.Tailor -> Icons.Outlined.Tune
                    }
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) StudioPrimaryContainer else StudioOnSurfaceVariant,
                        animationSpec = tween(150),
                        label = "tabColor"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onTabSelected(tab) }
                            .testTag("nav_tab_${tab.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(30.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        StudioSecondaryInteractive.copy(alpha = 0.12f)
                                    } else {
                                        Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) activeIcon else inactiveIcon,
                                contentDescription = "${tab.label} workspace",
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )

                            if (tab == StudioTab.Capture && isRecording) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-6).dp, y = 3.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StudioRose)
                                )
                            } else if (tab == StudioTab.Gallery && galleryCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-3).dp, y = 1.dp)
                                        .clip(CircleShape)
                                        .background(StudioPrimaryContainer)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = galleryCount.toString(),
                                        style = StudioTextStyles.CodeSm.copy(fontSize = 9.sp),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudioCard(
    modifier: Modifier = Modifier,
    containerColor: Color = StudioCardSurface,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = StudioPrimaryContainer.copy(alpha = 0.10f),
                ambientColor = StudioPrimaryContainer.copy(alpha = 0.06f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(1.dp, StudioBorderSubtle)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun StudioPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = StudioPrimaryContainer,
    testTag: String = "primary_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150, easing = EaseOut),
        label = "primaryButtonScale"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (enabled) containerColor else containerColor.copy(alpha = 0.45f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StudioSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String = "secondary_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150, easing = EaseOut),
        label = "secondaryButtonScale"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(if (isPressed) Color(0xFFF9FBFF) else StudioCardSurface)
            .border(1.5.dp, StudioBorderSubtle, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 18.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = StudioPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) StudioPrimaryContainer else StudioOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun <T> StudioSegmentedControl(
    options: List<T>,
    selectedOption: T,
    labelProvider: (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "segment"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(StudioAppCanvas)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val selected = option == selectedOption
            val label = labelProvider(option)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .then(
                        if (selected) {
                            Modifier.shadow(
                                elevation = 3.dp,
                                shape = CircleShape,
                                spotColor = StudioPrimaryContainer.copy(alpha = 0.12f)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clip(CircleShape)
                    .background(if (selected) StudioCardSurface else Color.Transparent)
                    .clickable { onOptionSelected(option) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("${testTagPrefix}_${label.lowercase().replace(":", "_").replace(" ", "_")}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = if (selected) StudioPrimaryContainer else StudioOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StudioParameterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = StudioPrimaryContainer,
    testTag: String = "chip_${label.lowercase().replace(" ", "_")}"
) {
    val bgColor = if (selected) accentColor.copy(alpha = 0.12f) else Color.Transparent
    val borderColor = if (selected) accentColor.copy(alpha = 0.35f) else StudioBorderSubtle
    val textColor = if (selected) accentColor else StudioOnSurfaceVariant

    Box(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StudioPromptTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 52.dp,
    singleLine: Boolean = true,
    onSubmit: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    testTag: String = "studio_prompt_input"
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .then(
                if (isFocused) {
                    Modifier
                        .border(3.dp, StudioPrimaryContainer.copy(alpha = 0.15f), CircleShape)
                        .padding(3.dp)
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(CircleShape)
                .background(StudioCardSurface)
                .border(
                    width = 1.5.dp,
                    color = if (isFocused) StudioPrimaryContainer else StudioBorderSubtle,
                    shape = CircleShape
                )
                .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioOnSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = StudioOnSurface),
                    cursorBrush = SolidColor(StudioPrimaryContainer),
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (onSubmit != null) ImeAction.Send else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { onSubmit?.invoke() },
                        onDone = { onSubmit?.invoke() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .testTag(testTag)
                )
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingContent()
            }
        }
    }
}

@Composable
fun PreviewMonitorFrame(
    aspectRatio: AspectRatioPreset,
    timecode: String,
    showGrid: Boolean,
    showSafeZones: Boolean,
    badgeLabel: String,
    modifier: Modifier = Modifier,
    badgeColor: Color = Color.Black.copy(alpha = 0.60f),
    topRightOverlay: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(StudioPreviewCanvas)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        val ratioModifier = when (aspectRatio) {
            AspectRatioPreset.WIDE_16_9 -> Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
            AspectRatioPreset.VERTICAL_9_16 -> Modifier
                .fillMaxHeight()
                .aspectRatio(9f / 16f)
            AspectRatioPreset.SQUARE_1_1 -> Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
        }

        Box(
            modifier = ratioModifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            content()

            if (showGrid || showSafeZones) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (showGrid) {
                        val gridColor = Color.White.copy(alpha = 0.22f)
                        val thirdW = size.width / 3f
                        val thirdH = size.height / 3f
                        drawLine(gridColor, Offset(thirdW, 0f), Offset(thirdW, size.height), 1.dp.toPx())
                        drawLine(gridColor, Offset(thirdW * 2f, 0f), Offset(thirdW * 2f, size.height), 1.dp.toPx())
                        drawLine(gridColor, Offset(0f, thirdH), Offset(size.width, thirdH), 1.dp.toPx())
                        drawLine(gridColor, Offset(0f, thirdH * 2f), Offset(size.width, thirdH * 2f), 1.dp.toPx())
                    }
                    if (showSafeZones) {
                        val insetX = size.width * 0.08f
                        val insetY = size.height * 0.08f
                        drawRect(
                            color = Color(0xFF93C5FD).copy(alpha = 0.45f),
                            topLeft = Offset(insetX, insetY),
                            size = androidx.compose.ui.geometry.Size(
                                size.width - insetX * 2f,
                                size.height - insetY * 2f
                            ),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                            )
                        )
                    }
                }
            }
        }

        // Top-left SMPTE Timecode badge
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(CircleShape)
                .background(badgeColor)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timecode,
                style = StudioTextStyles.CodeSm,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = badgeLabel,
                style = StudioTextStyles.CodeSm,
                color = Color(0xFF93C5FD)
            )
        }

        if (topRightOverlay != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                topRightOverlay()
            }
        }
    }
}

@Composable
fun StudioSectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = StudioOnSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioOnSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}
