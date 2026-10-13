package com.dukilab.launcher.ui

import com.dukilab.launcher.core.AppEntry
import com.dukilab.launcher.core.Cell
import com.dukilab.launcher.core.Dock
import com.dukilab.launcher.core.HomeItem
import com.dukilab.launcher.core.HomeLayout
import com.dukilab.launcher.core.SavedState

object TestData {
    val camera = AppEntry("com.android.camera/Main#0", "カメラ", "com.android.camera")
    val chrome = AppEntry("com.android.chrome/Main#0", "Chrome", "com.android.chrome")
    val maps = AppEntry("com.google.maps/Main#0", "Google Maps", "com.google.maps")
    val phone = AppEntry("com.android.phone/Main#0", "電話", "com.android.phone", isSystemApp = true)
    val all = listOf(camera, chrome, maps, phone)

    fun state(
        layout: HomeLayout = HomeLayout(4, 5, mapOf(Cell(0, 0) to HomeItem.App(camera.key))),
        dock: Dock = Dock(listOf(phone.key)),
    ) = LauncherUiState(
        apps = all.associateBy { it.key },
        saved = SavedState(layout = layout, dock = dock, initialized = true, known = all.map { it.key }.toSet()),
        loaded = true,
    )
}
