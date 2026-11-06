/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.kunzisoft.keepass.activities.stylish

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.NestedSettingsFragment
import com.kunzisoft.keepass.settings.SettingsActivity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [19, 28, 34], application = android.app.Application::class)
class AppearanceSettingsTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clear() { PreferenceManager.getDefaultSharedPreferences(base).edit().clear().commit() }

    @Test @Config(sdk = [34]) fun navigateFromSettingsToAppearance() {
        val controller = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        try {
            val activity = controller.get()
            activity.supportFragmentManager.executePendingTransactions()
            activity.onNestedPreferenceSelected(NestedSettingsFragment.Screen.APPEARANCE, false)
            activity.supportFragmentManager.executePendingTransactions()
            controller.visible()
            shadowOf(Looper.getMainLooper()).idle()
            val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                as NestedSettingsFragment
            val list = fragment.listView
            list.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY))
            list.layout(0, 0, 1080, 2400)
            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            assertNotNull(list.findViewWithTag<View>("purple"))
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test fun openingAppearanceLoadsRealPreferencesAndBindsThemeGallery() {
        val controller = Robolectric.buildActivity(AppearanceProbeActivity::class.java).setup()
        try {
            val activity = controller.get()
            val fragment = NestedSettingsFragment.newInstance(NestedSettingsFragment.Screen.APPEARANCE)
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment).commitNow()
            controller.visible()
            shadowOf(Looper.getMainLooper()).idle()
            val list = fragment.listView
            list.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY))
            list.layout(0, 0, 1080, 2400)
            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            assertNotNull(list.findViewWithTag<View>("purple"))
            assertTrue(list.findViewWithTag<View>("purple").performClick())
            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            assertEquals("purple", AppearancePalette.selected(activity))
            // A new activity must load the persisted selection and animate safely again.
            controller.recreate().visible()
            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            val restored = controller.get().supportFragmentManager
                .findFragmentById(R.id.fragment_container) as NestedSettingsFragment
            assertEquals("purple", restored.findPreference<androidx.preference.ListPreference>(
                activity.getString(R.string.modern_palette_key))!!.value)
            restored.listView.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY))
            restored.listView.layout(0, 0, 1080, 2400)
            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            assertTrue((restored.listView.findViewWithTag<View>("purple")
                as com.google.android.material.card.MaterialCardView).isChecked)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}

/** Real preference fragment and lifecycle, with a stable container after recreation. */
class AppearanceProbeActivity : StylishActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply { id = R.id.fragment_container })
    }
}
