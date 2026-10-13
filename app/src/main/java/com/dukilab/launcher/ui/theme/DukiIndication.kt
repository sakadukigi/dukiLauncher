package com.dukilab.launcher.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/**
 * 押したときの表示。角丸の波紋は使わず、面を薄いグレーで覆い、
 * 左上に45°の三角を出す（dukiLab. のボタンの動きを簡略にしたもの）。
 */
object DukiIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        DukiIndicationNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = 0x6475
}

private class DukiIndicationNode(
    private val source: InteractionSource,
) : Modifier.Node(), DrawModifierNode {
    private var pressed = false

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                val next = when (interaction) {
                    is PressInteraction.Press -> true
                    is PressInteraction.Release, is PressInteraction.Cancel -> false
                    else -> pressed
                }
                if (next != pressed) {
                    pressed = next
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!pressed) return
        drawRect(PressOverlay)
        val s = minOf(size.minDimension, 12f * density)
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(s, 0f)
            lineTo(0f, s)
            close()
        }
        drawPath(path, PressMark)
    }

    private companion object {
        val PressOverlay = Color(0x2E7D8892)
        val PressMark = Color(0xFF7D8892)
    }
}
