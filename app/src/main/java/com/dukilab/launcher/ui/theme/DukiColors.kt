package com.dukilab.launcher.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * dukiLab. の色トークン（デザインシステム tokens.json と同じ名前・値）。
 * モノクロだけで組み、アクセントカラーや状態色は持たない。
 */
@Immutable
data class DukiColors(
    /** ページの地色。 */
    val surface: Color,
    /** カード・パネルなど一段上の面。 */
    val surfaceRaised: Color,
    /** 入力欄など一段沈んだ面。 */
    val surfaceSunken: Color,
    /** 方眼の線。装飾専用。 */
    val grid: Color,
    /** カード枠・区切り線。 */
    val line: Color,
    /** 入力欄・アウトラインボタンの枠。 */
    val lineStrong: Color,
    /** 本文・見出し。 */
    val ink: Color,
    /** 補足・日付。 */
    val inkMuted: Color,
    /** ロゴ専用の純黒／純白。 */
    val logoInk: Color,
    /** ヘッダーの黒い帯（テーマに関係なく黒）。 */
    val headerBar: Color,
    /** ヘッダーの帯の上の文字（常に白）。 */
    val headerBarInk: Color,
    /** ヘッダーのロゴ面（常に白）。 */
    val headerLogoBg: Color,
    val isDark: Boolean,
) {
    /** 反転面（塗りボタン・強調帯）。 */
    val inverseSurface: Color get() = ink

    /** 反転面の上の文字。 */
    val inverseInk: Color get() = surface

    /** 左上の45°三角マーク。 */
    val cornerMark: Color get() = ink

    /** フォーカスの枠。 */
    val focusRing: Color get() = ink

    companion object {
        val Light = DukiColors(
            surface = Color(0xFFF6F8F9),
            surfaceRaised = Color(0xFFFFFFFF),
            surfaceSunken = Color(0xFFE9EDF0),
            grid = Color(0xFFE1E6EA),
            line = Color(0xFFC5CED6),
            lineStrong = Color(0xFF6B7680),
            ink = Color(0xFF0B0E11),
            inkMuted = Color(0xFF525D67),
            logoInk = Color(0xFF000000),
            headerBar = Color(0xFF0B0E11),
            headerBarInk = Color(0xFFEEF2F5),
            headerLogoBg = Color(0xFFFFFFFF),
            isDark = false,
        )

        val Dark = DukiColors(
            surface = Color(0xFF0D1013),
            surfaceRaised = Color(0xFF161A1E),
            surfaceSunken = Color(0xFF1C2126),
            grid = Color(0xFF1B2026),
            line = Color(0xFF333B43),
            lineStrong = Color(0xFF7D8892),
            ink = Color(0xFFEEF2F5),
            inkMuted = Color(0xFF9CA7B1),
            logoInk = Color(0xFFFFFFFF),
            headerBar = Color(0xFF0B0E11),
            headerBarInk = Color(0xFFEEF2F5),
            headerLogoBg = Color(0xFFFFFFFF),
            isDark = true,
        )
    }
}
