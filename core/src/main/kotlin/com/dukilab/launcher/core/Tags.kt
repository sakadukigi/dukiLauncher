package com.dukilab.launcher.core

/** dukiLab. のタグ表記（`分類 · 3桁の番号`）。 */
object Tags {
    fun of(category: String, number: Int? = null): String =
        if (number == null) category.uppercase() else "${category.uppercase()} · %03d".format(number)
}
