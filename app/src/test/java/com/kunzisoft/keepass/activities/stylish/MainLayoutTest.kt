/* Local Android redesign. SPDX-License-Identifier: GPL-3.0-or-later */
package com.kunzisoft.keepass.activities.stylish

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.kunzisoft.keepass.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class MainLayoutTest {
    @Test fun databaseActionsRemainVisibleWithLargeTextInEveryLanguage() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        for (tag in base.resources.getStringArray(R.array.modern_language_values)) {
            for (scale in listOf(1.0f, 2.0f)) {
                val config = Configuration(base.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(tag))
                    fontScale = scale
                    screenWidthDp = 320
                }
                for (night in listOf(false, true)) {
                    val themed = ContextThemeWrapper(base.createConfigurationContext(config),
                        AppearancePalette.resolve("blue", night)!!)
                    val root = LayoutInflater.from(themed).inflate(R.layout.activity_file_selection, null)
                    val density = themed.resources.displayMetrics.density
                    val width = (320 * density).toInt()
                    val height = (640 * density).toInt()
                    root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                    root.layout(0, 0, width, height)
                    val open = root.findViewById<TextView>(R.id.open_database_button)
                    val create = root.findViewById<TextView>(R.id.create_database_button)
                    val panel = root.findViewById<View>(R.id.file_selection_buttons_container)
                    for (button in listOf(open, create)) {
                        assertTrue("$tag scale=$scale touch target", button.height >= 48 * density)
                        assertTrue("$tag scale=$scale width", button.width >= 240 * density)
                        val textHeight = button.layout.height
                        assertTrue("$tag scale=$scale clipped label",
                            textHeight <= button.height - button.compoundPaddingTop - button.compoundPaddingBottom)
                    }
                    assertTrue("$tag actions overlap", open.bottom <= create.top)
                    assertTrue("$tag footer clipped", panel.top >= 0 && panel.bottom <= height)
                    val content = root.findViewById<View>(R.id.activity_file_selection_coordinator_layout)
                    assertTrue("$tag content obscured", content.height > 0 && content.bottom <= panel.top)
                }
            }
        }
    }

    @Test fun searchAndSettingsFitSmallScreensInEverySupportedLanguage() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        for (tag in base.resources.getStringArray(R.array.modern_language_values)) {
            val config = Configuration(base.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(tag))
                fontScale = 2.0f
                screenWidthDp = 320
            }
            for (night in listOf(false, true)) {
                val themed = ContextThemeWrapper(base.createConfigurationContext(config),
                    AppearancePalette.resolve("blue", night)!!)
                val root = LayoutInflater.from(themed).inflate(R.layout.activity_group, null)
                val density = themed.resources.displayMetrics.density
                val width = (320 * density).toInt()
                val height = (640 * density).toInt()
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                root.layout(0, 0, width, height)
                val search = root.findViewById<TextView>(R.id.vault_search)
                val settings = root.findViewById<View>(R.id.vault_settings)
                assertTrue("$tag search height", search.height >= 48 * density)
                assertTrue("$tag settings target", settings.height >= 48 * density)
                assertTrue("$tag search width", search.width > 0)
                assertTrue("$tag action overlap", search.right <= settings.left)
                assertTrue("$tag actions out of bounds", settings.right <= width)
                assertEquals(themed.getString(R.string.modern_search), search.text.toString())
            }
        }
    }
}
