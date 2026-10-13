package com.dukilab.launcher.core

/**
 * ランチャーに並ぶアプリ1件。
 *
 * [key] は「パッケージ/アクティビティ#ユーザー」の形で、仕事用プロフィールなど
 * 同じアプリが複数あっても区別できる。保存する並びやドックはこのキーで持つ。
 */
data class AppEntry(
    val key: String,
    val label: String,
    val packageName: String,
    /** 端末に最初から入っているアプリ（アンインストールできない）。 */
    val isSystemApp: Boolean = false,
) {
    /** 検索と並べ替えに使う、表記ゆれをそろえたラベル。 */
    val normalizedLabel: String = TextNormalizer.normalize(label)

    companion object {
        fun keyOf(packageName: String, activityName: String, userSerial: Long): String =
            "$packageName/$activityName#$userSerial"
    }
}
