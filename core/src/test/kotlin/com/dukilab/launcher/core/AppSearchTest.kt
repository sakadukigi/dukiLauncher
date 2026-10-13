package com.dukilab.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSearchTest {
    private fun app(label: String, pkg: String = "pkg.${label.hashCode()}") =
        AppEntry(AppEntry.keyOf(pkg, "Main", 0), label, pkg)

    private val camera = app("カメラ", "com.android.camera")
    private val calendar = app("Calendar", "com.google.calendar")
    private val chrome = app("Chrome", "com.android.chrome")
    private val maps = app("Google Maps", "com.google.maps")
    private val settings = app("設定", "com.android.settings")
    private val all = listOf(camera, calendar, chrome, maps, settings)

    @Test
    fun emptyQueryReturnsAllSorted() {
        assertEquals(listOf(calendar, chrome, maps, camera, settings), AppSearch.search(all, " "))
    }

    @Test
    fun hiraganaMatchesKatakana() {
        assertEquals(listOf(camera), AppSearch.search(all, "かめ"))
        assertEquals(listOf(camera), AppSearch.search(all, "ｶﾒﾗ"))
    }

    @Test
    fun fullWidthAndCaseAreIgnored() {
        assertEquals(listOf(chrome), AppSearch.search(all, "ＣＨＲ"))
    }

    @Test
    fun prefixRanksBeforeWordPrefixAndContains() {
        val result = AppSearch.search(all, "ma")
        assertEquals(listOf(maps), result)
        val c = AppSearch.search(all, "c")
        // 先頭一致（Calendar・Chrome）→ パッケージ名に含む（カメラ・設定・Maps）
        assertEquals(listOf(calendar, chrome), c.take(2))
    }

    @Test
    fun wordPrefixMatches() {
        assertEquals(listOf(maps), AppSearch.search(all, "maps"))
    }

    @Test
    fun packageNameMatchesLast() {
        assertEquals(listOf(settings), AppSearch.search(all, "settings"))
    }

    @Test
    fun indexLetters() {
        assertEquals("C", IndexLetter.of("chrome"))
        assertEquals("か", IndexLetter.of("カメラ"))
        assertEquals("か", IndexLetter.of("ガイド"))
        assertEquals("#", IndexLetter.of("設定"))
        assertEquals("#", IndexLetter.of("1Password"))
        assertEquals(listOf("C", "G", "か", "#"), sectioned(all).map { it.letter })
    }
}
