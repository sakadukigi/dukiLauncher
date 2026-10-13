package com.dukilab.launcher.core

/**
 * アプリ一覧の見出し（索引）。英字は A〜Z、かなは行（あ・か・さ…）、
 * それ以外（数字・漢字・記号）は「#」にまとめる。
 */
object IndexLetter {
    const val OTHER = "#"

    private val kanaRows = listOf(
        "あ" to "ぁあぃいぅうぇえぉお",
        "か" to "かがきぎくぐけげこご",
        "さ" to "さざしじすずせぜそぞ",
        "た" to "ただちぢっつづてでとど",
        "な" to "なにぬねの",
        "は" to "はばぱひびぴふぶぷへべぺほぼぽ",
        "ま" to "まみむめも",
        "や" to "ゃやゅゆょよ",
        "ら" to "らりるれろ",
        "わ" to "ゎわゐゑをんゔ",
    )

    fun of(label: String): String {
        val first = TextNormalizer.normalize(label).firstOrNull() ?: return OTHER
        if (first in 'a'..'z') return first.uppercaseChar().toString()
        for ((row, chars) in kanaRows) if (first in chars) return row
        return OTHER
    }

    /** 見出しの並び順：A〜Z、あ〜わ、#。 */
    fun order(letter: String): Int = when {
        letter.length == 1 && letter[0] in 'A'..'Z' -> letter[0] - 'A'
        else -> kanaRows.indexOfFirst { it.first == letter }.let { if (it >= 0) 100 + it else 1000 }
    }
}

/** 見出しごとにまとめたアプリ一覧の1区切り。 */
data class AppSection(val letter: String, val apps: List<AppEntry>)

/** 表示名の順に並べてから見出しごとに区切る。 */
fun sectioned(apps: Collection<AppEntry>): List<AppSection> =
    AppSearch.sorted(apps)
        .groupBy { IndexLetter.of(it.label) }
        .toSortedMap(compareBy { IndexLetter.order(it) })
        .map { (letter, list) -> AppSection(letter, list) }
