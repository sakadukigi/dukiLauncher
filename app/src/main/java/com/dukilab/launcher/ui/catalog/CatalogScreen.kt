package com.dukilab.launcher.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.dukilab.launcher.core.Tags
import com.dukilab.launcher.ui.components.DukiButton
import com.dukilab.launcher.ui.components.DukiButtonStyle
import com.dukilab.launcher.ui.components.DukiCard
import com.dukilab.launcher.ui.components.DukiChoiceRow
import com.dukilab.launcher.ui.components.DukiHeader
import com.dukilab.launcher.ui.components.DukiLogo
import com.dukilab.launcher.ui.components.DukiTag
import com.dukilab.launcher.ui.components.DukiText
import com.dukilab.launcher.ui.components.DukiTextField
import com.dukilab.launcher.ui.components.DukiToggleRow
import com.dukilab.launcher.ui.settings.SubScreen
import com.dukilab.launcher.ui.theme.DukiDimens
import com.dukilab.launcher.ui.theme.DukiTheme

/**
 * デザイン部品の一覧（M1 の確認用）。dukiLab. のトークンと部品が
 * デザインシステムどおりに見えるかを、実機で並べて確かめる。
 */
@Composable
fun CatalogScreen(onBack: () -> Unit) {
    val c = DukiTheme.colors
    val t = DukiTheme.type
    SubScreen(title = "デザイン部品", onBack = onBack, testTag = "catalog") {
        DukiCard(Modifier.fillMaxWidth()) {
            DukiTag(Tags.of("logo", 1))
            Spacer(Modifier.height(DukiDimens.space3))
            DukiLogo(Modifier.height(36.dp))
            Spacer(Modifier.height(DukiDimens.space3))
            DukiText("dukiLauncher.", t.logotypeSm, color = c.logoInk)
        }

        Column {
            DukiTag(Tags.of("header", 2))
            Spacer(Modifier.height(DukiDimens.space2))
            DukiHeader {
                Spacer(Modifier.weight(1f))
                DukiText("MENU", t.tag, color = c.headerBarInk)
            }
        }

        DukiCard(Modifier.fillMaxWidth()) {
            DukiTag(Tags.of("color", 3))
            DukiText("色", t.h3)
            Spacer(Modifier.height(DukiDimens.space2))
            listOf(
                "surface" to c.surface,
                "surface-raised" to c.surfaceRaised,
                "surface-sunken" to c.surfaceSunken,
                "grid" to c.grid,
                "line" to c.line,
                "line-strong" to c.lineStrong,
                "ink" to c.ink,
                "ink-muted" to c.inkMuted,
                "inverse-surface" to c.inverseSurface,
                "inverse-ink" to c.inverseInk,
                "logo-ink" to c.logoInk,
                "header-bar" to c.headerBar,
            ).forEach { (name, color) -> Swatch(name, color) }
        }

        DukiCard(Modifier.fillMaxWidth()) {
            DukiTag(Tags.of("type", 4))
            DukiText("書体", t.h3)
            Spacer(Modifier.height(DukiDimens.space2))
            TypeSample("display", t.display.copy(fontSize = t.h1.fontSize, lineHeight = t.h1.lineHeight), "ラボの記録")
            TypeSample("h1", t.h1, "ホームラボ構成")
            TypeSample("h2", t.h2, "ネットワーク設計")
            TypeSample("h3", t.h3, "VLANの切り方")
            TypeSample("body", t.body, "設定ファイルは下にまとめてあります。")
            TypeSample("body-sm", t.bodySm, "※ 環境によって手順が異なります。")
            TypeSample("caption", t.caption, "2026.10.13 更新")
            TypeSample("spec", t.spec, "CPU 8C/16T · RAM 64GB")
            TypeSample("code", t.code, "pvecm add 192.168.10.11")
        }

        DukiCard(Modifier.fillMaxWidth()) {
            DukiTag(Tags.of("button", 5))
            DukiText("ボタン", t.h3)
            Spacer(Modifier.height(DukiDimens.space3))
            Row(horizontalArrangement = Arrangement.spacedBy(DukiDimens.space3)) {
                DukiButton("ダウンロードする", onClick = {})
                DukiButton("詳しく見る", onClick = {}, style = DukiButtonStyle.Outline)
            }
            Spacer(Modifier.height(DukiDimens.space3))
            DukiButton("押せません", onClick = {}, enabled = false)
        }

        DukiCard(inverse = true, modifier = Modifier.fillMaxWidth()) {
            DukiTag(Tags.of("card", 6), color = c.inverseInk)
            DukiText("反転カード", t.h3, color = c.inverseInk)
            DukiText("強調したい面は反転面で組み、三角は地の色で抜きます。", t.bodySm, color = c.inverseInk)
        }

        DukiCard(Modifier.fillMaxWidth()) {
            DukiTag(Tags.of("input", 7))
            DukiText("入力", t.h3)
            Spacer(Modifier.height(DukiDimens.space3))
            var text by remember { mutableStateOf("") }
            DukiTextField(text, { text = it }, placeholder = "アプリを検索", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(DukiDimens.space3))
            var choice by remember { mutableStateOf(0) }
            DukiChoiceRow(listOf(0 to "左", 1 to "中", 2 to "右"), choice, { choice = it })
            Spacer(Modifier.height(DukiDimens.space2))
            var on by remember { mutableStateOf(true) }
            DukiToggleRow("切り替え", on, { on = it }, note = "直角のスイッチと文字で示します。")
        }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    val c = DukiTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = DukiDimens.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .background(color)
                .border(DukiDimens.borderHair, c.line),
        )
        Spacer(Modifier.width(DukiDimens.space3))
        DukiText(name, DukiTheme.type.code, modifier = Modifier.weight(1f))
        DukiText(hex(color), DukiTheme.type.code, color = c.inkMuted)
    }
}

@Composable
private fun TypeSample(name: String, style: TextStyle, sample: String) {
    Column(Modifier.padding(vertical = DukiDimens.space1)) {
        DukiTag(name)
        DukiText(sample, style)
    }
}

private fun hex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)
