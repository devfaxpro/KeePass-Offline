package com.kunzisoft.keepass.database

import android.content.Context
import android.net.Uri
import java.security.MessageDigest

/** Memorizza solo la data di una copia riuscita, separatamente per ogni database. */
object BackupHistory {
    private fun key(uri: Uri): String = MessageDigest.getInstance("SHA-256")
        .digest(uri.toString().toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    fun record(context: Context, source: Uri?, destination: Uri?, timestamp: Long = System.currentTimeMillis()) {
        if (source == null || destination == null || source == destination) return
        context.getSharedPreferences("backup_history", Context.MODE_PRIVATE)
            .edit().putLong(key(source), timestamp).apply()
    }

    fun lastSuccess(context: Context, source: Uri?): Long = source?.let {
        context.getSharedPreferences("backup_history", Context.MODE_PRIVATE).getLong(key(it), 0)
    } ?: 0
}
