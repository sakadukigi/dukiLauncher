package com.dukilab.launcher.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dukilab.launcher.core.Cell
import com.dukilab.launcher.core.HomeItem
import com.dukilab.launcher.core.HomeLayout
import com.dukilab.launcher.core.Shortcut
import com.dukilab.launcher.core.ThemeMode
import com.dukilab.launcher.ui.components.IconSource
import com.dukilab.launcher.ui.home.LocalClockTicking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherRootTest {
    @get:Rule
    val rule = createComposeRule()

    private val actions = FakeActions()
    private val nav = LauncherNav()
    private var state by mutableStateOf(TestData.state())

    private fun show() {
        rule.setContent {
            CompositionLocalProvider(LocalClockTicking provides false) {
                LauncherRoot(state = state, actions = actions, iconSource = IconSource { _, _, _ -> null }, nav = nav)
            }
        }
    }

    @Test
    fun homeShowsAppsAndDock() {
        show()
        rule.onNodeWithTag("home").assertIsDisplayed()
        rule.onNodeWithContentDescription("カメラ").assertIsDisplayed()
        rule.onNodeWithContentDescription("電話").assertIsDisplayed()
        rule.onNodeWithContentDescription("時刻").assertIsDisplayed()
    }

    @Test
    fun tapLaunchesApp() {
        show()
        rule.onNodeWithContentDescription("カメラ").performClick()
        rule.onNodeWithContentDescription("電話").performClick()
        assertEquals(listOf("launch:${TestData.camera.key}", "launch:${TestData.phone.key}"), actions.calls)
    }

    @Test
    fun longPressOnHomeAppShowsMenu() {
        show()
        rule.onNodeWithContentDescription("カメラ").performTouchInput { longClick() }
        rule.onNodeWithText("ホーム画面から外す").performClick()
        assertEquals(listOf("removeHomeCell:0:0"), actions.calls)
    }

    @Test
    fun systemAppHasNoUninstall() {
        show()
        rule.onNodeWithContentDescription("電話").performTouchInput { longClick() }
        rule.onNodeWithText("アプリ情報").assertIsDisplayed()
        rule.onNodeWithText("アンインストール").assertDoesNotExist()
        rule.onNodeWithText("左へ移動").assertIsDisplayed()
    }

    @Test
    fun swipeUpOpensDrawerAndSearchFilters() {
        show()
        rule.onNodeWithTag("home").performTouchInput { swipeUp() }
        rule.onNodeWithTag("drawer").assertIsDisplayed()
        rule.onNodeWithContentDescription("Chrome").assertIsDisplayed()
        rule.onNodeWithTag("drawer-search").performTextInput("ＧＯＯ")
        rule.onNodeWithContentDescription("Google Maps").assertExists()
        rule.onNodeWithContentDescription("Chrome").assertDoesNotExist()
    }

    @Test
    fun drawerMenuAddsToHomeAndDock() {
        show()
        rule.runOnIdle { nav.openDrawer(false) }
        rule.onNodeWithContentDescription("Chrome").performTouchInput { longClick() }
        rule.onNodeWithText("ホーム画面に置く").performClick()
        rule.onNodeWithContentDescription("Chrome").performTouchInput { longClick() }
        rule.onNodeWithText("ドックに追加").performClick()
        assertEquals(listOf("addToHome:${TestData.chrome.key}:0", "addToDock:${TestData.chrome.key}"), actions.calls)
    }

    @Test
    fun hiddenAppsAreNotInDrawer() {
        state = state.copy(saved = state.saved.copy(hidden = setOf(TestData.maps.key)))
        show()
        rule.runOnIdle { nav.openDrawer(false) }
        rule.onNodeWithContentDescription("Google Maps").assertDoesNotExist()
    }

    @Test
    fun folderOpensAndLaunches() {
        val folder = HomeItem.Folder("f1", "ツール", listOf(TestData.chrome.key, TestData.maps.key))
        state = TestData.state(layout = HomeLayout(4, 5, mapOf(Cell(0, 1) to folder)))
        show()
        rule.onNodeWithContentDescription("ツール").performClick()
        rule.onNodeWithTag("folder").assertIsDisplayed()
        rule.onNodeWithContentDescription("Google Maps").performClick()
        assertEquals(listOf("launch:${TestData.maps.key}"), actions.calls)
        rule.runOnIdle { assertNull("フォルダが閉じていない", nav.folderId) }
        rule.onNodeWithTag("folder").assertDoesNotExist()
    }

    @Test
    fun dragInFolderReordersAndDragOutTakesOut() {
        val folder = HomeItem.Folder("f1", "ツール", listOf(TestData.chrome.key, TestData.maps.key))
        state = TestData.state(layout = HomeLayout(4, 5, mapOf(Cell(0, 1) to folder)))
        show()
        rule.runOnIdle { nav.openFolder("f1") }
        val chrome = rule.onNodeWithContentDescription("Chrome").fetchSemanticsNode().boundsInRoot
        val maps = rule.onNodeWithContentDescription("Google Maps").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithContentDescription("Chrome").performTouchInput {
            down(center)
            // 動かさずに長押しが成立するまで待つ（longClick と同じく、その場で1回動かす）。
            moveBy(Offset.Zero, delayMillis = 1_000)
            val to = maps.center - chrome.topLeft
            repeat(5) { i -> moveTo(center + (to - center) * ((i + 1) / 5f)) }
            up()
        }
        assertEquals(listOf("reorder:f1:0->1"), actions.calls)

        rule.onNodeWithContentDescription("Chrome").performTouchInput {
            down(center)
            // 動かさずに長押しが成立するまで待つ（longClick と同じく、その場で1回動かす）。
            moveBy(Offset.Zero, delayMillis = 1_000)
            repeat(5) { i -> moveBy(Offset(0f, (i + 1) * 300f)) }
            up()
        }
        assertEquals("takeOut:f1:${TestData.chrome.key}", actions.calls.last())
    }

    @Test
    fun homeAppCanBePlacedAgain() {
        show()
        rule.onNodeWithContentDescription("カメラ").performTouchInput { longClick() }
        rule.onNodeWithText("ホーム画面にもう1つ置く").performClick()
        assertEquals(listOf("addToHome:${TestData.camera.key}:0"), actions.calls)
    }

    @Test
    fun webShortcutFromUrl() {
        show()
        rule.runOnIdle { nav.page = Page.ADD_WEB }
        rule.onNodeWithTag("add-web").assertIsDisplayed()
        rule.onNodeWithContentDescription("URL").performTextInput("javascript:x")
        rule.onNodeWithText("ホーム画面に置く").performClick()
        rule.onNodeWithText("http:// か https:// で始まる URL を入れてください").assertExists()
        rule.onNodeWithContentDescription("URL").performTextClearance()
        rule.onNodeWithContentDescription("URL").performTextInput("example.com/news")
        rule.onNodeWithText("ホーム画面に置く").performClick()
        assertEquals(listOf("addWeb:https://example.com/news:example.com:0"), actions.calls)
        rule.runOnIdle { assertEquals(Page.NONE, nav.page) }
    }

    @Test
    fun shortcutOnHomeShowsWithoutAppInfo() {
        val web = Shortcut(Shortcut.webKey("w1"), "ニュース", url = "https://example.com")
        val base = TestData.state()
        state = base.copy(saved = base.saved.addShortcut(web))
        show()
        rule.onNodeWithContentDescription("ニュース").performClick()
        assertEquals(listOf("launch:${web.key}"), actions.calls)
        rule.onNodeWithContentDescription("ニュース").performTouchInput { longClick() }
        rule.onNodeWithText("ホーム画面から外す").assertIsDisplayed()
        rule.onNodeWithText("アプリ情報").assertDoesNotExist()
    }

    @Test
    fun settingsChangeTheme() {
        show()
        rule.runOnIdle { nav.page = Page.SETTINGS }
        rule.onNodeWithTag("settings").assertIsDisplayed()
        rule.onNodeWithText("ダーク").performClick()
        assertEquals(ThemeMode.DARK, actions.settings.themeMode)
        rule.onNodeWithText("5列").performClick()
        assertTrue("grid:5x5" in actions.calls)
    }

    @Test
    fun backClosesTopMostFirst() {
        show()
        rule.runOnIdle {
            nav.openDrawer(false)
            nav.menu = MenuTarget.DrawerApp(TestData.chrome.key)
        }
        rule.runOnIdle { nav.back() }
        rule.onNodeWithTag("drawer").assertIsDisplayed()
        rule.runOnIdle { nav.back() }
        rule.onNodeWithTag("drawer").assertDoesNotExist()
    }

    @Test
    fun catalogShowsComponents() {
        show()
        rule.runOnIdle { nav.page = Page.CATALOG }
        rule.onNodeWithTag("catalog").assertIsDisplayed()
        rule.onNodeWithText("ダウンロードする").assertExists()
    }
}
