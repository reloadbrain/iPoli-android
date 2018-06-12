package io.ipoli.android.common.view

import android.content.Context
import android.preference.PreferenceManager
import android.support.annotation.AttrRes
import android.support.v7.view.ContextThemeWrapper
import android.util.TypedValue
import io.ipoli.android.Constants

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 15.12.17.
 */

val Context.playerTheme: Int
    get() {
        val pm = PreferenceManager.getDefaultSharedPreferences(this)
        if (pm.contains(Constants.KEY_THEME)) {
            val themeName = pm.getString(Constants.KEY_THEME, "")
            if (themeName.isNotEmpty()) {
                return AndroidTheme.valueOf(themeName).style
            }
        }
        return AndroidTheme.valueOf(Constants.DEFAULT_THEME.name).style
    }

fun Context.asThemedWrapper() =
    ContextThemeWrapper(this, playerTheme)

fun Context.attrData(@AttrRes attributeRes: Int) =
    TypedValue().let {
        theme.resolveAttribute(attributeRes, it, true)
        it.data
    }