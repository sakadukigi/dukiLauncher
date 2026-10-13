package com.dukilab.launcher.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.CancellationException

private enum class PressResult { Tap, Cancel, Long }

/**
 * アイコンの操作をまとめて受け取る。
 *
 * - 短く押す：[onTap]
 * - 長押し：[onLongPress]（指の位置）。そのまま指を動かすと [onDrag]、離すと [onRelease]
 * - 長押しになる前に指が大きく動いたら何もしない（ページ送りやスワイプに任せる）
 */
fun Modifier.itemGestures(
    key: Any?,
    onTap: () -> Unit,
    onLongPress: (Offset) -> Unit,
    onDrag: (Offset) -> Unit = {},
    onRelease: () -> Unit = {},
    onCancel: () -> Unit = {},
): Modifier = pointerInput(key) {
    detectItemGestures(onTap, onLongPress, onDrag, onRelease, onCancel)
}

private suspend fun PointerInputScope.detectItemGestures(
    onTap: () -> Unit,
    onLongPress: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onRelease: () -> Unit,
    onCancel: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        val result = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var r = PressResult.Cancel
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null) {
                    r = PressResult.Cancel
                    break
                }
                if (change.changedToUp()) {
                    // 途中でスクロールなどに取られていなければ、離した時点でタップとみなす。
                    r = PressResult.Tap
                    change.consume()
                    break
                }
                if (change.isConsumed) {
                    r = PressResult.Cancel
                    break
                }
                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                    r = PressResult.Cancel
                    break
                }
            }
            r
        } ?: PressResult.Long

        when (result) {
            PressResult.Tap -> onTap()
            PressResult.Cancel -> Unit
            PressResult.Long -> {
                onLongPress(down.position)
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null) {
                            onCancel()
                            break
                        }
                        if (change.changedToUp()) {
                            change.consume()
                            onRelease()
                            break
                        }
                        val delta = change.positionChange()
                        change.consume()
                        if (delta != Offset.Zero) onDrag(delta)
                    }
                } catch (e: CancellationException) {
                    onCancel()
                    throw e
                }
            }
        }
    }
}
