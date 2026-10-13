package com.dukilab.launcher.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dukilab.launcher.core.AppEntry
import com.dukilab.launcher.core.Cell
import com.dukilab.launcher.core.ClockText
import com.dukilab.launcher.core.DoubleTapAction
import com.dukilab.launcher.core.HomeItem
import com.dukilab.launcher.core.SwipeDownAction
import com.dukilab.launcher.core.Tags
import com.dukilab.launcher.ui.LauncherActions
import com.dukilab.launcher.ui.LauncherUiState
import com.dukilab.launcher.ui.MenuTarget
import com.dukilab.launcher.ui.components.AppIcon
import com.dukilab.launcher.ui.components.DukiCard
import com.dukilab.launcher.ui.components.DukiHeader
import com.dukilab.launcher.ui.components.DukiTag
import com.dukilab.launcher.ui.components.DukiText
import com.dukilab.launcher.ui.components.FolderIcon
import com.dukilab.launcher.ui.components.cutCorner
import com.dukilab.launcher.ui.components.gridBackground
import com.dukilab.launcher.ui.components.itemGestures
import com.dukilab.launcher.ui.components.topLine
import com.dukilab.launcher.ui.theme.DukiDimens
import com.dukilab.launcher.ui.theme.DukiTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.math.roundToInt

/** 時計を進めるか（画面のテストでは止める）。 */
val LocalClockTicking = staticCompositionLocalOf { true }

/** ホーム画面のアイコンの大きさ。 */
val HomeIconSize = 52.dp

/** ドラッグ中の状態。座標は画面全体（ルート）基準。 */
private class DragState {
    var item by mutableStateOf<HomeItem?>(null)
    var from by mutableStateOf<Cell?>(null)
    var start by mutableStateOf(Offset.Zero)
    var pointer by mutableStateOf(Offset.Zero)

    val active: Boolean get() = item != null

    fun begin(item: HomeItem, from: Cell, at: Offset) {
        this.item = item
        this.from = from
        start = at
        pointer = at
    }

    fun clear() {
        item = null
        from = null
    }
}

/** ドロップ先。 */
private sealed interface DropTarget {
    data class ToCell(val cell: Cell) : DropTarget
    data object ToDock : DropTarget
}

/**
 * ホーム画面。上にヘッダー（ロゴと時計）、中央にアプリを並べる方眼のページ、下にドック。
 *
 * - アイコンを長押しして動かすと並べ替え。アプリをアプリに重ねるとフォルダ、ドックに落とすとドックへ
 * - 長押しして動かさずに離すとメニュー
 * - 上にスワイプでアプリ一覧、下にスワイプで通知パネル（設定で変更可）
 * - 何もないところをダブルタップで画面オフ（設定で有効にしたとき）
 */
