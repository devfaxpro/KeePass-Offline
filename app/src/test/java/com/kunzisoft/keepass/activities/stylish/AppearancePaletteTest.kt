/* Local Android redesign. SPDX-License-Identifier: GPL-3.0-or-later */
package com.kunzisoft.keepass.activities.stylish

import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.kunzisoft.keepass.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class AppearancePaletteTest {
    private lateinit var context: Context

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test fun everyPaletteMeetsTextContrastInBothModes() {
        for (name in listOf("blue", "green", "purple", "red", "orange", "teal", "pink", "slate")) {
            for (night in listOf(false, true)) {
                val themed = ContextThemeWrapper(context, AppearancePalette.resolve(name, night)!!)
                val pairs = listOf(
                    R.attr.colorOnPrimary to R.attr.colorPrimary,
                    R.attr.colorOnPrimaryContainer to R.attr.colorPrimaryContainer,
                    R.attr.colorOnPrimaryContainer to R.attr.atelierSceneEnd,
                    R.attr.colorOnSurface to R.attr.colorSurface,
                    R.attr.colorOnSurface to android.R.attr.colorBackground,
                    R.attr.colorOnSurfaceVariant to R.attr.colorSurfaceContainerLow,
                    R.attr.colorOnSurfaceVariant to R.attr.colorSurfaceContainer,
                    R.attr.colorOnSurfaceInverse to R.attr.colorSurfaceInverse,
                    R.attr.colorOnSecondary to R.attr.colorSecondary)
                for ((foreground, background) in pairs) {
                    val contrast = ColorUtils.calculateContrast(color(themed, foreground), color(themed, background))
                    assertTrue("$name night=$night contrast=$contrast", contrast >= 4.5)
                }
            }
        }
    }

    @Test fun brightnessOverridesSystemAndAutomaticFollowsIt() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val nightConfig = Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
        }
        val darkSystem = context.createConfigurationContext(nightConfig)
        assertTrue(AppearancePalette.isNight(darkSystem))
        prefs.edit().putString(context.getString(R.string.setting_style_brightness_key),
            context.getString(R.string.list_style_brightness_light)).commit()
        assertFalse(AppearancePalette.isNight(darkSystem))
        prefs.edit().putString(context.getString(R.string.setting_style_brightness_key),
            context.getString(R.string.list_style_brightness_night)).commit()
        assertTrue(AppearancePalette.isNight(context))
    }

    @Test fun selectionPersistsAndFallbackAndLegacyAreDefined() {
        assertEquals("blue", AppearancePalette.selected(context))
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(context.getString(R.string.modern_palette_key), "teal").commit()
        assertEquals("teal", AppearancePalette.selected(context.createConfigurationContext(Configuration())))
        assertEquals(R.style.Modern_Blue_Light, AppearancePalette.resolve("invalid", false))
        assertEquals(R.style.Modern_Blue_Night, AppearancePalette.resolve("dynamic", true))
        assertNull(AppearancePalette.resolve("legacy", false))
        assertFalse(Stylish.isDynamic(context))
    }

    @Test fun languageSelectorPreservesRequestedOrderAndNativeNames() {
        assertArrayEquals(arrayOf("it", "en", "en-US", "es", "pt-BR", "fr", "de", "zh-Hans", "ja", "ko", "ru", "hi", "ar", "id", "bn", "ur", "tr", "vi", "th", "pl", "nl", "ro", "uk", "pt-PT", "es-419", "zh-Hant"),
            context.resources.getStringArray(R.array.modern_language_values))
        assertArrayEquals(arrayOf("Italiano", "English", "English (United States)", "Español", "Português (Brasil)", "Français", "Deutsch",
            "简体中文", "日本語", "한국어", "Русский", "हिन्दी", "العربية", "Bahasa Indonesia", "বাংলা", "اردو", "Türkçe", "Tiếng Việt", "ไทย", "Polski", "Nederlands", "Română", "Українська", "Português (Portugal)", "Español (Latinoamérica)", "繁體中文"), context.resources.getStringArray(R.array.modern_language_names))
    }

    @Test fun sharedLayoutsInflateForEveryPaletteAndBrightness() {
        for (name in listOf("blue", "green", "purple", "red", "orange", "teal", "pink", "slate")) {
            for (night in listOf(false, true)) {
                val themed = ContextThemeWrapper(context, AppearancePalette.resolve(name, night)!!)
                val inflater = LayoutInflater.from(themed)
                for (layout in listOf(R.layout.preference_appearance_preview,
                    R.layout.item_list_nodes_entry, R.layout.item_list_nodes_group, R.layout.view_main_credentials,
                    R.layout.view_template, R.layout.preference_material, R.layout.preference_category_material,
                    R.layout.activity_file_selection, R.layout.activity_main_credential, R.layout.item_file_info,
                    R.layout.fragment_group)) {
                    assertNotNull(inflater.inflate(layout, null))
                }
            }
        }
    }

    @Test fun changingPaletteKeepsScreenshotProtectionAfterRecreation() {
        val controller = Robolectric.buildActivity(PaletteProbeActivity::class.java).setup()
        try {
            val before = color(controller.get(), R.attr.colorPrimary)
            assertTrue(controller.get().window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(context.getString(R.string.modern_palette_key), "purple").commit()
            controller.recreate()
            assertNotEquals(before, color(controller.get(), R.attr.colorPrimary))
            assertTrue(controller.get().window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test fun unlockPasswordRemainsMaskedAndOptsOutOfPersonalizedLearning() {
        val themed = ContextThemeWrapper(context, R.style.Modern_Blue_Light)
        val view = LayoutInflater.from(themed).inflate(R.layout.view_main_credentials, null)
        val password = view.findViewById<TextView>(R.id.password_text_view)
        assertEquals(InputType.TYPE_TEXT_VARIATION_PASSWORD,
            password.inputType and InputType.TYPE_MASK_VARIATION)
        assertTrue(password.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0)
        assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO, password.importantForAutofill)
    }

    private fun color(context: Context, attr: Int): Int = TypedValue().let {
        assertTrue(context.theme.resolveAttribute(attr, it, true))
        it.data
    }
}

/** No database or fake authentication; exercises only theme and window lifecycle. */
class PaletteProbeActivity : StylishActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this))
    }
}
