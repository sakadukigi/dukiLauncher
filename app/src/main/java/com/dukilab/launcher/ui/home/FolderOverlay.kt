package com.dukilab.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dukilab.launcher.core.AppEntry
import com.dukilab.launcher.core.HomeItem
import com.dukilab.launcher.core.Tags
import com.dukilab.launcher.ui.components.AppIcon
import com.dukilab.launcher.ui.components.DukiCard
import com.dukilab.launcher.ui.components.DukiTag
import com.dukilab.launcher.ui.components.DukiText
import com.dukilab.launcher.ui.components.bottomLine
import com.dukilab.launcher.ui.components.itemGestures
import com.dukilab.launcher.ui.theme.DukiDimens
import com.dukilab.launcher.ui.theme.DukiTheme
import kotlin.math.roundToInt

/** フォルダの中で、アプリを長押しして動かしている途中の状態。座標は画面全体（ルート）基準。 */
private class FolderDrag {
    var from by mutableStateOf<Int?>(null)
    var start by mutableStateOf(Offset.Zero)
    var pointer by mutableStateOf(Offset.Zero)

    fun begin(index: Int, at: Offset) {
        from = index
        start = at
        pointer = at
    }
}

private const val FOLDER_COLUMNS = 4
private val FolderIconSize = 48.dp

/**
 * 開いたフォルダ。中央のカードに名前（その場で書き換えられる）とアプリを並べる。
 *
 * - アプリを押すと開く
 * - 長押ししてそのまま動かすと並べ替え。カードの外で離すとフォルダから出す
 * - 長押しして動かさずに離すとメニュー
 * - カードの外を押すと閉じる
 */
@Composable
fun FolderOverlay(
    folder: HomeItem.Folder,
    apps: (String) -> AppEntry?,
    editName: Boolean,
    onRename: (String) -> Unit,
    onLaunch: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    onTakeOut: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = DukiTheme.colors
    var name by remember(folder.id) { mutableStateOf(folder.name) }
    val focus = remember { FocusRequester() }
    val rename by rememberUpdatedState(onRename)
    val drag = remember(folder.id) { FolderDrag() }
    val slots = remember(folder.id) { mutableStateMapOf<Int, Rect>() }
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val slop = with(density) { 12.dp.toPx() }
    val keys = folder.appKeys

    LaunchedEffect(editName) {
        if (editName) runCatching { focus.requestFocus() }
    }

    fun commit() {
        if (name.trim() != folder.name) rename(name)
    }

    // 指の位置から、カードの外か・一番近いマスはどこかを求める（離した瞬間も最新の位置で判断する）。
    fun isOutside(): Boolean = !cardBounds.contains(drag.pointer)
    fun hoverSlot(from: Int): Int =
        if (isOutside()) from
        else slots.filterKeys { it in keys.indices }.minByOrNull { (it.value.center - drag.pointer).getDistanceSquared() }?.key ?: from

    val from = drag.from?.takeIf { it in keys.indices }
    val dragging = from != null
    val outside = from != null && isOutside()
    val hover = from?.let(::hoverSlot)
    // 動かしている途中は、離したらこうなるという並びで見せる。
    val order = keys.indices.toMutableList().apply {
        if (from != null && hover != null && hover != from) add(hover, removeAt(from))
    }

    fun finishDrag() {
        val index = drag.from ?: return
        drag.from = null
        val key = keys.getOrNull(index) ?: return
        val moved = (drag.pointer - drag.start).getDistance() > slop
        val target = hoverSlot(index)
        when {
            !moved -> onLongPress(key)
            isOutside() -> onTakeOut(key)
            target != index -> onReorder(index, target)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                detectTapGestures {
                    commit()
                    onDismiss()
                }
            }
            .testTag("folder"),
        contentAlignment = Alignment.Center,
    ) {
        DukiCard(
            modifier = Modifier
                .padding(DukiDimens.space5)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .onGloballyPositioned { cardBounds = it.boundsInRoot() }
                // カードの中を押しても閉じないようにする。
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            DukiTag(Tags.of("folder", keys.size))
            Spacer(Modifier.height(DukiDimens.space1))
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = DukiTheme.type.h3.copy(color = c.ink),
                cursorBrush = SolidColor(c.ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .onFocusChanged { if (!it.isFocused) commit() }
                    .bottomLine(c.lineStrong)
                    .padding(vertical = DukiDimens.space1)
                    .semantics { contentDescription = "フォルダ名" },
            )
            Spacer(Modifier.height(DukiDimens.space4))
            order.chunked(FOLDER_COLUMNS).forEachIndexed { row, rowIndices ->
                Row(Modifier.fillMaxWidth()) {
                    rowIndices.forEachIndexed { col, itemIndex ->
                        val slot = row * FOLDER_COLUMNS + col
                        FolderApp(
                            slot = slot,
                            folderId = folder.id,
                            app = apps(keys[itemIndex]),
                            dimmed = dragging && itemIndex == from,
                            highlighted = dragging && !outside && slot == hover && hover != from,
                            onTap = { onLaunch(keys[itemIndex]) },
                            onLongPress = { at -> drag.begin(slot, at) },
                            onDrag = { drag.pointer += it },
                            onRelease = ::finishDrag,
                            onCancel = { drag.from = null },
                            onPlaced = { slots[slot] = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(FOLDER_COLUMNS - rowIndices.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (dragging) {
                Spacer(Modifier.height(DukiDimens.space2))
                DukiTag(if (outside) "離すとフォルダから出します" else "離すとここへ動かします")
            }
        }

        // 指に付いて動くアイコン。
        if (dragging) {
            val half = with(density) { (FolderIconSize / 2).toPx() }
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset((drag.pointer.x - half).roundToInt(), (drag.pointer.y - half * 2).roundToInt()) }
                    .size(FolderIconSize)
                    .border(DukiDimens.borderStrong, c.ink),
            ) {
                AppIcon(apps(keys[from!!]), FolderIconSize)
            }
        }
    }
}

@Composable
private fun FolderApp(
    slot: Int,
    folderId: String,
    app: AppEntry?,
    dimmed: Boolean,
    highlighted: Boolean,
    onTap: () -> Unit,
    onLongPress: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onRelease: () -> Unit,
    onCancel: () -> Unit,
    onPlaced: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DukiTheme.colors
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val tap by rememberUpdatedState(onTap)
    val long by rememberUpdatedState(onLongPress)
    val dragDelta by rememberUpdatedState(onDrag)
    val release by rememberUpdatedState(onRelease)
    val cancel by rememberUpdatedState(onCancel)
    Column(
        modifier = modifier
            .onGloballyPositioned {
                coords = it
                onPlaced(it.boundsInRoot())
            }
            .then(if (highlighted) Modifier.border(DukiDimens.borderStrong, c.lineStrong) else Modifier)
            // マスごとに固定のキーにする（並びが変わっても、動かしている途中の操作が切れないように）。
            .itemGestures(
                key = folderId to slot,
                onTap = { tap() },
                onLongPress = { local -> long(coords?.localToRoot(local) ?: local) },
                onDrag = { dragDelta(it) },
                onRelease = { release() },
                onCancel = { cancel() },
            )
            .semantics { contentDescription = app?.label ?: "" }
            .padding(vertical = DukiDimens.space2)
            .alpha(if (dimmed) 0.25f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(app, FolderIconSize)
        Spacer(Modifier.height(DukiDimens.space1))
        DukiText(app?.label.orEmpty(), DukiTheme.type.appLabel, maxLines = 1)
    }
}
