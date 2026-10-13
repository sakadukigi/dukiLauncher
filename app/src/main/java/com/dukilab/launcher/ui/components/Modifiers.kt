package com.dukilab.launcher.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import com.dukilab.launcher.ui.theme.DukiDimens

/** 左上に一辺 [sizePx] の直角二等辺三角形を描く。 */
fun DrawScope.drawCornerMark(sizePx: Float, color: Color, blendMode: BlendMode = DrawScope.DefaultBlendMode) {
    if (sizePx <= 0f) return
    val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(sizePx, 0f)
        lineTo(0f, sizePx)
        close()
    }
    drawPath(path, color, blendMode = blendMode)
}

/**
 * dukiLab. の目印、左上の45°三角マーク（corner-mark）を中身の上に重ねる。
 * 塗り面の上では [color] に地の色（surface）を渡して「抜く」。
 */
fun Modifier.cutCorner(size: Dp, color: Color): Modifier = drawWithContent {
    drawContent()
    drawCornerMark(size.toPx(), color)
}

/**
 * 方眼の地（GridSurface）。[surface] で塗り、[grid] 色で [cell] ごとに線を引く。
 * 情報を伝える線には使わない。
 */
fun Modifier.gridBackground(surface: Color, grid: Color, cell: Dp = DukiDimens.gridCell): Modifier =
    drawBehind {
        drawRect(surface)
        val step = cell.toPx()
        if (step <= 0f) return@drawBehind
        val stroke = 1f
        var x = 0f
        while (x <= size.width) {
            drawLine(grid, Offset(x, 0f), Offset(x, size.height), stroke)
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(grid, Offset(0f, y), Offset(size.width, y), stroke)
            y += step
        }
    }

/** 上辺だけに線を引く。 */
fun Modifier.topLine(color: Color, width: Dp = DukiDimens.borderHair): Modifier = drawBehind {
    drawRect(color, size = size.copy(height = width.toPx()))
}

/** 下辺だけに線を引く。 */
fun Modifier.bottomLine(color: Color, width: Dp = DukiDimens.borderHair): Modifier = drawBehind {
    val w = width.toPx()
    drawRect(color, topLeft = Offset(0f, size.height - w), size = size.copy(height = w))
}
