package com.kunzisoft.keepass.settings

import java.util.Locale

/** Conserva le varianti regionali e la scrittura cinese nel selettore. */
object AppLanguage {
    fun selection(locale: Locale?): String = when (locale?.language) {
        "en" -> if (locale.country == "US") "en-US" else "en"
        "pt" -> if (locale.country == "PT") "pt-PT" else "pt-BR"
        "es" -> if (locale.country.isNotEmpty() && locale.country != "ES") "es-419" else "es"
        "zh" -> if ((android.os.Build.VERSION.SDK_INT >= 21 && locale.script == "Hant") || locale.country in listOf("TW", "HK", "MO")) "zh-Hant" else "zh-Hans"
        "in", "id" -> "id"
        else -> locale?.language ?: "en"
    }
}
