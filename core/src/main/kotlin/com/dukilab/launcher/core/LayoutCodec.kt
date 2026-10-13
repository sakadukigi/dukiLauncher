package com.dukilab.launcher.core

/**
 * ホーム画面の配置を文字列にして保存・復元する。
 *
 * 1行目が `v1 列数 行数`、以降は1マス1行：
 * - アプリ：`A ページ 番号 キー`
 * - フォルダ：`F ページ 番号 ID 名前 キー1 キー2 …`
 *
 * 項目はタブ区切り。値の中のタブ・改行・% はパーセント表記に置き換える。
 */
object LayoutCodec {
    private const val VERSION = "v1"

    fun encode(layout: HomeLayout): String = buildString {
        append(VERSION).append('\t').append(layout.columns).append('\t').append(layout.rows)
        layout.items.entries
            .sortedWith(compareBy({ it.key.page }, { it.key.index }))
            .forEach { (cell, item) ->
                append('\n')
                when (item) {
                    is HomeItem.App -> join("A", cell.page, cell.index, item.appKey)
                    is HomeItem.Folder -> join("F", cell.page, cell.index, item.id, item.name, *item.appKeys.toTypedArray())
                }
            }
    }

    /** 壊れた行は読み飛ばす。読めなければ空の配置を返す。 */
    fun decode(text: String?): HomeLayout {
        if (text.isNullOrBlank()) return HomeLayout()
        val lines = text.split('\n')
        val head = lines.first().split('\t')
        if (head.size < 3 || head[0] != VERSION) return HomeLayout()
        val columns = head[1].toIntOrNull()?.takeIf { it > 0 } ?: return HomeLayout()
        val rows = head[2].toIntOrNull()?.takeIf { it > 0 } ?: return HomeLayout()
        val items = mutableMapOf<Cell, HomeItem>()
        for (line in lines.drop(1)) {
            val f = line.split('\t').map(::unescape)
            if (f.size < 4) continue
            val page = f[1].toIntOrNull() ?: continue
            val index = f[2].toIntOrNull() ?: continue
            if (page < 0 || index !in 0 until columns * rows) continue
            val cell = Cell(page, index)
            if (cell in items) continue
            when (f[0]) {
                "A" -> items[cell] = HomeItem.App(f[3])
                "F" -> {
                    if (f.size < 5) continue
                    val keys = f.drop(5).filter { it.isNotEmpty() }
                    when (keys.size) {
                        0 -> Unit
                        1 -> items[cell] = HomeItem.App(keys.single())
                        else -> items[cell] = HomeItem.Folder(f[3], f[4], keys)
                    }
                }
            }
        }
        return HomeLayout(columns, rows, items)
    }

    /** 文字列の並び（ドック・非表示アプリなど）を保存用の1つの文字列にする。 */
    fun encodeList(values: List<String>): String = values.joinToString("\t") { escape(it) }

    fun decodeList(text: String?): List<String> =
        if (text.isNullOrEmpty()) emptyList() else text.split('\t').map(::unescape)

    /**
     * ショートカットを保存用の文字列にする。1件1行：
     * `S キー 名前 パッケージ ID ユーザー URL`
     */
    fun encodeShortcuts(shortcuts: Collection<Shortcut>): String =
        shortcuts.sortedBy { it.key }.joinToString("\n") { sc ->
            buildString { join("S", sc.key, sc.label, sc.packageName, sc.shortcutId, sc.userSerial, sc.url) }
        }

    fun decodeShortcuts(text: String?): Map<String, Shortcut> {
        if (text.isNullOrBlank()) return emptyMap()
        return text.split('\n').mapNotNull { line ->
            val f = line.split('\t').map(::unescape)
            if (f.size < 7 || f[0] != "S" || !Shortcut.isShortcutKey(f[1])) return@mapNotNull null
            Shortcut(f[1], f[2], f[3], f[4], f[5].toLongOrNull() ?: 0L, f[6])
        }.associateBy { it.key }
    }

    private fun StringBuilder.join(vararg parts: Any) {
        parts.forEachIndexed { i, p ->
            if (i > 0) append('\t')
            append(escape(p.toString()))
        }
    }

    private fun escape(s: String): String =
        s.replace("%", "%25").replace("\t", "%09").replace("\n", "%0A")

    private fun unescape(s: String): String =
        s.replace("%0A", "\n").replace("%09", "\t").replace("%25", "%")
}
