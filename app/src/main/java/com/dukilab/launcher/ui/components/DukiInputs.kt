package com.dukilab.launcher.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dukilab.launcher.ui.theme.DukiDimens
import com.dukilab.launcher.ui.theme.DukiTheme

/**
 * 1行の入力欄。一段沈んだ面（surface-sunken）に、必ず見える2pxの枠（line-strong）。
 * 左上に cut-sm の三角。
 */
@Composable
fun DukiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
) {
    val c = DukiTheme.colors
    val t = DukiTheme.type
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = t.body.copy(color = c.ink),
        cursorBrush = SolidColor(c.ink),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = modifier.semantics { contentDescription = placeholder },
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surfaceSunken)
                    .border(DukiDimens.borderStrong, c.lineStrong)
                    .cutCorner(DukiDimens.cutSm, c.cornerMark)
                    .padding(horizontal = DukiDimens.space4, vertical = DukiDimens.space2),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) DukiText(placeholder, t.body, color = c.inkMuted, maxLines = 1)
                inner()
            }
        },
    )
}

/**
 * 選択肢を横に並べたもの。選ばれているものは反転面（塗り）、ほかは枠だけ。
 */
@Composable
fun <T> DukiChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DukiTheme.colors
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DukiDimens.space2)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .background(if (on) c.inverseSurface else c.surface)
                    .border(DukiDimens.borderStrong, if (on) c.inverseSurface else c.line)
                    .cutCorner(DukiDimens.cutSm, if (on) c.surface else c.cornerMark)
                    .semantics { this.selected = on }
                    .clickable(role = Role.RadioButton) { onSelect(value) }
                    .padding(horizontal = DukiDimens.space2, vertical = DukiDimens.space2),
                contentAlignment = Alignment.Center,
            ) {
                DukiText(
                    text = label,
                    style = DukiTheme.type.bodySm.copy(fontWeight = DukiTheme.type.button.fontWeight),
                    color = if (on) c.inverseInk else c.ink,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * オン／オフの切り替え。直角のスイッチと「オン」「オフ」の文字で、いまの状態を示す。
 */
@Composable
fun DukiToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    val c = DukiTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = if (checked) "オン" else "オフ" }
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = DukiDimens.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            DukiText(title, DukiTheme.type.body)
            if (note != null) DukiText(note, DukiTheme.type.caption, color = c.inkMuted)
        }
        Spacer(Modifier.width(DukiDimens.space3))
        DukiText(
            if (checked) "オン" else "オフ",
            DukiTheme.type.tag,
            color = if (checked) c.ink else c.inkMuted,
        )
        Spacer(Modifier.width(DukiDimens.space2))
        DukiSwitch(checked)
    }
}

/**
 * 直角のスイッチ。オンは枠の中が ink で塗られ、つまみが右に寄る。
 * オフは中が空で、つまみが左（薄い色）。
 */
@Composable
private fun DukiSwitch(checked: Boolean) {
    val c = DukiTheme.colors
    val knobOffset by animateDpAsState(if (checked) 20.dp else 0.dp, label = "knob")
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 24.dp)
            .border(DukiDimens.borderStrong, c.lineStrong)
            .background(if (checked) c.inverseSurface else c.surface)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(16.dp)
                .background(if (checked) c.inverseInk else c.inkMuted),
        )
    }
}

/** 区切りの見出し（タグ＋見出し）。 */
@Composable
fun SectionHeading(tag: String?, title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (tag != null) {
            DukiTag(tag)
            Spacer(Modifier.height(DukiDimens.space1))
        }
        DukiText(title, DukiTheme.type.h3)
    }
}
