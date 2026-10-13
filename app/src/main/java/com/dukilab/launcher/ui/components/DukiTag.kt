package com.dukilab.launcher.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dukilab.launcher.ui.theme.DukiTheme

/** 分類と通し番号の控えめなタグ（等幅11px・大文字・ink-muted、枠や塗りなし）。 */
@Composable
fun DukiTag(text: String, modifier: Modifier = Modifier, color: Color = DukiTheme.colors.inkMuted) {
    DukiText(text = text.uppercase(), style = DukiTheme.type.tag, color = color, modifier = modifier, maxLines = 1)
}
