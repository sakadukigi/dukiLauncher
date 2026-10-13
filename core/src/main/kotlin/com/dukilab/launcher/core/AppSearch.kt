package com.dukilab.launcher.core

/** アプリ一覧の並べ替えと検索。 */
object AppSearch {

    /** 表示名の順（表記ゆれをそろえた上で辞書順、同じならキー順）。 */
    fun sorted(apps: Collection<AppEntry>): List<AppEntry> =
        apps.sortedWith(compareBy<AppEntry> { it.normalizedLabel }.thenBy { it.key })

    /**
     * [query] に合うアプリを、合い方の強い順に返す。
     *
     * 1. 表示名の先頭が一致
     * 2. 表示名の単語の先頭が一致
     * 3. 表示名のどこかに含む
     * 4. パッケージ名に含む
     *
     * 空の検索語ならすべてを表示名の順で返す。
     */
    fun search(apps: Collection<AppEntry>, query: String): List<AppEntry> {
        val q = TextNormalizer.normalize(query)
        if (q.isEmpty()) return sorted(apps)
        return apps.mapNotNull { app -> rank(app, q)?.let { it to app } }
            .sortedWith(compareBy<Pair<Int, AppEntry>> { it.first }
                .thenBy { it.second.normalizedLabel }
                .thenBy { it.second.key })
            .map { it.second }
    }

    private fun rank(app: AppEntry, q: String): Int? {
        val label = app.normalizedLabel
        return when {
            label.startsWith(q) -> 0
            wordStarts(app.label).any { it.startsWith(q) } -> 1
            label.contains(q) -> 2
            app.packageName.lowercase().contains(q) -> 3
            else -> null
        }
    }

    private fun wordStarts(label: String): List<String> =
        label.split(' ', '　', '-', '_', '・', '.')
            .filter { it.isNotBlank() }
            .map { TextNormalizer.normalize(it) }
            .let { words -> words.indices.map { i -> words.drop(i).joinToString("") } }
}
