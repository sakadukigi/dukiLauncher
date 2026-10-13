package com.dukilab.launcher.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.dukilab.launcher.ui.catalog.CatalogScreen
import com.dukilab.launcher.ui.components.IconSource
import com.dukilab.launcher.ui.components.LocalIconSource
import com.dukilab.launcher.ui.components.LocalMonochromeIcons
import com.dukilab.launcher.ui.drawer.AppDrawer
import com.dukilab.launcher.ui.home.FolderOverlay
import com.dukilab.launcher.ui.home.HomeScreen
import com.dukilab.launcher.ui.settings.AddWebShortcutScreen
import com.dukilab.launcher.ui.settings.HiddenAppsScreen
import com.dukilab.launcher.ui.settings.SettingsScreen
import com.dukilab.launcher.ui.theme.DukiTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** ホーム画面の上に重ねて開く画面。 */
enum class Page { NONE, SETTINGS, HIDDEN_APPS, CATALOG, ADD_WEB }

/** 開いている画面のまとまり（どれが一番上か）。 */
class LauncherNav {
    var page by mutableStateOf(Page.NONE)
    var drawerOpen by mutableStateOf(false)
    var drawerSearch by mutableStateOf(false)
    var folderId by mutableStateOf<String?>(null)
    var folderRename by mutableStateOf(false)
    var menu by mutableStateOf<MenuTarget?>(null)

    val anythingOpen: Boolean
        get() = page != Page.NONE || drawerOpen || folderId != null || menu != null

    /** 一番上のものを1つ閉じる。 */
    fun back() {
        when {
            menu != null -> menu = null
            page == Page.HIDDEN_APPS || page == Page.CATALOG -> page = Page.SETTINGS
            page != Page.NONE -> page = Page.NONE
            folderId != null -> folderId = null
            drawerOpen -> drawerOpen = false
        }
    }

    fun closeAll() {
        menu = null
        page = Page.NONE
        folderId = null
        drawerOpen = false
    }

    fun openDrawer(search: Boolean) {
        drawerSearch = search
        drawerOpen = true
    }

    fun openFolder(id: String, rename: Boolean = false) {
        folderRename = rename
        folderId = id
    }
}

/**
 * ランチャー全体。ホーム画面の上に、アプリ一覧・フォルダ・設定・長押しメニューを重ねる。
 * 戻る操作で一番上から閉じ、ホームボタンですべて閉じる（何も開いていなければ1ページ目へ）。
 */
@Composable
fun LauncherRoot(
    state: LauncherUiState,
    actions: LauncherActions,
    iconSource: IconSource,
    homePressed: Flow<Unit> = emptyFlow(),
    nav: LauncherNav = remember { LauncherNav() },
) {
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    var homeSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(homePressed) {
        homePressed.collect {
            if (nav.anythingOpen) nav.closeAll() else homeSignal++
        }
    }
    BackHandler(enabled = nav.anythingOpen) { nav.back() }

    DukiTheme(state.settings.themeMode) {
        CompositionLocalProvider(
            LocalIconSource provides iconSource,
            LocalMonochromeIcons provides state.settings.monochromeIcons,
        ) {
            SystemBarsAppearance(lightStatusBar = nav.drawerOpen && nav.page == Page.NONE && !DukiTheme.colors.isDark)
            Box(Modifier.fillMaxSize()) {
                HomeScreen(
                    state = state,
                    actions = actions,
                    homeSignal = homeSignal,
                    onPageChanged = { currentPage = it },
                    onOpenDrawer = nav::openDrawer,
                    onOpenFolder = { nav.openFolder(it) },
                    onMenu = { nav.menu = it },
                )

                AnimatedVisibility(
                    visible = nav.drawerOpen,
                    enter = slideInVertically { it / 3 } + fadeIn(),
                    exit = slideOutVertically { it / 3 } + fadeOut(),
                ) {
                    AppDrawer(
                        apps = state.drawerApps,
                        columns = state.settings.gridColumns,
                        focusSearch = nav.drawerSearch,
                        onLaunch = {
                            actions.launch(it.key)
                            nav.drawerOpen = false
                        },
                        onLongPress = { nav.menu = MenuTarget.DrawerApp(it.key) },
                        onClose = { nav.drawerOpen = false },
                    )
                }

                val folder = nav.folderId?.let { state.saved.layout.folder(it) }
                if (nav.folderId != null && folder == null) {
                    // フォルダが解かれたなど、もう無いときは閉じる。
                    LaunchedEffect(nav.folderId) { nav.folderId = null }
                }
                if (folder != null) {
                    FolderOverlay(
                        folder = folder,
                        apps = state::app,
                        editName = nav.folderRename,
                        onRename = { actions.renameFolder(folder.id, it) },
                        onLaunch = {
                            actions.launch(it)
                            nav.folderId = null
                        },
                        onReorder = { from, to -> actions.reorderInFolder(folder.id, from, to) },
                        onTakeOut = { actions.takeOutOfFolder(folder.id, it) },
                        onLongPress = { nav.menu = MenuTarget.FolderApp(folder.id, it) },
                        onDismiss = { nav.folderId = null },
                    )
                }

                when (nav.page) {
                    Page.NONE -> Unit
                    Page.SETTINGS -> SettingsScreen(
                        state = state,
                        actions = actions,
                        onOpenHiddenApps = { nav.page = Page.HIDDEN_APPS },
                        onOpenCatalog = { nav.page = Page.CATALOG },
                        onOpenAddWeb = { nav.page = Page.ADD_WEB },
                        onBack = { nav.page = Page.NONE },
                    )
                    Page.HIDDEN_APPS -> HiddenAppsScreen(state, actions, onBack = { nav.page = Page.SETTINGS })
                    Page.CATALOG -> CatalogScreen(onBack = { nav.page = Page.SETTINGS })
                    Page.ADD_WEB -> AddWebShortcutScreen(
                        onAdd = { url, label -> actions.addWebShortcut(url, label, currentPage) },
                        onBack = { nav.page = Page.NONE },
                    )
                }

                val menu = nav.menu
                if (menu != null) {
                    AppMenuSheet(
                        target = menu,
                        state = state,
                        items = menuActions(
                            target = menu,
                            state = state,
                            actions = actions,
                            currentPage = currentPage,
                            openFolderForRename = { nav.openFolder(it, rename = true) },
                            openSettings = {
                                nav.drawerOpen = false
                                nav.page = Page.SETTINGS
                            },
                            openDrawer = { nav.openDrawer(false) },
                            openAddWeb = {
                                nav.drawerOpen = false
                                nav.page = Page.ADD_WEB
                            },
                        ),
                        onDismiss = { nav.menu = null },
                    )
                }
            }
        }
    }
}

/**
 * ステータスバー・ナビゲーションバーの文字色を、下の面に合わせる。
 * ホーム画面と設定は上が黒いヘッダーなので白い文字、アプリ一覧（ライト）は黒い文字。
 */
@Composable
private fun SystemBarsAppearance(lightStatusBar: Boolean) {
    val view = LocalView.current
    val lightNavigationBar = !DukiTheme.colors.isDark
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = lightStatusBar
        controller.isAppearanceLightNavigationBars = lightNavigationBar
    }
}
