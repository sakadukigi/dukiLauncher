package com.dukilab.launcher.core

import java.text.Normalizer

/**
 * 検索用に文字の表記ゆれをそろえる。
 *
 * - 全角英数・半角カナは NFKC でそろえる
 * - カタカナはひらがなにする
 * - 英字は小文字にする
 * - 空白・中黒・ハイフンなどの区切りは取り除く
 */
object TextNormalizer {
    private val separators = setOf(' ', '　', '・', '-', '_', '.', '\'', '’')

    fun normalize(text: String): String {
        val nfkc = Normalizer.normalize(text, Normalizer.Form.NFKC)
        val sb = StringBuilder(nfkc.length)
        for (ch in nfkc) {
            if (ch in separators) continue
            sb.append(toHiragana(ch).lowercaseChar())
        }
        return sb.toString()
    }

    private fun toHiragana(ch: Char): Char =
        if (ch in 'ァ'..'ヶ') ch - 0x60 else ch
}
