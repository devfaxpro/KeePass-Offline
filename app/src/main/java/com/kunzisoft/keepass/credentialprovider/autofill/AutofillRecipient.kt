package com.kunzisoft.keepass.credentialprovider.autofill

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.kunzisoft.keepass.credentialprovider.passkey.data.AndroidPrivilegedApp
import com.kunzisoft.keepass.credentialprovider.passkey.util.PrivilegedAllowLists
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

/** Only explicitly trusted, signed browsers may assert an arbitrary website identity. */
internal object AutofillRecipient {
    internal fun trustedApps(context: Context): List<AndroidPrivilegedApp> {
        val bundled = context.assets.open("autofill_trusted_browsers.json").bufferedReader().use {
            AndroidPrivilegedApp.extractPrivilegedApps(JSONObject(it.readText()))
        }
        return bundled + PrivilegedAllowLists.retrieveCustomPrivilegedApps(context)
    }

    @Suppress("DEPRECATION")
    fun canAssertWebDomain(context: Context, packageName: String): Boolean = try {
        val allowed = trustedApps(context)
            .filter { it.packageName == packageName }
            .flatMap { it.fingerprints }.map { it.uppercase(Locale.ROOT) }.toSet()
        if (allowed.isEmpty()) {
            false
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = context.packageManager.getPackageInfo(
                packageName, PackageManager.GET_SIGNING_CERTIFICATES
            ).signingInfo
            if (signing == null) false else {
                val multiple = signing.hasMultipleSigners()
                val certificates = if (multiple) signing.apkContentsSigners
                    else signing.signingCertificateHistory
                matchesSigners(certificates?.map { fingerprint(it.toByteArray()) }.orEmpty(), allowed, multiple)
            }
        } else {
            val signatures = context.packageManager.getPackageInfo(
                packageName, PackageManager.GET_SIGNATURES
            ).signatures
            matchesSigners(signatures?.map { fingerprint(it.toByteArray()) }.orEmpty(), allowed, true)
        }
    } catch (_: Exception) {
        false
    }

    internal fun matchesSigners(signers: List<String>, allowed: Set<String>, multiple: Boolean): Boolean {
        if (signers.isEmpty()) return false
        // Multiple current signers must all be trusted. Android verifies single-signer rotation lineage.
        return if (multiple) signers.all { it in allowed } else signers.any { it in allowed }
    }

    private fun fingerprint(certificate: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(certificate)
            .joinToString(":") { "%02X".format(it) }
}
