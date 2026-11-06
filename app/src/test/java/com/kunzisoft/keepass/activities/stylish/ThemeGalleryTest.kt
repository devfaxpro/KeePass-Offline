/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.kunzisoft.keepass.activities.stylish

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.card.MaterialCardView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.preference.ThemeGalleryPreference
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class ThemeGalleryTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clear() { PreferenceManager.getDefaultSharedPreferences(base).edit().clear().commit() }

    private fun preference(context: Context): ThemeGalleryPreference {
        val manager = PreferenceManager(context)
        return ThemeGalleryPreference(context).apply {
            key = context.getString(R.string.modern_palette_key)
            entries = context.resources.getStringArray(R.array.modern_palette_names)
            entryValues = context.resources.getStringArray(R.array.modern_palette_values)
            manager.createPreferenceScreen(context).addPreference(this)
        }
    }

    @Test fun tappingPreviewUsesExistingChangeCallbackAndPersistsSelection() {
        val controller = Robolectric.buildActivity(PaletteProbeActivity::class.java).setup()
        try {
        val context = controller.get()
        val pref = preference(context)
        pref.value = "blue"
        val root = LayoutInflater.from(context).inflate(R.layout.preference_theme_gallery, null)
        val holder = PreferenceViewHolder.createInstanceForTests(root)
        pref.onBindViewHolder(holder)
        context.setContentView(root)
        controller.visible()
        var change: Any? = null
        pref.setOnPreferenceChangeListener { _, new -> change = new; true }
        assertTrue(root.findViewWithTag<View>("purple").performClick())
        assertEquals("purple", change)
        assertEquals("purple", AppearancePalette.selected(context))
        pref.onBindViewHolder(holder)
        assertTrue(root.findViewWithTag<View>("purple").createAccessibilityNodeInfo().isChecked)
        assertFalse(root.findViewWithTag<View>("blue").createAccessibilityNodeInfo().isChecked)
        assertEquals("purple", preference(context).value)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test fun rejectedUnknownAndRepeatedSelectionsDoNotRewritePreferences() {
        val context = ContextThemeWrapper(base, R.style.Modern_Blue_Light)
        val pref = preference(context)
        pref.value = "green"
        var calls = 0
        pref.setOnPreferenceChangeListener { _, _ -> calls++; false }
        assertFalse(pref.selectTheme("invalid"))
        assertFalse(pref.selectTheme("green"))
        assertEquals(0, calls)
        assertFalse(pref.selectTheme("pink"))
        assertEquals(1, calls)
        assertEquals("green", AppearancePalette.selected(context))
    }

    @Test fun galleryFitsSmallScreensAndLargeTextInAllLanguagesAndBothModes() {
        for (tag in base.resources.getStringArray(R.array.modern_language_values)) {
            for (scale in listOf(1f, 2f)) for (night in listOf(false, true)) {
                val config = Configuration(base.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(tag)); fontScale = scale; screenWidthDp = 320
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), AppearancePalette.resolve("blue", night)!!)
                val pref = preference(context)
                pref.value = "blue"
                val root = LayoutInflater.from(context).inflate(R.layout.preference_theme_gallery, null)
                pref.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(root))
                val density = context.resources.displayMetrics.density
                val width = (320 * density).toInt()
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                root.layout(0, 0, width, root.measuredHeight)
                for (id in listOf("blue","green","purple","red","orange","teal","pink","slate")) {
                    val card = root.findViewWithTag<MaterialCardView>(id)
                    val label = (card.getChildAt(0) as LinearLayout).getChildAt(1) as TextView
                    assertTrue("$tag $id target", card.width >= 120 * density && card.height >= 48 * density)
                    assertTrue("$tag $id bounds", card.right <= (card.parent as View).width)
                    assertTrue("$tag $id label clipped", label.layout.height <= label.height - label.paddingTop - label.paddingBottom)
                }
            }
        }
    }
}
