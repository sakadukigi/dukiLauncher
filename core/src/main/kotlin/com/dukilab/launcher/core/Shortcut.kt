package com.dukilab.launcher.core

/**
 * ホーム画面に置いたショートカット。
 *
 * - アプリが作ったもの（Chrome の「ホーム画面に追加」など）：[packageName]・[shortcutId]・[userSerial] で開く
 * - Web ページ：[url] をブラウザで開く
 *
 * 配置やドックにはアプリと同じく [key] で置く。キーは必ず [PREFIX] で始まる。
 */
data class Shortcut(
    val key: String,
    val label: String,
    val packageName: String = "",
    val shortcutId: String = "",
    val userSerial: Long = 0,
    val url: String = "",
) {
    val isWeb: Boolean get() = url.isNotEmpty()

    /** 一覧やメニューに出すための [AppEntry] にする（アンインストールはできない扱い）。 */
    fun toEntry(): AppEntry = AppEntry(key = key, label = label, packageName = if (isWeb) url else packageName, isSystemApp = true)

    companion object {
        const val PREFIX = "sc:"

        fun isShortcutKey(key: String): Boolean = key.startsWith(PREFIX)

        fun pinnedKey(packageName: String, shortcutId: String, userSerial: Long): String =
            "${PREFIX}$packageName/$shortcutId#$userSerial"

        fun webKey(id: String): String = "${PREFIX}web/$id"

        /** 入力された URL を開ける形にそろえる。開けそうになければ null。 */
        fun normalizeUrl(input: String): String? {
            val t = input.trim()
            if (t.isEmpty() || t.any { it.isWhitespace() }) return null
            val withScheme = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(t)) t else "https://$t"
            val scheme = withScheme.substringBefore(':').lowercase()
            if (scheme != "http" && scheme != "https") return null
            val host = withScheme.substringAfter("://", "").substringBefore('/').substringBefore('?')
            return if (host.isEmpty()) null else withScheme
        }

        /** URL から既定の名前（ドメイン）を作る。 */
        fun labelFromUrl(url: String): String =
            url.substringAfter("://").substringBefore('/').substringBefore('?').removePrefix("www.")
    }
}
