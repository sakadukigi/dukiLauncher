package com.dukilab.launcher.system

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.dukilab.launcher.core.Shortcut
import com.dukilab.launcher.data.AppRepository

/** Android の機能を呼び出す（アプリの起動・アプリ情報・アンインストール・通知パネルなど）。 */
class SystemBridge(
    context: Context,
    private val repo: AppRepository,
    private val shortcutOf: (String) -> Shortcut?,
) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(android.content.pm.LauncherApps::class.java)

    fun launch(appKey: String) {
        shortcutOf(appKey)?.let { return launchShortcut(it) }
        val component = repo.component(appKey) ?: return toast("アプリが見つかりません")
        val user = repo.user(appKey) ?: return toast("アプリが見つかりません")
        try {
            launcherApps.startMainActivity(component, user, null, null)
        } catch (e: ActivityNotFoundException) {
            toast("アプリを開けませんでした")
        } catch (e: SecurityException) {
            toast("アプリを開けませんでした")
        }
    }

    private fun launchShortcut(sc: Shortcut) {
        if (sc.isWeb) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sc.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            start(intent, "ページを開けませんでした")
            return
        }
        val user = repo.userForSerial(sc.userSerial) ?: return toast("ショートカットを開けませんでした")
        try {
            launcherApps.startShortcut(sc.packageName, sc.shortcutId, null, null, user)
        } catch (e: ActivityNotFoundException) {
            toast("ショートカットを開けませんでした")
        } catch (e: IllegalStateException) {
            toast("ショートカットを開けませんでした")
        } catch (e: SecurityException) {
            toast("ショートカットを開くには、dukiLauncher を既定のホームアプリにしてください")
        }
    }

    fun openAppInfo(appKey: String) {
        val component = repo.component(appKey) ?: return
        val user = repo.user(appKey) ?: return
        runCatching { launcherApps.startAppDetailsActivity(component, user, null, null) }
    }

    fun uninstall(appKey: String) {
        val component = repo.component(appKey) ?: return
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", component.packageName, null))
            .putExtra(Intent.EXTRA_USER, repo.user(appKey))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        start(intent, "アンインストール画面を開けませんでした")
    }

    /** 通知パネルを開く。ユーザー補助サービスが有効ならそちらを使う。 */
    @SuppressLint("WrongConstant")
    fun expandNotifications() {
        if (ScreenOffService.openNotifications()) return
        runCatching {
            val service = appContext.getSystemService("statusbar")
            service.javaClass.getMethod("expandNotificationsPanel").invoke(service)
        }
    }

    fun lockScreen() {
        if (!ScreenOffService.lockScreen()) {
            toast("画面を消すには、ユーザー補助で dukiLauncher を有効にしてください")
            openAccessibilitySettings()
        }
    }

    fun isDefaultHome(): Boolean {
        val roles = appContext.getSystemService(RoleManager::class.java) ?: return true
        return runCatching { roles.isRoleHeld(RoleManager.ROLE_HOME) }.getOrDefault(true)
    }

    fun openHomeSettings() {
        start(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "設定を開けませんでした")
    }

    fun openAccessibilitySettings() {
        start(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "設定を開けませんでした")
    }

    private fun start(intent: Intent, failure: String) {
        try {
            appContext.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(failure)
        }
    }

    private fun toast(text: String) {
        Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()
    }
}
