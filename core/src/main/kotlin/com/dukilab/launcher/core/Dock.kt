package com.dukilab.launcher.core

/** 画面下のドック。よく使うアプリを最大 [MAX] 個まで並べる。 */
data class Dock(val appKeys: List<String> = emptyList()) {

    val isFull: Boolean get() = appKeys.size >= MAX

    fun contains(appKey: String): Boolean = appKey in appKeys

    /** 末尾に追加する。すでにあるか満杯なら何もしない。 */
    fun add(appKey: String): Dock =
        if (contains(appKey) || isFull) this else Dock(appKeys + appKey)

    fun remove(appKey: String): Dock = Dock(appKeys - appKey)

    /** [from] 番目を [to] 番目へ動かす。 */
    fun move(from: Int, to: Int): Dock {
        if (from !in appKeys.indices || to !in appKeys.indices || from == to) return this
        return Dock(appKeys.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun retainInstalled(installed: Set<String>): Dock = Dock(appKeys.filter { it in installed })

    companion object {
        const val MAX = 5
    }
}
