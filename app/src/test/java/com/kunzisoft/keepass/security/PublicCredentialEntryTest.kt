package com.kunzisoft.keepass.security

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.kunzisoft.keepass.activities.*
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addSearchInfo
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addSpecialMode
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addTypeMode
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveSearchInfo
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveSpecialMode
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveTypeMode
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.credentialprovider.TypeMode
import com.kunzisoft.keepass.model.SearchInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = com.kunzisoft.keepass.app.App::class)
class PublicCredentialEntryTest {
    private fun request(type: TypeMode) = Intent(Intent.ACTION_VIEW, Uri.parse("content://example/vault.kdbx"))
        .addSpecialMode(SpecialMode.SELECTION).addTypeMode(type)
        .addSearchInfo(SearchInfo().apply { applicationId = "com.example.bank" })

    private fun assertSanitized(intent: Intent) {
        assertEquals(SpecialMode.DEFAULT, intent.retrieveSpecialMode())
        assertEquals(TypeMode.DEFAULT, intent.retrieveTypeMode())
        assertNull(intent.retrieveSearchInfo())
        assertEquals(Uri.parse("content://example/vault.kdbx"), intent.data)
    }

    @Test fun exportedFilePickerRejectsFreshAndReusedCredentialRequests() {
        for (type in listOf(TypeMode.PASSWORD, TypeMode.PASSKEY, TypeMode.AUTOFILL)) {
            val first = request(type)
            val controller = Robolectric.buildActivity(FileDatabaseSelectActivity::class.java, first).create()
            try {
                assertSanitized(controller.get().intent)
                val repeated = request(type)
                controller.newIntent(repeated)
                assertSanitized(repeated)
            } finally { controller.destroy() }
        }
    }

    @Test fun exportedUnlockScreenRejectsCredentialRequestsButKeepsDatabaseUri() {
        val first = request(TypeMode.PASSKEY)
        val controller = Robolectric.buildActivity(MainCredentialActivity::class.java, first).create()
        try {
            assertSanitized(controller.get().intent)
            val repeated = request(TypeMode.PASSWORD)
            controller.newIntent(repeated)
            assertSanitized(repeated)
        } finally { controller.destroy() }
    }

    @Test fun internalScreensKeepSelectionAndAreNotExported() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (activity in listOf(InternalFileDatabaseSelectActivity::class.java, InternalMainCredentialActivity::class.java)) {
            assertFalse(context.packageManager.getActivityInfo(ComponentName(context, activity), 0).exported)
            val controller = Robolectric.buildActivity(activity, request(TypeMode.PASSWORD)).create()
            try {
                assertEquals(SpecialMode.SELECTION, controller.get().intent.retrieveSpecialMode())
                assertEquals(TypeMode.PASSWORD, controller.get().intent.retrieveTypeMode())
            } finally { controller.destroy() }
        }
    }
}
