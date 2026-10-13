package com.dukilab.launcher

import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.dukilab.launcher.core.Shortcut
import com.dukilab.launcher.data.LauncherStore
import kotlinx.coroutines.launch

/**
 * ほかのアプリからの「ホーム画面に追加」（Chrome の Web ページなど）を受け取り、
 * ショートカットをホーム画面の空いているところに置く。画面は出さない。
 */
class AddShortcutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launcherApps = getSystemService(LauncherApps::class.java)
        val request = runCatching { launcherApps.getPinItemRequest(intent) }.getOrNull()
        val info = request?.shortcutInfo
        if (request == null || request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT ||
            info == null || !request.isValid
        ) {
            finish()
            return
        }
        val serial = getSystemService(UserManager::class.java).getSerialNumberForUser(info.userHandle)
        val label = (info.shortLabel ?: info.longLabel)?.toString()?.trim().orEmpty().ifEmpty { info.`package` }
        val shortcut = Shortcut(
            key = Shortcut.pinnedKey(info.`package`, info.id, serial),
            label = label,
            packageName = info.`package`,
            shortcutId = info.id,
            userSerial = serial,
        )
        if (!runCatching { request.accept() }.getOrDefault(false)) {
            Toast.makeText(this, "ホーム画面に追加できませんでした", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        lifecycleScope.launch {
            LauncherStore(applicationContext).updateSaved { it.addShortcut(shortcut) }
            Toast.makeText(applicationContext, "「$label」をホーム画面に置きました", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
