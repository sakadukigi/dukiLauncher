package com.dukilab.launcher.system

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ダブルタップで画面を消すためのユーザー補助サービス。
 * 画面の内容は読まず、画面ロックの操作だけに使う。
 */
class ScreenOffService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _ready.value = true
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        _ready.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        _ready.value = false
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        private var instance: ScreenOffService? = null
        private val _ready = MutableStateFlow(false)

        /** サービスが有効になっているか。 */
        val ready: StateFlow<Boolean> = _ready.asStateFlow()

        /** 画面を消す。サービスが有効でなければ false。 */
        fun lockScreen(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false

        /** 通知パネルを開く。サービスが有効でなければ false。 */
        fun openNotifications(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false
    }
}
