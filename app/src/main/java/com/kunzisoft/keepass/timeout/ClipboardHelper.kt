/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.timeout

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.PreferencesUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.UUID

private const val TAG = "ClipboardHelper"
private const val DEFAULT_LABEL = ""
private const val CLIP_TOKEN = "com.kunzisoft.keepass.CLIP_TOKEN"

private object ClipboardExpiration {
    var job: Job? = null
    private var tracker: ClipboardExpiry? = null

    fun tracker(context: Context): ClipboardExpiry = tracker ?: ClipboardExpiry(
        readToken = {
            try {
                context.clipboardManager()?.primaryClip?.description?.let { description ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        description.extras?.getString(CLIP_TOKEN) ?: ""
                    else description.label?.toString() ?: ""
                }
            } catch (_: Exception) { null }
        },
        clear = { context.clearOwnedClipboard() }
    ).also { tracker = it }

    fun retry() { tracker?.retry() }
}

/** Android may deny clipboard reads in background. Retry only when a window regains focus. */
fun retryExpiredClipboard() = ClipboardExpiration.retry()

private fun Context.clipboardManager() = ContextCompat.getSystemService(
    applicationContext,
    ClipboardManager::class.java
)

/**
 * Copy value to clipboard and clear it after a timeout.
 * @param label The label of the clipboard entry.
 * @param value The value to copy.
 * @param sensitive Whether the value is sensitive.
 */
fun Context.timeoutCopyToClipboard(
    label: String,
    value: CharSequence,
    sensitive: Boolean = false
) {
    val token: String
    try {
        token = copyOwnedClip(label, value, sensitive)
    } catch (e: Exception) {
        Log.e(TAG, "Unable to copy to clipboard", e)
        Toast.makeText(
            applicationContext,
            applicationContext.getString(R.string.clipboard_error_title),
            Toast.LENGTH_LONG
        ).show()
        return
    }

    val clipboardTimeout = PreferencesUtil.getClipboardTimeout(applicationContext)
    if (clipboardTimeout > 0) {
        val tracker = ClipboardExpiration.tracker(applicationContext)
        ClipboardExpiration.job = ProcessLifecycleOwner.get().lifecycleScope.launch {
            delay(clipboardTimeout)
            tracker.expire(token)
        }
    }
}

/**
 * Copy value to clipboard and clear it after a timeout.
 * @param label The label of the clipboard entry.
 * @param value The value to copy.
 * @param sensitive Whether the value is sensitive.
 */
fun Context.timeoutCopyToClipboard(
    label: String,
    value: CharArray?,
    sensitive: Boolean = false
) {
    if (value == null) return
    timeoutCopyToClipboard(label, String(value), sensitive)
}

/**
 * Copy value to clipboard.
 * @param label The label of the clipboard entry.
 * @param value The value to copy.
 * @param sensitive Whether the value is sensitive.
 */
fun Context.copyToClipboard(
    label: String,
    value: CharSequence,
    sensitive: Boolean = false
) {
    copyOwnedClip(label, value, sensitive)
}

private fun Context.copyOwnedClip(label: String, value: CharSequence, sensitive: Boolean): String {
    val token = UUID.randomUUID().toString()
    val manager = clipboardManager() ?: throw IllegalStateException("Clipboard unavailable")
    manager.setPrimaryClip(
        ClipData.newPlainText(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) DEFAULT_LABEL else token, value).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                description.extras = PersistableBundle().apply {
                    putBoolean("android.content.extra.IS_SENSITIVE", sensitive)
                    putString(CLIP_TOKEN, token)
                }
            }
        },
    )
    ClipboardExpiration.job?.cancel()
    ClipboardExpiration.tracker(applicationContext).copied(token)
    if (label.isNotEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2) {
        Toast.makeText(
            applicationContext,
            applicationContext.getString(R.string.copy_field, label),
            Toast.LENGTH_LONG
        ).show()
    }
    return token
}

/**
 * Copy value to clipboard.
 * @param label The label of the clipboard entry.
 * @param value The value to copy.
 * @param sensitive Whether the value is sensitive.
 */
fun Context.copyToClipboard(
    label: String,
    value: CharArray?,
    sensitive: Boolean = false
) {
    if (value == null) return
    copyToClipboard(label, String(value), sensitive)
}

/**
 * Clean the clipboard.
 */
private fun Context.clearOwnedClipboard(): Boolean {
    return try {
        val manager = clipboardManager() ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.clearPrimaryClip()
        } else {
            manager.setPrimaryClip(ClipData.newPlainText(DEFAULT_LABEL, ""))
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Unable to clean the clipboard", e)
        false
    }
}
