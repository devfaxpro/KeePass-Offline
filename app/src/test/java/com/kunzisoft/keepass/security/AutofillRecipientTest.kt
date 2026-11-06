package com.kunzisoft.keepass.security

import android.app.assist.AssistStructure
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.view.View
import android.view.autofill.AutofillId
import androidx.test.core.app.ApplicationProvider
import com.kunzisoft.keepass.credentialprovider.autofill.AutofillRecipient
import com.kunzisoft.keepass.credentialprovider.autofill.StructureParser
import com.kunzisoft.keepass.credentialprovider.passkey.data.AndroidPrivilegedApp
import com.kunzisoft.keepass.credentialprovider.passkey.util.PrivilegedAllowLists
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27], application = android.app.Application::class)
class AutofillRecipientTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val packageName = "com.example.browser"

    @Test fun libreIncludesOfflineBrowserTrustWithoutCustomConfiguration() {
        PrivilegedAllowLists.saveCustomPrivilegedApps(context, emptyList())
        val apps = AutofillRecipient.trustedApps(context)
        for (browser in listOf("com.android.chrome", "org.mozilla.firefox", "org.cromite.cromite")) {
            assertTrue(browser, apps.any { it.packageName == browser && it.fingerprints.isNotEmpty() })
        }
        assertFalse(apps.any { it.packageName == "com.google.android.gms" })
    }

    private fun <T> empty(type: Class<T>): T {
        val unsafe = Class.forName("sun.misc.Unsafe")
        val field = unsafe.getDeclaredField("theUnsafe").apply { isAccessible = true }
        return type.cast(unsafe.getMethod("allocateInstance", Class::class.java).invoke(field.get(null), type))
    }

    private fun node(domain: String, visible: Boolean = true): AssistStructure.ViewNode =
        empty(AssistStructure.ViewNode::class.java).apply {
            ReflectionHelpers.setField(this, "mWebDomain", domain)
            ReflectionHelpers.setField(this, "mFlags", if (visible) View.VISIBLE else View.GONE)
            ReflectionHelpers.setField(this, "mAutofillId", ReflectionHelpers.callConstructor(AutofillId::class.java,
                ReflectionHelpers.ClassParameter.from(Int::class.javaPrimitiveType, 42)))
            ReflectionHelpers.setField(this, "mAutofillHints", arrayOf(View.AUTOFILL_HINT_PASSWORD))
        }

    private fun structure(vararg nodes: AssistStructure.ViewNode) = AssistStructure().apply {
        ReflectionHelpers.setField(this, "mActivityComponent", ComponentName(packageName, "$packageName.Login"))
        val windows = nodes.map { root ->
            empty(AssistStructure.WindowNode::class.java).apply {
                ReflectionHelpers.setField(this, "mTitle", "com.bank.official/Login")
                ReflectionHelpers.setField(this, "mRoot", root)
            }
        }
        ReflectionHelpers.setField(this, "mWindowNodes", ArrayList(windows))
    }

    private fun trustBrowser(installed: ByteArray = byteArrayOf(1, 2, 3)) {
        val expected = byteArrayOf(1, 2, 3)
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(expected).joinToString(":") { "%02X".format(it) }
        assertTrue(PrivilegedAllowLists.saveCustomPrivilegedApps(context,
            listOf(AndroidPrivilegedApp(packageName, setOf(fingerprint)))))
        shadowOf(context.packageManager).installPackage(PackageInfo().apply {
            this.packageName = this@AutofillRecipientTest.packageName
            @Suppress("DEPRECATION")
            signatures = arrayOf(Signature(installed))
        })
    }

    @Test fun untrustedDomainAndForgedTitleCannotChooseWebsiteOrPackage() {
        PrivilegedAllowLists.saveCustomPrivilegedApps(context, emptyList())
        for (save in listOf(false, true)) {
            val parsed = StructureParser(structure(node("bank.example")), context = context).parse(save)!!
            assertEquals(packageName, parsed.applicationId)
            assertNull(parsed.webDomain)
            assertNull(parsed.webScheme)
            assertNotNull(parsed.passwordId)
        }
    }

    @Test fun trustedBrowserNeedsTheExpectedInstalledCertificate() {
        trustBrowser()
        assertEquals("bank.example", StructureParser(structure(node("bank.example")), context = context).parse(false)!!.webDomain)
        trustBrowser(byteArrayOf(9, 9, 9))
        assertNull(StructureParser(structure(node("bank.example")), context = context).parse(false)!!.webDomain)
    }

    @Test fun conflictingVisibleDomainsAreRejectedAndHiddenNodesIgnored() {
        trustBrowser()
        assertNull(StructureParser(structure(node("bank.example"), node("evil.example")), context = context).parse(false))
        assertEquals("bank.example", StructureParser(structure(node("bank.example"), node("evil.example", false)), context = context).parse(false)!!.webDomain)
    }

    @Test fun multipleSignersRequireAllCertificatesAndRotationNeedsATrustedAncestor() {
        assertFalse(AutofillRecipient.matchesSigners(listOf("trusted", "other"), setOf("trusted"), true))
        assertTrue(AutofillRecipient.matchesSigners(listOf("trusted", "other"), setOf("trusted", "other"), true))
        assertTrue(AutofillRecipient.matchesSigners(listOf("old", "new"), setOf("old"), false))
        assertFalse(AutofillRecipient.matchesSigners(emptyList(), setOf("trusted"), false))
    }

    @Test @Config(sdk = [28])
    fun modernPackageManagerChecksCurrentSignersAndVerifiedRotationHistory() {
        trustBrowser()
        val signing = ReflectionHelpers.callConstructor(SigningInfo::class.java)
        val shadow = shadowOf(signing)
        fun install() {
            shadowOf(context.packageManager).installPackage(PackageInfo().apply {
                packageName = this@AutofillRecipientTest.packageName
                signingInfo = signing
            })
        }
        val trusted = Signature(byteArrayOf(1, 2, 3))
        val other = Signature(byteArrayOf(9, 9, 9))
        shadow.setSignatures(arrayOf(trusted))
        install()
        assertTrue(AutofillRecipient.canAssertWebDomain(context, packageName))
        shadow.setSignatures(arrayOf(trusted, other))
        install()
        assertFalse(AutofillRecipient.canAssertWebDomain(context, packageName))
        shadow.setSignatures(arrayOf(other))
        shadow.setPastSigningCertificates(arrayOf(trusted, other))
        install()
        assertTrue(AutofillRecipient.canAssertWebDomain(context, packageName))
        assertFalse(AutofillRecipient.canAssertWebDomain(context, "com.other.browser"))
    }
}