@Composable
fun HomeScreen(
    state: LauncherUiState,
    actions: LauncherActions,
    homeSignal: Int,
    onPageChanged: (Int) -> Unit,
    onOpenDrawer: (search: Boolean) -> Unit,
    onOpenFolder: (String) -> Unit,
    onMenu: (MenuTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DukiTheme.colors
    val layout = state.saved.layout
    val drag = remember { DragState() }
    // ドラッグ中だけ、後ろに空のページを1枚足す（新しいページへ動かせるように）。
    val pagerState = rememberPagerState { layout.pageCount + if (drag.active) 1 else 0 }
    var lastSignal by remember { mutableStateOf(homeSignal) }
    LaunchedEffect(homeSignal) {
        if (homeSignal != lastSignal) {
            lastSignal = homeSignal
            pagerState.animateScrollToPage(0)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onPageChanged(it) }
    }
    var gridBounds by remember { mutableStateOf(Rect.Zero) }
    var dockBounds by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 72.dp.toPx() }
    val edge = with(density) { 28.dp.toPx() }
    val slop = with(density) { 12.dp.toPx() }

    fun dropTarget(): DropTarget? {
        val p = drag.pointer
        if (dockBounds.contains(p)) return DropTarget.ToDock
        if (!gridBounds.contains(p)) return null
        val col = ((p.x - gridBounds.left) / (gridBounds.width / layout.columns)).toInt().coerceIn(0, layout.columns - 1)
        val row = ((p.y - gridBounds.top) / (gridBounds.height / layout.rows)).toInt().coerceIn(0, layout.rows - 1)
        return DropTarget.ToCell(Cell(pagerState.currentPage, row * layout.columns + col))
    }

    fun finishDrag() {
        val from = drag.from
        val item = drag.item
        val target = dropTarget()
        val moved = (drag.pointer - drag.start).getDistance() > slop
        drag.clear()
        if (from == null || item == null) return
        when {
            !moved -> onMenu(
                when (item) {
                    is HomeItem.App -> MenuTarget.HomeApp(item.appKey, from)
                    is HomeItem.Folder -> MenuTarget.HomeFolder(item.id, from)
                },
            )
            target is DropTarget.ToDock -> if (item is HomeItem.App) actions.moveToDock(from)
            target is DropTarget.ToCell -> actions.moveHomeItem(from, target.cell)
        }
    }

    // ドラッグ中に指を画面の端で止めると、隣のページへ送る。
    LaunchedEffect(drag.active) {
        if (!drag.active) return@LaunchedEffect
        while (drag.active) {
            delay(450)
            val x = drag.pointer.x
            val page = pagerState.currentPage
            if (x < gridBounds.left + edge && page > 0) {
                pagerState.animateScrollToPage(page - 1)
            } else if (x > gridBounds.right - edge && page < pagerState.pageCount - 1) {
                pagerState.animateScrollToPage(page + 1)
            }
        }
    }

    val target = if (drag.active) dropTarget() else null

    Box(
        modifier = modifier
            .fillMaxSize()
            .gridBackground(c.surface, c.grid)
            .pointerInput(state.settings.doubleTap) {
                detectTapGestures(
                    onDoubleTap = {
                        if (state.settings.doubleTap == DoubleTapAction.SCREEN_OFF) actions.lockScreen()
                    },
                    onLongPress = { onMenu(MenuTarget.Background) },
                )
            }
            .pointerInput(state.settings.swipeDown) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                    onDragEnd = {
                        when {
                            total < -swipeThreshold -> onOpenDrawer(false)
                            total > swipeThreshold -> when (state.settings.swipeDown) {
                                SwipeDownAction.NOTIFICATIONS -> actions.expandNotifications()
                                SwipeDownAction.SEARCH -> onOpenDrawer(true)
                                SwipeDownAction.NONE -> Unit
                            }
                        }
                    },
                )
            }
            .testTag("home"),
    ) {
        Column(Modifier.fillMaxSize()) {
            HomeHeader(showSeconds = state.settings.showSeconds)
            PageTagRow(page = pagerState.currentPage, count = pagerState.pageCount)
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = maxOf(1, pagerState.pageCount),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = DukiDimens.space2)
                    .onGloballyPositioned { gridBounds = it.boundsInRoot() },
                key = { it },
            ) { page ->
                HomePage(
                    page = page,
                    state = state,
                    drag = drag,
                    dropCell = (target as? DropTarget.ToCell)?.cell,
                    onOpen = { cell, item ->
                        when (item) {
                            is HomeItem.App -> actions.launch(item.appKey)
                            is HomeItem.Folder -> onOpenFolder(item.id)
                        }
                    },
                    onDragDelta = { drag.pointer += it },
                    onDragEnd = ::finishDrag,
                )
            }
            if (state.loaded && layout.items.isEmpty() && !drag.active) {
                EmptyHint(Modifier.padding(horizontal = DukiDimens.space4, vertical = DukiDimens.space2))
            }
            PageIndicator(page = pagerState.currentPage, count = pagerState.pageCount)
            DockBar(
                state = state,
                highlighted = target is DropTarget.ToDock,
                onLaunch = actions::launch,
                onMenu = { key, index -> onMenu(MenuTarget.DockApp(key, index)) },
                modifier = Modifier.onGloballyPositioned { dockBounds = it.boundsInRoot() },
            )
        }

        // 指に付いて動くアイコン。
        val dragged = drag.item
        if (dragged != null) {
            val half = with(density) { (HomeIconSize / 2).toPx() }
            Box(
                Modifier
                    .offset { IntOffset((drag.pointer.x - half).roundToInt(), (drag.pointer.y - half * 2).roundToInt()) }
                    .size(HomeIconSize)
                    .border(DukiDimens.borderStrong, c.ink),
            ) {
                HomeItemIcon(dragged, state)
            }
        }
    }
}

