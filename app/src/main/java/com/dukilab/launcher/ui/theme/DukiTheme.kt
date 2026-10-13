package com.dukilab.launcher.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.dukilab.launcher.core.ThemeMode

val LocalDukiColors = staticCompositionLocalOf { DukiColors.Light }
val LocalDukiTypography = staticCompositionLocalOf { DukiTypography() }

/** 画面から色と文字スタイルを取り出す入口。 */
object DukiTheme {
    val colors: DukiColors
        @Composable @ReadOnlyComposable get() = LocalDukiColors.current

    val type: DukiTypography
        @Composable @ReadOnlyComposable get() = LocalDukiTypography.current
}

/** [mode] からダークにするかを決める。 */
@Composable
fun isDarkFor(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private val typography = DukiTypography()

@Composable
fun DukiTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val colors = if (isDarkFor(mode)) DukiColors.Dark else DukiColors.Light
    CompositionLocalProvider(
        LocalDukiColors provides colors,
        LocalDukiTypography provides typography,
        LocalIndication provides DukiIndication,
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = colors.ink,
            backgroundColor = colors.ink.copy(alpha = 0.25f),
        ),
        content = content,
    )
}
