package com.dukilab.launcher.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dukilab.launcher.core.AppEntry
import com.dukilab.launcher.core.AppSearch
import com.dukilab.launcher.core.Tags
import com.dukilab.launcher.core.sectioned
import com.dukilab.launcher.ui.components.AppIcon
import com.dukilab.launcher.ui.components.DukiTag
import com.dukilab.launcher.ui.components.DukiText
import com.dukilab.launcher.ui.components.DukiTextField
import com.dukilab.launcher.ui.components.bottomLine
import com.dukilab.launcher.ui.components.gridBackground
import com.dukilab.launcher.ui.components.itemGestures
import com.dukilab.launcher.ui.theme.DukiDimens
import com.dukilab.launcher.ui.theme.DukiTheme

/** 一覧の1行に並べる要素（見出しかアプリ）。 */
private sealed interface DrawerRow {
    data class Header(val letter: String) : DrawerRow
    data class App(val app: AppEntry) : DrawerRow
}

/**
 * アプリ一覧。上に検索欄、その下に見出し（A〜Z・あ〜わ・#）ごとにアプリを並べる。
 * 検索はひらがな・カタカナ・全角半角・大文字小文字を区別しない。
 * 一番上で下に引くと閉じる。検索欄で確定すると、いちばん上の候補を開く。
 */
@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    columns: Int,
    focusSearch: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DukiTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(apps, query) { AppSearch.search(apps, query) }
    val rows = remember(apps, query, results) {
        if (query.isBlank()) {
            sectioned(apps).flatMap { section ->
                listOf<DrawerRow>(DrawerRow.Header(section.letter)) + section.apps.map { DrawerRow.App(it) }
            }
        } else {
            results.map { DrawerRow.App(it) }
        }
    }
    val gridState = rememberLazyGridState()
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val closeDistance = with(LocalDensity.current) { 96.dp.toPx() }
    val close by rememberUpdatedState(onClose)

    LaunchedEffect(focusSearch) {
        if (focusSearch) runCatching { focus.requestFocus() }
    }
    LaunchedEffect(query) { gridState.scrollToItem(0) }

    // 一番上で下へ引っぱると閉じる。
    val pullToClose = remember(closeDistance) {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > closeDistance) {
                        pulled = 0f
                        close()
                    }
                } else if (available.y < 0f || consumed.y != 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .gridBackground(c.surface, c.grid)
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars).union(WindowInsets.ime))
            .testTag("drawer"),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onVerticalDrag = { _, dy -> total += dy },
                        onDragEnd = { if (total > closeDistance / 2) close() },
                    )
                }
                .bottomLine(c.line)
                .padding(horizontal = DukiDimens.space4, vertical = DukiDimens.space3),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DukiTag(Tags.of("apps", apps.size))
                Spacer(Modifier.weight(1f))
                DukiTag(
                    "閉じる",
                    modifier = Modifier
                        .itemGestures(Unit, onTap = { close() }, onLongPress = {})
                        .semantics { contentDescription = "アプリ一覧を閉じる" }
                        .padding(DukiDimens.space2),
                )
            }
            Spacer(Modifier.height(DukiDimens.space2))
            DukiTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "アプリを検索",
                imeAction = ImeAction.Go,
                onImeAction = {
                    results.firstOrNull()?.let {
                        focusManager.clearFocus()
                        onLaunch(it)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .testTag("drawer-search"),
            )
        }

        if (rows.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(DukiDimens.space5), contentAlignment = Alignment.Center) {
                DukiText("見つかりませんでした", DukiTheme.type.bodySm, color = c.inkMuted)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(pullToClose),
            horizontalArrangement = Arrangement.spacedBy(DukiDimens.space1),
            contentPadding = PaddingValues(
                horizontal = DukiDimens.space2,
                vertical = DukiDimens.space2,
            ),
        ) {
            items(
                items = rows,
                key = {
                    when (it) {
                        is DrawerRow.Header -> "h:${it.letter}"
                        is DrawerRow.App -> it.app.key
                    }
                },
                span = { row ->
                    when (row) {
                        is DrawerRow.Header -> GridItemSpan(maxLineSpan)
                        is DrawerRow.App -> GridItemSpan(1)
                    }
                },
            ) { row ->
                when (row) {
                    is DrawerRow.Header -> SectionHeader(row.letter)
                    is DrawerRow.App -> DrawerApp(
                        app = row.app,
                        onLaunch = { focusManager.clearFocus(); onLaunch(row.app) },
                        onLongPress = { focusManager.clearFocus(); onLongPress(row.app) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(letter: String) {
    val c = DukiTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = DukiDimens.space2, end = DukiDimens.space2, top = DukiDimens.space4, bottom = DukiDimens.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DukiText(letter, DukiTheme.type.spec, modifier = Modifier.background(c.surface))
        Spacer(Modifier.width(DukiDimens.space2))
        Box(
            Modifier
                .weight(1f)
                .height(DukiDimens.borderHair)
                .background(c.line),
        )
    }
}

@Composable
private fun DrawerApp(app: AppEntry, onLaunch: () -> Unit, onLongPress: () -> Unit) {
    val c = DukiTheme.colors
    val launch by rememberUpdatedState(onLaunch)
    val longPress by rememberUpdatedState(onLongPress)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .itemGestures(app.key, onTap = { launch() }, onLongPress = { longPress() })
            .semantics { contentDescription = app.label }
            .padding(vertical = DukiDimens.space2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(app, 48.dp)
        Spacer(Modifier.height(DukiDimens.space1))
        DukiText(
            text = app.label,
            style = DukiTheme.type.appLabel,
            maxLines = 1,
            modifier = Modifier
                .background(c.surface)
                .padding(horizontal = 2.dp),
        )
    }
}
