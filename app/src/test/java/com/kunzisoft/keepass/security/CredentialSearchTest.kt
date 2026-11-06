package com.kunzisoft.keepass.security

import android.content.Context
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.database.helper.SearchHelper.getSearchParametersFromSearchInfo
import com.kunzisoft.keepass.model.SearchInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class CredentialSearchTest {
    @Test fun automaticLookupUsesTheCompleteCaseSensitivePackageName() {
        val entry = Entry(EntryKDBX()).apply {
            putExtraField(Field("AndroidApp", ProtectedString(false, "com.example.bank")))
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        for ((query, expected) in mapOf("com.example.bank" to true, "com.example" to false,
            "example.bank" to false, "com.EXAMPLE.bank" to false, "com.example.bank.evil" to false)) {
            val info = SearchInfo().apply { applicationId = query }
            var checked = false
            info.getSearchParametersFromSearchInfo(context) { params ->
                assertEquals(query, expected, SearchHelper.searchInEntry(entry, params))
                val copy = params.copy()
                val parcel = Parcel.obtain()
                try {
                    copy.writeToParcel(parcel, 0)
                    parcel.setDataPosition(0)
                    assertEquals(expected, SearchHelper.searchInEntry(entry, SearchParameters(parcel)))
                } finally { parcel.recycle() }
                checked = true
            }
            assertTrue(checked)
        }
    }

    @Test fun ordinaryManualPartialSearchRemainsAvailable() {
        val entry = Entry(EntryKDBX()).apply {
            putExtraField(Field("AndroidApp", ProtectedString(false, "com.example.bank")))
        }
        assertTrue(SearchHelper.searchInEntry(entry, SearchParameters().apply { searchQuery = "example" }))
    }
}
