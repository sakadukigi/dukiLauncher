package com.dukilab.launcher.core

import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/** ホーム画面の時計と日付の表記。 */
object ClockText {
    /** 例：`07:05`（24時間表記）／`7:05`（12時間表記）。[seconds] なら `07:05:09`。 */
    fun time(now: LocalDateTime, is24Hour: Boolean, seconds: Boolean = false): String {
        val hour = if (is24Hour) now.hour else (now.hour % 12).let { if (it == 0) 12 else it }
        val h = if (is24Hour) "%02d".format(hour) else hour.toString()
        val hm = "$h:%02d".format(now.minute)
        return if (seconds) hm + ":%02d".format(now.second) else hm
    }

    /** 12時間表記のときの午前・午後。 */
    fun amPm(now: LocalDateTime): String = if (now.hour < 12) "AM" else "PM"

    /** 例：`2026.10.13`。 */
    fun date(now: LocalDateTime): String =
        "%04d.%02d.%02d".format(now.year, now.monthValue, now.dayOfMonth)

    /** 例：`TUE`。 */
    fun weekday(now: LocalDateTime): String =
        now.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH)

    /** 例：`2026.10.13 TUE`。 */
    fun dateLine(now: LocalDateTime): String = "${date(now)} ${weekday(now)}"
}
