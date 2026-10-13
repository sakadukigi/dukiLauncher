package com.dukilab.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dukilab.launcher.core.AppEntry
import com.dukilab.launcher.ui.theme.DukiDimens
import com.dukilab.launcher.ui.theme.DukiTheme

/** 読み込んだアイコン。[isMask] なら形だけを使い、色は ink で塗る。 */
@Immutable
class AppIconImage(val bitmap: ImageBitmap, val isMask: Boolean)

/** アイコンの読み込み元。画面のテストでは差し替える。 */
fun interface IconSource {
    suspend fun icon(key: String, sizePx: Int, monochrome: Boolean): AppIconImage?
}

val LocalIconSource = staticCompositionLocalOf { IconSource { _, _, _ -> null } }

/** アイコンをモノクロで出すか（画面全体で共有）。 */
val LocalMonochromeIcons = staticCompositionLocalOf { true }

private val Grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * アプリアイコン。直角の正方形で、モノクロ表示ならテーマアイコンを ink で塗るか白黒にする。
 * 読み込めないときは頭文字を置く。
 */
@Composable
fun AppIcon(app: AppEntry?, size: Dp, modifier: Modifier = Modifier) {
    val c = DukiTheme.colors
    val mono = LocalMonochromeIcons.current
    val source = LocalIconSource.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val image by produceState<AppIconImage?>(null, app?.key, px, mono, source) {
        value = app?.let { source.icon(it.key, px, mono) }
    }
    val current = image
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            current != null && current.isMask -> Box(
                Modifier
                    .fillMaxSize()
                    .background(c.surfaceRaised)
                    .border(DukiDimens.borderHair, c.line),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = current.bitmap,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(c.ink, BlendMode.SrcIn),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            current != null -> Image(
                bitmap = current.bitmap,
                contentDescription = null,
                colorFilter = if (mono) Grayscale else null,
                modifier = Modifier.fillMaxSize(),
            )

            else -> Box(
                Modifier
                    .fillMaxSize()
                    .background(c.surfaceRaised)
                    .border(DukiDimens.borderHair, c.line),
                contentAlignment = Alignment.Center,
            ) {
                DukiText(
                    text = app?.label?.take(1)?.uppercase().orEmpty(),
                    style = DukiTheme.type.h3,
                    color = c.ink,
                    maxLines = 1,
                )
            }
        }
    }
}

/** フォルダのアイコン。中の最初の4つを2×2で並べ、左上に三角を置く。 */
@Composable
fun FolderIcon(apps: List<AppEntry?>, size: Dp, modifier: Modifier = Modifier) {
    val c = DukiTheme.colors
    val gap = 4.dp
    val inner = (size - gap * 3) / 2
    Column(
        modifier = modifier
            .size(size)
            .background(c.surfaceRaised)
            .border(DukiDimens.borderHair, c.lineStrong)
            .cutCorner(6.dp, c.cornerMark)
            .padding(gap),
    ) {
        for (row in 0 until 2) {
            Row {
                for (col in 0 until 2) {
                    Box(Modifier.size(inner), contentAlignment = Alignment.Center) {
                        val app = apps.getOrNull(row * 2 + col)
                        if (app != null) AppIcon(app, inner)
                    }
                    if (col == 0) Spacer(Modifier.size(gap))
                }
            }
            if (row == 0) Spacer(Modifier.size(gap))
        }
    }
}
