package com.dukilab.launcher.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dukilab.launcher.R

/** 可変ウェイトのフォントから、使うウェイトだけを取り出す。 */
@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, vararg weights: Int): FontFamily = FontFamily(
    weights.map { w ->
        Font(
            resId = resId,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    },
)

/** 本文・見出し：M PLUS 2。 */
val MPlus2: FontFamily = variable(R.font.mplus2, 400, 500, 700, 800)

/** 等幅：M PLUS 1 Code。 */
val MPlus1Code: FontFamily = variable(R.font.mplus1code, 400, 500, 700)

/** ロゴ用：dukiLab Display（ロゴ・サービス名専用）。 */
val DukiLabDisplay: FontFamily = FontFamily(Font(R.font.dukilab_display, FontWeight.Normal))

/** dukiLab. の文字スタイル（tokens.json の type.groups と同じ名前・値）。 */
@Immutable
data class DukiTypography(
    val logotypeLg: TextStyle = logo(96),
    val logotype: TextStyle = logo(48),
    val logotypeSm: TextStyle = logo(24),
    val display: TextStyle = sans(56, 60, 800).copy(letterSpacing = (-0.01).em),
    val h1: TextStyle = sans(36, 44, 700),
    val h2: TextStyle = sans(28, 36, 700),
    val h3: TextStyle = sans(20, 28, 700),
    val bodyLg: TextStyle = sans(18, 30, 400),
    val body: TextStyle = sans(16, 28, 400),
    val bodySm: TextStyle = sans(14, 22, 400),
    val caption: TextStyle = sans(12, 18, 400),
    val tag: TextStyle = mono(11, 14, 500).copy(letterSpacing = 0.06.em),
    val spec: TextStyle = mono(14, 20, 500),
    val code: TextStyle = mono(14, 22, 400),
    /** ボタンの文字（bundle.css の .dl-btn）。 */
    val button: TextStyle = sans(15, 20, 700),
    /** ホーム画面のアプリ名。caption を詰めたもの。 */
    val appLabel: TextStyle = sans(12, 16, 500),
)

private fun sans(size: Int, line: Int, weight: Int) = TextStyle(
    fontFamily = MPlus2,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = FontWeight(weight),
)

private fun mono(size: Int, line: Int, weight: Int) = TextStyle(
    fontFamily = MPlus1Code,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = FontWeight(weight),
)

private fun logo(size: Int) = TextStyle(
    fontFamily = DukiLabDisplay,
    fontSize = size.sp,
    lineHeight = size.sp,
    fontWeight = FontWeight.Normal,
)
