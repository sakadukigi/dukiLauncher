package com.dukilab.launcher.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.dukilab.launcher.ui.theme.DukiTheme

/** dukiLab. の文字。色を省くと ink。 */
@Composable
fun DukiText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = DukiTheme.colors.ink,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = if (textAlign != null) style.copy(color = color, textAlign = textAlign) else style.copy(color = color),
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}