@Composable
private fun HomeHeader(showSeconds: Boolean) {
    val c = DukiTheme.colors
    val context = LocalContext.current
    val ticking = LocalClockTicking.current
    val now by produceState(LocalDateTime.now(), ticking, showSeconds) {
        while (ticking) {
            value = LocalDateTime.now()
            // 次の秒（秒を出さないときは次の分）の変わり目まで待つ。
            val toNextSecond = 1000L - value.nano / 1_000_000L
            delay(if (showSeconds) toNextSecond + 20 else (59 - value.second) * 1000L + toNextSecond + 50)
        }
    }
    val is24 = DateFormat.is24HourFormat(context)
    Column(Modifier.background(c.headerBar)) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        DukiHeader {
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    DukiText(
                        text = ClockText.time(now, is24, seconds = showSeconds),
                        style = DukiTheme.type.spec.copy(fontSize = DukiTheme.type.h3.fontSize, lineHeight = DukiTheme.type.h3.lineHeight),
                        color = c.headerBarInk,
                        modifier = Modifier.semantics { contentDescription = "時刻" },
                    )
                    if (!is24) {
                        Spacer(Modifier.width(DukiDimens.space1))
                        DukiTag(ClockText.amPm(now), color = c.headerBarInk)
                    }
                }
                DukiTag(ClockText.dateLine(now), color = c.headerBarInk)
            }
        }
    }
}

@Composable
private fun PageTagRow(page: Int, count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = DukiDimens.space4, vertical = DukiDimens.space2),
    ) {
        DukiTag("${Tags.of("home", page + 1)} / %03d".format(count))
    }
}

