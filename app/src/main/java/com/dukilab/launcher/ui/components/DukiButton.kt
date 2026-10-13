package com.dukilab.launcher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.dukilab.launcher.ui.theme.DukiDimens
import com.dukilab.launcher.ui.theme.DukiTheme

enum class DukiButtonStyle {
    /** 主ボタン：反転面の塗り。1画面に1つまで。 */
    Primary,

    /** 副ボタン：地色の面に line-strong の枠。 */
    Outline,
}

private val PressEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val ReleaseEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * 直角・太字のボタン（Button）。左上に cut-sm の三角が付く。
 *
 * 押すと三角が45°の斜めのまま右下へ広がってボタン全体を覆い、白黒が反転する（0.24秒）。
 * 離すと0.32秒で左上へ戻る。三角は白の「差の絶対値」合成で描くので、
 * 主ボタンでは地の色で抜け、副ボタンでは ink 色になる。
 */
@Composable
fun DukiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DukiButtonStyle = DukiButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val c = DukiTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val wipe = remember { Animatable(0f) }
    LaunchedEffect(pressed) {
        if (pressed) wipe.animateTo(1f, tween(240, easing = PressEasing))
        else wipe.animateTo(0f, tween(320, easing = ReleaseEasing))
    }

    val bg: Color
    val fg: Color
    val border: Color
    when {
        !enabled -> {
            bg = c.surfaceSunken; fg = c.inkMuted; border = c.line
        }
        style == DukiButtonStyle.Primary -> {
            bg = c.inverseSurface; fg = c.inverseInk; border = c.inverseSurface
        }
        else -> {
            bg = c.surface; fg = c.ink; border = c.lineStrong
        }
    }
    val disabledMark = c.line

    Box(
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .background(bg)
            .border(DukiDimens.borderStrong, border)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .drawWithContent {
                drawContent()
                val rest = DukiDimens.cutSm.toPx()
                val full = size.width + size.height
                val s = rest + (full - rest) * wipe.value
                if (enabled) drawCornerMark(s, Color.White, BlendMode.Difference)
                else drawCornerMark(rest, disabledMark)
            }
            .padding(horizontal = DukiDimens.space5, vertical = DukiDimens.space3),
        contentAlignment = Alignment.Center,
    ) {
        DukiText(text = text, style = DukiTheme.type.button, color = fg, maxLines = 1)
    }
}
