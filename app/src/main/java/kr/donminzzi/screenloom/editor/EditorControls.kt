package kr.donminzzi.screenloom.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kr.donminzzi.screenloom.R
import kr.donminzzi.screenloom.render.colors
import kr.donminzzi.screenloom.ui.theme.Cobalt
import kr.donminzzi.screenloom.ui.theme.ElevatedPaper
import kr.donminzzi.screenloom.ui.theme.Ink
import kr.donminzzi.screenloom.ui.theme.MutedInk
import kr.donminzzi.screenloom.ui.theme.Outline
import kr.donminzzi.screenloom.ui.theme.Paper
import kr.donminzzi.screenloom.ui.theme.SelectedWash

private enum class EditorTab(val labelRes: Int) {
    Layout(R.string.tab_layout),
    Copy(R.string.tab_copy),
    Style(R.string.tab_style),
}

@Composable
internal fun EditorControls(
    document: EditorDocument,
    enabled: Boolean,
    onAction: (EditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(EditorTab.Layout) }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Outline),
        tonalElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                EditorTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    Button(
                        onClick = { selectedTab = tab },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .semantics { this.selected = selected },
                        enabled = enabled,
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Ink else Color.Transparent,
                            contentColor = if (selected) Paper else MutedInk,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = MutedInk.copy(alpha = 0.42f),
                        ),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                    ) {
                        Text(text = stringResource(tab.labelRes), maxLines = 1)
                    }
                }
            }
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.padding(top = 18.dp),
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(90)) },
                label = "editor controls",
            ) { tab ->
                when (tab) {
                    EditorTab.Layout -> LayoutControls(document, enabled, onAction)
                    EditorTab.Copy -> CopyControls(document, enabled, onAction)
                    EditorTab.Style -> StyleControls(document, enabled, onAction)
                }
            }
        }
    }
}

@Composable
private fun LayoutControls(
    document: EditorDocument,
    enabled: Boolean,
    onAction: (EditorAction) -> Unit,
) {
    val deviceFrameLabel = stringResource(R.string.device_frame)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(R.string.layout_section_label)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.device_frame), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.device_frame_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = document.frameEnabled,
                    onCheckedChange = { onAction(EditorAction.SetFrameEnabled(it)) },
                    modifier = Modifier.semantics { contentDescription = deviceFrameLabel },
                    enabled = enabled,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LayoutMode.entries.forEach { layout ->
                val splitUnavailable = layout == LayoutMode.Split && !document.canUseSplit
                val unavailableDescription = stringResource(R.string.split_unavailable)
                OptionButton(
                    text = when (layout) {
                        LayoutMode.Focus -> stringResource(R.string.layout_focus)
                        LayoutMode.Stack -> stringResource(R.string.layout_stack)
                        LayoutMode.Split -> stringResource(R.string.layout_split)
                    },
                    selected = document.layout == layout,
                    enabled = enabled && !splitUnavailable,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            if (splitUnavailable) stateDescription = unavailableDescription
                        },
                    onClick = { onAction(EditorAction.SetLayout(layout)) },
                )
            }
        }
        Text(
            text = stringResource(R.string.layout_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CopyControls(
    document: EditorDocument,
    enabled: Boolean,
    onAction: (EditorAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(R.string.copy_section_label)
        OutlinedTextField(
            value = document.title,
            onValueChange = { onAction(EditorAction.SetTitle(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("title-field"),
            enabled = enabled,
            label = { Text(stringResource(R.string.title_label)) },
            supportingText = {
                Text(stringResource(R.string.character_counter, document.title.codePointLength(), 60))
            },
            maxLines = 2,
            shape = RoundedCornerShape(16.dp),
        )
        OutlinedTextField(
            value = document.subtitle,
            onValueChange = { onAction(EditorAction.SetSubtitle(it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text(stringResource(R.string.subtitle_label)) },
            supportingText = {
                Text(stringResource(R.string.character_counter, document.subtitle.codePointLength(), 100))
            },
            maxLines = 2,
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@Composable
private fun StyleControls(
    document: EditorDocument,
    enabled: Boolean,
    onAction: (EditorAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(R.string.palette_section_label)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PaletteId.entries.forEach { palette ->
                    PaletteButton(
                        palette = palette,
                        selected = palette == document.palette,
                        enabled = enabled,
                        onClick = { onAction(EditorAction.SetPalette(palette)) },
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(R.string.shadow_section_label)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShadowLevel.entries.forEach { shadow ->
                    OptionButton(
                        text = when (shadow) {
                            ShadowLevel.Soft -> stringResource(R.string.shadow_soft)
                            ShadowLevel.Medium -> stringResource(R.string.shadow_medium)
                            ShadowLevel.Strong -> stringResource(R.string.shadow_strong)
                        },
                        selected = document.shadow == shadow,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        onClick = { onAction(EditorAction.SetShadow(shadow)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteButton(
    palette: PaletteId,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = palette.colors()
    val outlineWidth = if (selected) 2.dp else 1.dp
    val outlineColor = if (selected) Cobalt else Outline
    val containerColor = if (selected) SelectedWash else ElevatedPaper
    val name = when (palette) {
        PaletteId.Ink -> stringResource(R.string.palette_ink)
        PaletteId.Cobalt -> stringResource(R.string.palette_cobalt)
        PaletteId.Coral -> stringResource(R.string.palette_coral)
        PaletteId.Moss -> stringResource(R.string.palette_moss)
        PaletteId.Violet -> stringResource(R.string.palette_violet)
        PaletteId.Sunrise -> stringResource(R.string.palette_sunrise)
    }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(76.dp)
            .semantics { this.selected = selected },
        enabled = enabled,
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = outlineWidth,
            color = outlineColor,
        ),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(colors.startColor), Color(colors.endColor)),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OptionButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val outlineWidth = if (selected) 2.dp else 1.dp
    val outlineColor = if (selected) Cobalt else Outline
    val containerColor = if (selected) SelectedWash else ElevatedPaper
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { this.selected = selected },
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            width = outlineWidth,
            color = outlineColor,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Ink,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
    ) {
        Text(text = text, maxLines = 1)
    }
}

@Composable
private fun SectionLabel(textRes: Int) {
    Text(
        text = stringResource(textRes),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelSmall,
    )
}
