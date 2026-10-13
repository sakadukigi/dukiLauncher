package com.dukilab.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.dukilab.launcher.R
import com.dukilab.launcher.ui.theme.DukiDimens
import com.dukilab.launcher.ui.theme.DukiTheme

/** ロゴ面の形：左端は45°の右上がり（／）、右端は垂直。 */
private object LogoFaceShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(size.height, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

/** dukiLab. のワードマーク。色は既定で logo-ink。 */
@Composable
fun DukiLogo(modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = DukiTheme.colors.logoInk) {
    Image(
        painter = painterResource(R.drawable.dukilab_logo),
        contentDescription = "dukiLab.",
        colorFilter = ColorFilter.tint(color),
        modifier = modifier,
    )
}

/**
 * ヘッダー（Header）。左から「黒 ▶ 右上がりの斜め境界 ▶ 白いロゴ面 ▶ 垂直の境界 ▶ 黒い帯」。
 * 色はテーマに関係なく固定。斜め境界とロゴのすき間は12px（左の余白＝高さ÷2＋12）。
 * 黒い帯の右側には [content] を置く。
 */
@Composable
fun DukiHeader(
    modifier: Modifier = Modifier,
    height: Dp = DukiDimens.headerHeight,
    logoHeight: Dp = 22.dp,
    content: @Composable RowScope.() -> Unit = {},
) {
    val c = DukiTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(c.headerBar)
            .padding(start = DukiDimens.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clip(LogoFaceShape)
                .background(c.headerLogoBg)
                .padding(start = height / 2 + 12.dp, end = DukiDimens.space4),
            contentAlignment = Alignment.CenterStart,
        ) {
            DukiLogo(modifier = Modifier.height(logoHeight), color = DukiColorsFixed.LogoBlack)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = DukiDimens.space4),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** テーマに関係なく固定の色。 */
object DukiColorsFixed {
    /** ヘッダーの白いロゴ面に置く黒ロゴ。 */
    val LogoBlack = androidx.compose.ui.graphics.Color(0xFF000000)
}

/** ヘッダーの帯の上に置く文字の色。 */
@Composable
fun headerInk() = DukiTheme.colors.headerBarInk

