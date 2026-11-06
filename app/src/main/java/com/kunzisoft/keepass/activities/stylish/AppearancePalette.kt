/* Local Android redesign. SPDX-License-Identifier: GPL-3.0-or-later */
package com.kunzisoft.keepass.activities.stylish

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StyleRes
import androidx.preference.PreferenceManager
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.PreferencesUtil

/** Only presentation preferences live here; no vault data or credentials are accessed. */
object AppearancePalette {
    fun selected(context: Context): String =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.modern_palette_key), "blue") ?: "blue"

    fun isNight(context: Context): Boolean = when (PreferencesUtil.getStyleBrightness(context)) {
        context.getString(R.string.list_style_brightness_light) -> false
        context.getString(R.string.list_style_brightness_night) -> true
        else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    @StyleRes
    fun theme(context: Context): Int? = resolve(selected(context), isNight(context))

    /** A stable fallback also covers malformed or restored preferences. */
    @StyleRes
    fun resolve(palette: String, night: Boolean): Int? = when (palette) {
        "legacy" -> null
        "green" -> if (night) R.style.Modern_Green_Night else R.style.Modern_Green_Light
        "purple" -> if (night) R.style.Modern_Purple_Night else R.style.Modern_Purple_Light
        "red" -> if (night) R.style.Modern_Red_Night else R.style.Modern_Red_Light
        "orange" -> if (night) R.style.Modern_Orange_Night else R.style.Modern_Orange_Light
        "teal" -> if (night) R.style.Modern_Teal_Night else R.style.Modern_Teal_Light
        "pink" -> if (night) R.style.Modern_Pink_Night else R.style.Modern_Pink_Light
        "slate" -> if (night) R.style.Modern_Slate_Night else R.style.Modern_Slate_Light
        else -> if (night) R.style.Modern_Blue_Night else R.style.Modern_Blue_Light
    }
}
