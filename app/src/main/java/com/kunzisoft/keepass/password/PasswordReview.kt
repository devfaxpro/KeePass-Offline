package com.kunzisoft.keepass.password

import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.EntryId
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.coroutineContext

/** Analisi locale: nessuna password o impronta viene salvata, registrata o inviata. */
object PasswordReview {
    data class Item(val id: EntryId, val title: String, val shortOrRepeated: Boolean, var reused: Boolean = false)
    data class Result(val checked: Int, val items: List<Item>)

    // È un criterio esplicito e limitato, non una misura della sicurezza complessiva.
    fun isShortOrRepeated(value: CharArray): Boolean {
        if (value.isEmpty()) return false
        if (Character.codePointCount(value, 0, value.size) < 12) return true
        val first = Character.codePointAt(value, 0)
        var offset = Character.charCount(first)
        while (offset < value.size) {
            val next = Character.codePointAt(value, offset)
            if (next != first) return false
            offset += Character.charCount(next)
        }
        return true
    }

    suspend fun inspect(database: ContextualDatabase): Result {
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }
        key.fill(0)
        val fingerprints = mutableMapOf<String, Item>()
        val items = mutableListOf<Item>()
        val groups = java.util.ArrayDeque<com.kunzisoft.keepass.database.element.Group>()
        database.rootGroup?.let { groups.add(it) }
        var checked = 0
        try {
            while (groups.isNotEmpty()) {
                coroutineContext.ensureActive()
                if (!database.loaded) break
                val group = groups.removeFirst()
                if (group == database.recycleBin || group == database.templatesGroup) continue
                groups.addAll(group.getChildGroups())
                for (entry in group.getChildEntries()) {
                    coroutineContext.ensureActive()
                    if (!database.loaded) break
                    val password = entry.password.copyOf()
                    try {
                        if (password.isEmpty()) continue
                        // Codifica UTF-16 esplicita senza creare una String contenente il segreto.
                        password.forEach { mac.update((it.code ushr 8).toByte()); mac.update(it.code.toByte()) }
                        val digest = mac.doFinal()
                        val fingerprint = digest.joinToString("") { "%02x".format(it) }
                        digest.fill(0)
                        val item = Item(entry.nodeId, entry.title, isShortOrRepeated(password))
                        fingerprints[fingerprint]?.let { previous -> previous.reused = true; item.reused = true }
                            ?: run { fingerprints[fingerprint] = item }
                        items.add(item)
                        checked++
                    } finally {
                        password.fill('\u0000')
                    }
                    // Mantiene reattivi sia l'interfaccia sia il blocco automatico del database.
                    if (checked % 16 == 0) yield()
                }
                yield()
            }
            return Result(checked, items.filter { it.shortOrRepeated || it.reused })
        } finally {
            fingerprints.clear()
            groups.clear()
            mac.reset()
        }
    }
}
