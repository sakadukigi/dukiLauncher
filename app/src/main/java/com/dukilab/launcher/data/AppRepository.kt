package com.dukilab.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.dukilab.launcher.core.AppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

/**
 * 端末に入っているアプリ（ホーム画面から起動できるもの）を読み込む。
 * 仕事用プロフィールのアプリも含める。インストール・削除・更新があれば読み直す。
 */
class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)

    private val infos = ConcurrentHashMap<String, LauncherActivityInfo>()

    /** アプリ一覧。変化があるたびに新しい一覧が流れる。 */
    val apps: Flow<List<AppEntry>> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageAdded(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageChanged(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) { trySend(Unit) }
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) { trySend(Unit) }
        }
        trySend(Unit)
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }.conflate().map { load() }.flowOn(Dispatchers.IO)

    /** 起動・アイコン用に、キーからアクティビティ情報を引く。 */
    fun info(key: String): LauncherActivityInfo? = infos[key]

    fun component(key: String): ComponentName? = infos[key]?.componentName

    fun user(key: String): UserHandle? = infos[key]?.user

    fun isMainUser(key: String): Boolean = infos[key]?.user == Process.myUserHandle()

    /** 保存しておいたユーザー番号から、そのユーザーを引く（いなければ null）。 */
    fun userForSerial(serial: Long): UserHandle? = userManager.getUserForSerialNumber(serial)

    private fun load(): List<AppEntry> {
        val result = mutableListOf<AppEntry>()
        val fresh = mutableMapOf<String, LauncherActivityInfo>()
        for (user in userManager.userProfiles) {
            val serial = userManager.getSerialNumberForUser(user)
            val list = runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
            for (info in list) {
                if (info.componentName.packageName == appContext.packageName) continue
                val key = AppEntry.keyOf(info.componentName.packageName, info.componentName.className, serial)
                val baseLabel = info.label?.toString()?.trim().orEmpty().ifEmpty { info.componentName.packageName }
                val label = if (user == Process.myUserHandle()) baseLabel
                else appContext.packageManager.getUserBadgedLabel(baseLabel, user).toString()
                val flags = info.applicationInfo.flags
                fresh[key] = info
                result += AppEntry(
                    key = key,
                    label = label,
                    packageName = info.componentName.packageName,
                    isSystemApp = flags and ApplicationInfo.FLAG_SYSTEM != 0,
                )
            }
        }
        infos.keys.retainAll(fresh.keys)
        infos.putAll(fresh)
        return result
    }
}