@Composable
private fun HomePage(
    page: Int,
    state: LauncherUiState,
    drag: DragState,
    dropCell: Cell?,
    onOpen: (Cell, HomeItem) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val layout = state.saved.layout
    val items = layout.pageItems(page)
    Column(Modifier.fillMaxSize()) {
        for (row in 0 until layout.rows) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (col in 0 until layout.columns) {
                    val index = row * layout.columns + col
                    val cell = Cell(page, index)
                    HomeCell(
                        cell = cell,
                        item = items[index],
                        state = state,
                        isDragSource = drag.from == cell,
                        isDropTarget = dropCell == cell && drag.from != cell,
                        onOpen = onOpen,
                        onLongPress = { item, at -> drag.begin(item, cell, at) },
                        onDragDelta = onDragDelta,
                        onDragEnd = onDragEnd,
                        onDragCancel = { drag.clear() },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCell(
    cell: Cell,
    item: HomeItem?,
    state: LauncherUiState,
    isDragSource: Boolean,
    isDropTarget: Boolean,
    onOpen: (Cell, HomeItem) -> Unit,
    onLongPress: (HomeItem, Offset) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DukiTheme.colors
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val open by rememberUpdatedState(onOpen)
    val longPress by rememberUpdatedState(onLongPress)
    val dragDelta by rememberUpdatedState(onDragDelta)
    val dragEnd by rememberUpdatedState(onDragEnd)
    val dragCancel by rememberUpdatedState(onDragCancel)
    Box(
        modifier = modifier
            .onGloballyPositioned { coords = it }
            .then(if (isDropTarget) Modifier.border(DukiDimens.borderStrong, c.lineStrong) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (item == null) return@Box
        val label = when (item) {
            is HomeItem.App -> state.app(item.appKey)?.label ?: ""
            is HomeItem.Folder -> item.name
        }
        Column(
            modifier = Modifier
                .alpha(if (isDragSource) 0.25f else 1f)
                .itemGestures(
                    key = item to cell,
                    onTap = { open(cell, item) },
                    onLongPress = { local ->
                        val root = coords?.localToRoot(local) ?: local
                        longPress(item, root)
                    },
                    onDrag = { dragDelta(it) },
                    onRelease = { dragEnd() },
                    onCancel = { dragCancel() },
                )
                .semantics { contentDescription = label }
                .padding(DukiDimens.space1),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeItemIcon(item, state)
            if (state.settings.showLabels) {
                Spacer(Modifier.height(DukiDimens.space1))
                DukiText(
                    text = label,
                    style = DukiTheme.type.appLabel,
                    maxLines = 1,
                    modifier = Modifier
                        .background(c.surface)
                        .padding(horizontal = 2.dp),
                )
            }
        }
    }
}

/** ホーム画面の1マスのアイコン（アプリかフォルダ）。 */
@Composable
fun HomeItemIcon(item: HomeItem, state: LauncherUiState) {
    when (item) {
        is HomeItem.App -> AppIcon(state.app(item.appKey), HomeIconSize)
        is HomeItem.Folder -> FolderIcon(item.appKeys.map { state.app(it) }, HomeIconSize)
    }
}

@Composable
private fun PageIndicator(page: Int, count: Int) {
    if (count <= 1) {
        Spacer(Modifier.height(DukiDimens.space4))
        return
    }
    val c = DukiTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = DukiDimens.space2),
        horizontalArrangement = Arrangement.Center,
    ) {
        for (i in 0 until count) {
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(6.dp)
                    .then(
                        if (i == page) Modifier.background(c.ink)
                        else Modifier.border(DukiDimens.borderHair, c.lineStrong),
                    ),
            )
        }
    }
}

@Composable
private fun EmptyHint(modifier: Modifier = Modifier) {
    DukiCard(modifier = modifier.fillMaxWidth()) {
        DukiTag(Tags.of("hint"))
        DukiText("ホーム画面にはまだ何もありません", DukiTheme.type.h3)
        DukiText(
            "上にスワイプしてアプリ一覧を開き、アプリを長押しすると、ホーム画面やドックに置けます。",
            DukiTheme.type.bodySm,
            color = DukiTheme.colors.inkMuted,
        )
    }
}

/** 画面下のドック。 */
@Composable
private fun DockBar(
    state: LauncherUiState,
    highlighted: Boolean,
    onLaunch: (String) -> Unit,
    onMenu: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DukiTheme.colors
    val keys = state.saved.dock.appKeys
    val launch by rememberUpdatedState(onLaunch)
    val menu by rememberUpdatedState(onMenu)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surfaceRaised)
            .topLine(if (highlighted) c.ink else c.line, if (highlighted) DukiDimens.borderStrong else DukiDimens.borderHair)
            .cutCorner(DukiDimens.cutMd, c.cornerMark)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("dock"),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DukiDimens.space4, vertical = DukiDimens.space3),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (keys.isEmpty()) {
                DukiTag("${Tags.of("dock")} · よく使うアプリをここに置けます", modifier = Modifier.padding(vertical = DukiDimens.space4))
            }
            keys.forEachIndexed { index, key ->
                val app: AppEntry? = state.app(key)
                Box(
                    Modifier
                        .itemGestures(
                            key = key to index,
                            onTap = { launch(key) },
                            onLongPress = { menu(key, index) },
                        )
                        .semantics { contentDescription = app?.label ?: key }
                        .padding(DukiDimens.space1),
                ) {
                    AppIcon(app, HomeIconSize)
                }
            }
        }
    }
}
