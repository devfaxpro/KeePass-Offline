package com.kunzisoft.keepass.timeout

/** Tracks ownership without retaining password text. A null read means access is unavailable. */
internal class ClipboardExpiry(
    private val readToken: () -> String?,
    private val clear: () -> Boolean
) {
    private var token: String? = null
    private var expired = false

    fun copied(newToken: String) {
        token = newToken
        expired = false
    }

    fun expire(expectedToken: String) {
        if (token != expectedToken) return
        expired = true
        retry()
    }

    fun retry() {
        val expected = token ?: return
        if (!expired) return
        val observed = readToken() ?: return
        if (observed != expected || clear()) {
            token = null
            expired = false
        }
    }
}
