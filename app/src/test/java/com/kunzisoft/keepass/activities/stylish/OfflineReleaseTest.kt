package com.kunzisoft.keepass.activities.stylish

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.BackupHistory
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.password.PasswordReview
import com.kunzisoft.keepass.settings.AppLanguage
import com.kunzisoft.keepass.view.TemplateAbstractView
import com.kunzisoft.keepass.view.TemplateEditView
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class OfflineReleaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun passwordCriteriaCountUnicodeCharactersInsteadOfUtf16Units() {
        assertTrue(PasswordReview.isShortOrRepeated("🙂".repeat(12).toCharArray()))
        assertTrue(PasswordReview.isShortOrRepeated("🙂abc".toCharArray()))
        assertFalse(PasswordReview.isShortOrRepeated("abcdefghijkl".toCharArray()))
        assertFalse(PasswordReview.isShortOrRepeated(charArrayOf()))
    }

    @Test fun regionalLanguageChoicesSurviveReload() {
        for (tag in listOf("it", "en", "en-US", "pt-BR", "pt-PT", "es", "es-419", "zh-Hans", "zh-Hant", "ur", "id")) {
            assertEquals(tag, AppLanguage.selection(Locale.forLanguageTag(tag)))
        }
        assertEquals("zh-Hant", AppLanguage.selection(Locale.TAIWAN))
        assertEquals("es-419", AppLanguage.selection(Locale("es", "MX")))
        val codes = context.resources.getStringArray(R.array.modern_language_values).toList()
        assertEquals(26, codes.size)
        assertEquals(26, codes.distinct().size)
        assertEquals(listOf("it", "en", "en-US", "es", "pt-BR", "fr", "de", "zh-Hans", "ja", "ko", "ru", "hi", "ar", "id", "bn", "ur", "tr", "vi", "th", "pl", "nl", "ro", "uk", "pt-PT", "es-419", "zh-Hant"), codes)
    }

    @Test fun backupHistoryIsScopedToDatabaseAndRequiresDistinctCopy() {
        val source = Uri.parse("content://local/source.kdbx")
        val copy = Uri.parse("content://local/copy.kdbx")
        context.getSharedPreferences("backup_history", Context.MODE_PRIVATE).edit().clear().commit()
        BackupHistory.record(context, source, source, 100)
        BackupHistory.record(context, source, null, 100)
        assertEquals(0L, BackupHistory.lastSuccess(context, source))
        BackupHistory.record(context, source, copy, 200)
        assertEquals(200L, BackupHistory.lastSuccess(context, source))
        assertEquals(0L, BackupHistory.lastSuccess(context, copy))
    }

    @Test fun reviewFindsEveryReuseWithoutChangingStoredPasswords() = runBlocking {
        val database = ContextualDatabase().apply { createData("test", "root", "templates"); loaded = true }
        fun add(title: String, value: String): Entry = database.createEntry()!!.apply {
            this.title = title; password = value.toCharArray(); database.addEntryTo(this, database.rootGroup!!)
        }
        val original = "long unique passphrase 2026"
        val first = add("first", original)
        add("second", original); add("third", original)
        add("short", "abc"); add("empty", ""); add("unique", "another long passphrase")
        val template = database.createEntry()!!.apply { password = "a".toCharArray() }
        database.templatesGroup?.let { database.addEntryTo(template, it) }
        val result = PasswordReview.inspect(database)
        assertEquals(5, result.checked)
        assertEquals(4, result.items.size)
        assertEquals(3, result.items.count { it.reused })
        assertArrayEquals(original.toCharArray(), first.password)
        assertEquals(1, result.items.count { it.shortOrRepeated })
    }

    @Test fun collapsedFieldsKeepTheirDataAndExistingNotesRemainVisible() {
        val themed = androidx.appcompat.view.ContextThemeWrapper(context, R.style.KeepassDXStyle_Blue)
        val view = TemplateEditView(themed)
        val info = EntryInfo().apply { template = Template.STANDARD; title = "Example"; notes = "Keep this note" }
        view.setEntryInfo(info)
        val notes = view.findViewWithTag<View>(TemplateAbstractView.FIELD_NOTES_TAG)
        assertEquals(View.VISIBLE, notes.visibility)
        view.findViewWithTag<View>("advanced_toggle").performClick()
        assertEquals(View.GONE, notes.visibility)
        assertEquals("Keep this note", view.getEntryInfo().notes)
        view.findViewWithTag<View>("advanced_toggle").performClick()
        assertEquals(View.VISIBLE, notes.visibility)
    }
}
