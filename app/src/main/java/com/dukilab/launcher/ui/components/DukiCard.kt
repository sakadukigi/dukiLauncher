package com.dukilab.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dukilab.launcher.ui.theme.DukiDimens
import com.dukilab.launcher.ui.theme.DukiTheme

/**
 * 基本の入れ物（Card）。直角の面の左上に cut-md の三角を置く。
 * [inverse] にすると反転面になり、三角は地の色で抜ける。影・角丸は付けない。
 */
@Composable
fun DukiCard(
    modifier: Modifier = Modifier,
    inverse: Boolean = false,
    cut: Dp = DukiDimens.cutMd,
    contentPadding: PaddingValues = PaddingValues(
        start = DukiDimens.space5,
        top = DukiDimens.space4,
        end = DukiDimens.space5,
        bottom = DukiDimens.space5,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = DukiTheme.colors
    val bg = if (inverse) c.inverseSurface else c.surfaceRaised
    val border = if (inverse) c.inverseSurface else c.line
    val mark = if (inverse) c.surface else c.cornerMark
    Column(
        modifier = modifier
            .background(bg)
            .border(DukiDimens.borderHair, border)
            .cutCorner(cut, mark)
            .padding(contentPadding),
        content = content,
    )
}
