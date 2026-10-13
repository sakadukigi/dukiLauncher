package com.dukilab.launcher.ui.theme

import androidx.compose.ui.unit.dp

/** 寸法トークン（spacing / cut / grid / border）。角丸は使わない（radius-none）。 */
object DukiDimens {
    val space1 = 4.dp
    val space2 = 8.dp
    val space3 = 12.dp
    val space4 = 16.dp
    val space5 = 24.dp
    val space6 = 32.dp
    val space7 = 48.dp
    val space8 = 64.dp

    /** 左上の45°三角の一辺：ボタン・小さな要素。 */
    val cutSm = 8.dp

    /** 左上の45°三角の一辺：カード・パネル。 */
    val cutMd = 12.dp

    /** 左上の45°三角の一辺：大きな面。 */
    val cutLg = 32.dp

    /** 方眼の1マス。 */
    val gridCell = 16.dp

    val borderHair = 1.dp
    val borderStrong = 2.dp

    /** ヘッダーの高さ（スマホ幅）。 */
    val headerHeight = 56.dp
}
