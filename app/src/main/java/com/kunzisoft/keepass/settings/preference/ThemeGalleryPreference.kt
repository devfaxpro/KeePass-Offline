/* Local appearance preference. SPDX-License-Identifier: GPL-3.0-or-later */
package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.AppearancePalette
import com.kunzisoft.keepass.view.AtelierArtworkView

/** Keeps ListPreference storage and change callbacks; adds visible, accessible theme previews. */
class ThemeGalleryPreference @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.dialogPreferenceStyle, defStyleRes: Int = 0
) : ListPreference(context, attrs, defStyleAttr, defStyleRes) {
    init {
        layoutResource = R.layout.preference_theme_gallery
        isSelectable = false
    }

    internal fun selectTheme(id: String): Boolean {
        if (id == value || entryValues.none { it.toString() == id } || !callChangeListener(id)) return false
        value = id
        return true
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.isDividerAllowedAbove = false
        holder.isDividerAllowedBelow = false
        holder.itemView.isClickable = false
        holder.itemView.isFocusable = false
        (holder.findViewById(android.R.id.summary) as TextView).setText(R.string.modern_palette_summary)
        val grid = holder.findViewById(R.id.theme_gallery) as LinearLayout
        grid.removeAllViews()
        val selected = value?.takeIf { entryValues.contains(it) } ?: "blue"
        val indices = entryValues.indices.filter { entryValues[it] != "dynamic" && entryValues[it] != "legacy" }
        // At large font sizes use a single column, preserving complete names and touch targets.
        val columns = if (context.resources.configuration.fontScale >= 1.5f) 1 else 2
        for (group in indices.chunked(columns)) {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            grid.addView(row, LinearLayout.LayoutParams(-1, -2))
            for ((position, index) in group.withIndex()) {
                val id = entryValues[index].toString()
                val themed = ContextThemeWrapper(context, AppearancePalette.resolve(id, AppearancePalette.isNight(context))!!)
                val checked = id == selected
                val card = MaterialCardView(themed).apply {
                    tag = id
                    radius = dp(16).toFloat()
                    cardElevation = 0f
                    setCardBackgroundColor(color(themed, R.attr.colorSurface))
                    strokeColor = color(themed, if (checked) R.attr.colorPrimary else R.attr.atelierSeparator)
                    strokeWidth = dp(if (checked) 2 else 1)
                    isClickable = true
                    isFocusable = true
                    isEnabled = this@ThemeGalleryPreference.isEnabled
                    // setChecked starts Material's icon animation. Clear the unused icon first,
                    // so a pending animation cannot dereference an icon removed afterwards.
                    checkedIcon = null
                    isCheckable = true
                    isChecked = checked
                    setOnClickListener {
                        selectTheme(id)
                        isChecked = this@ThemeGalleryPreference.value == id
                    }
                }
                val content = LinearLayout(themed).apply {
                    orientation = LinearLayout.VERTICAL
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                }
                content.addView(AtelierArtworkView(themed).apply { preview = true }, LinearLayout.LayoutParams(-1, dp(64)))
                content.addView(TextView(themed).apply {
                    text = entries[index].toString() + if (checked) "  ✓" else ""
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                    setTextColor(color(themed, R.attr.colorOnSurface))
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    minHeight = dp(36)
                }, LinearLayout.LayoutParams(-1, -2))
                card.addView(content)
                radioAccessibility(card, entries[index].toString(), checked)
                row.addView(card, LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (position > 0) marginStart = dp(12)
                    bottomMargin = dp(8)
                })
            }
            if (group.size < columns) row.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
        }
        // System colors and legacy themes keep their existing availability and behavior.
        for (index in entryValues.indices.filter { it !in indices }) {
            val id = entryValues[index].toString()
            val checked = id == selected
            val button = MaterialButton(context).apply {
                tag = id
                text = entries[index].toString() + if (checked) "  ✓" else ""
                isAllCaps = false
                cornerRadius = dp(12)
                minHeight = dp(48)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setPadding(dp(16), dp(8), dp(16), dp(8))
                backgroundTintList = ColorStateList.valueOf(color(context, R.attr.colorSurface))
                setTextColor(color(context, R.attr.colorOnSurface))
                strokeWidth = dp(if (checked) 2 else 1)
                strokeColor = ColorStateList.valueOf(color(context, if (checked) R.attr.colorPrimary else R.attr.atelierSeparator))
                isEnabled = this@ThemeGalleryPreference.isEnabled
                isCheckable = true
                isChecked = checked
                setOnClickListener {
                    selectTheme(id)
                    isChecked = this@ThemeGalleryPreference.value == id
                }
            }
            radioAccessibility(button, entries[index].toString(), checked)
            grid.addView(button, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun radioAccessibility(view: View, label: String, checked: Boolean) {
        view.contentDescription = label
        ViewCompat.setAccessibilityDelegate(view, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = "android.widget.RadioButton"
                info.isCheckable = true
                info.isChecked = checked
            }
        })
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density + 0.5f).toInt()
    private fun color(context: Context, attr: Int) = TypedValue().let {
        context.theme.resolveAttribute(attr, it, true)
        it.data
    }
}
