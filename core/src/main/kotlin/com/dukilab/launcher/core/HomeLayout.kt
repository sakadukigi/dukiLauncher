package com.dukilab.launcher.core

/** ホーム画面のマス目に置くもの。 */
sealed interface HomeItem {
    /** アプリ1つ。 */
    data class App(val appKey: String) : HomeItem

    /** 複数のアプリをまとめたフォルダ。 */
    data class Folder(val id: String, val name: String, val appKeys: List<String>) : HomeItem
}

/** ホーム画面のマスの位置（ページ番号と、ページ内で左上から数えた番号）。 */
data class Cell(val page: Int, val index: Int)

/**
 * ホーム画面の配置。ページごとに [columns]×[rows] のマスがあり、
 * 各マスにアプリかフォルダを1つ置ける。値は変更せず、操作のたびに新しい配置を返す。
 */
data class HomeLayout(
    val columns: Int = DEFAULT_COLUMNS,
    val rows: Int = DEFAULT_ROWS,
    val items: Map<Cell, HomeItem> = emptyMap(),
) {
    init {
        require(columns > 0 && rows > 0)
    }

    val cellsPerPage: Int get() = columns * rows

    /** ページ数。空でも1ページはある。後ろの空ページは数えない。 */
    val pageCount: Int get() = (items.keys.maxOfOrNull { it.page } ?: 0) + 1

    fun itemAt(cell: Cell): HomeItem? = items[cell]

    fun pageItems(page: Int): Map<Int, HomeItem> =
        items.filterKeys { it.page == page }.mapKeys { it.key.index }

    /** ホーム画面（フォルダの中も含む）に置かれているアプリのキー。 */
    val appKeys: Set<String>
        get() = items.values.flatMap {
            when (it) {
                is HomeItem.App -> listOf(it.appKey)
                is HomeItem.Folder -> it.appKeys
            }
        }.toSet()

    fun contains(appKey: String): Boolean = appKey in appKeys

    fun folder(id: String): HomeItem.Folder? =
        items.values.filterIsInstance<HomeItem.Folder>().firstOrNull { it.id == id }

    fun cellOfFolder(id: String): Cell? =
        items.entries.firstOrNull { (it.value as? HomeItem.Folder)?.id == id }?.key

    /** 最初の空きマス。全ページ埋まっていれば新しいページの先頭。 */
    fun firstEmptyCell(fromPage: Int = 0): Cell {
        for (page in fromPage until pageCount) {
            for (i in 0 until cellsPerPage) {
                val c = Cell(page, i)
                if (c !in items) return c
            }
        }
        return Cell(maxOf(pageCount, fromPage), 0)
    }

    /**
     * アプリをホーム画面の空きマスに追加する。
     * [allowDuplicate] でなければ、すでに置いてあるときは何もしない（新しいアプリの自動配置など）。
     */
    fun addApp(appKey: String, preferredPage: Int = 0, allowDuplicate: Boolean = false): HomeLayout {
        if (!allowDuplicate && contains(appKey)) return this
        val cell = firstEmptyCell(preferredPage.coerceAtLeast(0))
        return copy(items = items + (cell to HomeItem.App(appKey)))
    }

    /** アプリをホーム画面（フォルダの中も含む）から外す。 */
    fun removeApp(appKey: String): HomeLayout {
        val next = items.mapNotNull { (cell, item) ->
            when (item) {
                is HomeItem.App -> if (item.appKey == appKey) null else cell to item
                is HomeItem.Folder -> {
                    if (appKey !in item.appKeys) cell to item
                    else unwrapIfSmall(item.copy(appKeys = item.appKeys - appKey))?.let { cell to it }
                }
            }
        }.toMap()
        return copy(items = next)
    }

    /** マスの中身を外す（フォルダならフォルダごと）。 */
    fun removeAt(cell: Cell): HomeLayout = copy(items = items - cell)

    /**
     * [from] の中身を [to] へ動かす。
     *
     * - 移動先が空：そのまま移動
     * - アプリをアプリに重ねる：2つでフォルダを作る
     * - アプリをフォルダに重ねる：フォルダに入れる
     * - それ以外（フォルダをアプリに重ねるなど）：入れ替える
     */
    fun move(from: Cell, to: Cell, newFolderId: () -> String = { "f${System.nanoTime()}" }): HomeLayout {
        if (from == to) return this
        val moving = items[from] ?: return this
        require(to.index in 0 until cellsPerPage && to.page >= 0)
        val target = items[to]
        val next = items.toMutableMap()
        when {
            target == null -> {
                next.remove(from)
                next[to] = moving
            }
            moving is HomeItem.App && target is HomeItem.App -> {
                next.remove(from)
                next[to] = HomeItem.Folder(newFolderId(), DEFAULT_FOLDER_NAME, listOf(target.appKey, moving.appKey))
            }
            moving is HomeItem.App && target is HomeItem.Folder -> {
                next.remove(from)
                next[to] = target.copy(appKeys = target.appKeys + moving.appKey)
            }
            else -> {
                next[from] = target
                next[to] = moving
            }
        }
        return copy(items = next)
    }

    /** フォルダの名前を変える。空なら既定の名前にする。 */
    fun renameFolder(id: String, name: String): HomeLayout = mapFolder(id) {
        it.copy(name = name.trim().ifEmpty { DEFAULT_FOLDER_NAME })
    }

    /** フォルダからアプリを出して、空いているマスに置く。 */
    fun takeOutOfFolder(id: String, appKey: String): HomeLayout {
        val cell = cellOfFolder(id) ?: return this
        val folder = items[cell] as HomeItem.Folder
        if (appKey !in folder.appKeys) return this
        val rest = unwrapIfSmall(folder.copy(appKeys = folder.appKeys - appKey))
        val without = copy(items = if (rest == null) items - cell else items + (cell to rest))
        val place = without.firstEmptyCell(cell.page)
        return without.copy(items = without.items + (place to HomeItem.App(appKey)))
    }

    /** フォルダを解いて、中のアプリをホーム画面に並べ直す（先頭のアプリはフォルダのあった場所へ）。 */
    fun dissolveFolder(id: String): HomeLayout {
        val cell = cellOfFolder(id) ?: return this
        val folder = items[cell] as HomeItem.Folder
        var next = copy(items = items + (cell to HomeItem.App(folder.appKeys.first())))
        for (key in folder.appKeys.drop(1)) {
            next = next.copy(items = next.items + (next.firstEmptyCell(cell.page) to HomeItem.App(key)))
        }
        return next
    }

    /** フォルダの中の順番を入れ替える。 */
    fun reorderInFolder(id: String, from: Int, to: Int): HomeLayout = mapFolder(id) { f ->
        if (from !in f.appKeys.indices || to !in f.appKeys.indices) f
        else f.copy(appKeys = f.appKeys.toMutableList().apply { add(to, removeAt(from)) })
    }

    /** アンインストールされたアプリを取り除く。 */
    fun retainInstalled(installed: Set<String>): HomeLayout {
        val missing = appKeys - installed
        return missing.fold(this) { acc, key -> acc.removeApp(key) }
    }

    /**
     * マス目の大きさを変える。はみ出すものは元の並び順のまま詰め直す。
     */
    fun resized(newColumns: Int, newRows: Int): HomeLayout {
        if (newColumns == columns && newRows == rows) return this
        val ordered = items.entries.sortedWith(compareBy({ it.key.page }, { it.key.index })).map { it.value }
        val perPage = newColumns * newRows
        val next = ordered.mapIndexed { i, item -> Cell(i / perPage, i % perPage) to item }.toMap()
        return HomeLayout(newColumns, newRows, next)
    }

    private fun mapFolder(id: String, f: (HomeItem.Folder) -> HomeItem.Folder): HomeLayout {
        val cell = cellOfFolder(id) ?: return this
        return copy(items = items + (cell to f(items[cell] as HomeItem.Folder)))
    }

    /** 中身が1つになったフォルダはアプリに戻し、空なら消す。 */
    private fun unwrapIfSmall(folder: HomeItem.Folder): HomeItem? = when (folder.appKeys.size) {
        0 -> null
        1 -> HomeItem.App(folder.appKeys.single())
        else -> folder
    }

    companion object {
        const val DEFAULT_COLUMNS = 4
        const val DEFAULT_ROWS = 5
        const val DEFAULT_FOLDER_NAME = "フォルダ"
    }
}
