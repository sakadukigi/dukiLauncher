package com.dukilab.launcher.core

/** 画面の明るさ。 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** ホーム画面を下にスワイプしたときの動き。 */
enum class SwipeDownAction { NOTIFICATIONS, SEARCH, NONE }

/** ホーム画面の何もないところをダブルタップしたときの動き。 */
enum class DoubleTapAction { SCREEN_OFF, NONE }

/** 利用者が変えられる設定。 */
data class LauncherSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** アプリアイコンをモノクロにする。 */
    val monochromeIcons: Boolean = true,
    /** ホーム画面のアイコンの下に名前を出す。 */
    val showLabels: Boolean = true,
    val gridColumns: Int = HomeLayout.DEFAULT_COLUMNS,
    val gridRows: Int = HomeLayout.DEFAULT_ROWS,
    val swipeDown: SwipeDownAction = SwipeDownAction.NOTIFICATIONS,
    val doubleTap: DoubleTapAction = DoubleTapAction.NONE,
    /** ホーム画面の時計に秒を出す。 */
    val showSeconds: Boolean = true,
    /** 新しく入れたアプリをホーム画面に自動で置く。 */
    val autoAddNewApps: Boolean = true,
) {
    companion object {
        val COLUMN_CHOICES = listOf(4, 5)
        val ROW_CHOICES = listOf(4, 5, 6)
    }
}
