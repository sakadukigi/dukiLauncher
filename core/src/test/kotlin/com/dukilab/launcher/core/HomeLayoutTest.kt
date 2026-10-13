package com.dukilab.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    private val small = HomeLayout(columns = 2, rows = 2)

    @Test
    fun addFillsCellsInOrderAndOpensNewPage() {
        val l = listOf("a", "b", "c", "d", "e").fold(small) { acc, k -> acc.addApp(k) }
        assertEquals(HomeItem.App("a"), l.itemAt(Cell(0, 0)))
        assertEquals(HomeItem.App("d"), l.itemAt(Cell(0, 3)))
        assertEquals(HomeItem.App("e"), l.itemAt(Cell(1, 0)))
        assertEquals(2, l.pageCount)
    }

    @Test
    fun addingTwiceDoesNothing() {
        val l = small.addApp("a").addApp("a")
        assertEquals(1, l.items.size)
    }

    @Test
    fun sameAppCanBePlacedTwiceAndSurvivesSaving() {
        val l = small.addApp("a").addApp("a", allowDuplicate = true)
        assertEquals(listOf(HomeItem.App("a"), HomeItem.App("a")), listOf(l.itemAt(Cell(0, 0)), l.itemAt(Cell(0, 1))))
        assertEquals(l, LayoutCodec.decode(LayoutCodec.encode(l)))
        assertTrue(l.removeApp("a").items.isEmpty())
    }

    @Test
    fun moveToEmptyCell() {
        val l = small.addApp("a").move(Cell(0, 0), Cell(0, 3))
        assertNull(l.itemAt(Cell(0, 0)))
        assertEquals(HomeItem.App("a"), l.itemAt(Cell(0, 3)))
    }

    @Test
    fun dropAppOnAppMakesFolder() {
        val l = small.addApp("a").addApp("b").move(Cell(0, 1), Cell(0, 0)) { "f1" }
        assertEquals(HomeItem.Folder("f1", "フォルダ", listOf("a", "b")), l.itemAt(Cell(0, 0)))
        assertNull(l.itemAt(Cell(0, 1)))
        assertTrue(l.contains("b"))
    }

    @Test
    fun dropAppOnFolderAddsToIt() {
        val l = small.addApp("a").addApp("b").addApp("c")
            .move(Cell(0, 1), Cell(0, 0)) { "f1" }
            .move(Cell(0, 2), Cell(0, 0))
        assertEquals(listOf("a", "b", "c"), (l.itemAt(Cell(0, 0)) as HomeItem.Folder).appKeys)
    }

    @Test
    fun dropFolderOnAppSwaps() {
        val l = small.addApp("a").addApp("b").addApp("c")
            .move(Cell(0, 1), Cell(0, 0)) { "f1" }
            .move(Cell(0, 0), Cell(0, 2))
        assertEquals(HomeItem.App("c"), l.itemAt(Cell(0, 0)))
        assertTrue(l.itemAt(Cell(0, 2)) is HomeItem.Folder)
    }

    @Test
    fun removingFromFolderUnwrapsLastApp() {
        val l = small.addApp("a").addApp("b").move(Cell(0, 1), Cell(0, 0)) { "f1" }.removeApp("a")
        assertEquals(HomeItem.App("b"), l.itemAt(Cell(0, 0)))
    }

    @Test
    fun takeOutOfFolderPlacesAppInEmptyCell() {
        val l = small.addApp("a").addApp("b").addApp("c")
            .move(Cell(0, 1), Cell(0, 0)) { "f1" }
            .move(Cell(0, 2), Cell(0, 0))
            .takeOutOfFolder("f1", "b")
        assertEquals(listOf("a", "c"), (l.itemAt(Cell(0, 0)) as HomeItem.Folder).appKeys)
        assertEquals(HomeItem.App("b"), l.itemAt(Cell(0, 1)))
    }

    @Test
    fun renameFolderFallsBackToDefault() {
        val l = small.addApp("a").addApp("b").move(Cell(0, 1), Cell(0, 0)) { "f1" }
        assertEquals("ツール", l.renameFolder("f1", " ツール ").folder("f1")!!.name)
        assertEquals("フォルダ", l.renameFolder("f1", "  ").folder("f1")!!.name)
    }

    @Test
    fun dissolveFolderSpreadsApps() {
        val l = small.addApp("a").addApp("b").addApp("c")
            .move(Cell(0, 1), Cell(0, 0)) { "f1" }
            .move(Cell(0, 2), Cell(0, 0))
            .dissolveFolder("f1")
        assertEquals(HomeItem.App("a"), l.itemAt(Cell(0, 0)))
        assertEquals(HomeItem.App("b"), l.itemAt(Cell(0, 1)))
        assertEquals(HomeItem.App("c"), l.itemAt(Cell(0, 2)))
    }

    @Test
    fun reorderInFolder() {
        val l = small.addApp("a").addApp("b").addApp("c")
            .move(Cell(0, 1), Cell(0, 0)) { "f1" }
            .move(Cell(0, 2), Cell(0, 0))
            .reorderInFolder("f1", 2, 0)
        assertEquals(listOf("c", "a", "b"), l.folder("f1")!!.appKeys)
    }

    @Test
    fun resizeKeepsOrder() {
        val l = listOf("a", "b", "c", "d", "e").fold(HomeLayout(3, 3)) { acc, k -> acc.addApp(k) }
            .resized(2, 2)
        assertEquals(HomeItem.App("d"), l.itemAt(Cell(0, 3)))
        assertEquals(HomeItem.App("e"), l.itemAt(Cell(1, 0)))
    }

    @Test
    fun retainInstalledDropsMissing() {
        val l = small.addApp("a").addApp("b").retainInstalled(setOf("b"))
        assertFalse(l.contains("a"))
        assertTrue(l.contains("b"))
    }
}
