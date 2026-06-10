package com.scanprototype

import android.content.Context
import android.content.SharedPreferences

enum class HudMode {
    OFF,
    ANDROID_CALL_HUD
}

object HudSettings {
    private const val PREFERENCES_NAME = "scan_prototype_settings"
    private const val KEY_HUD_MODE = "hud_mode"
    private val DEFAULT_HUD_MODE = HudMode.ANDROID_CALL_HUD

    private fun preferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun getHudMode(context: Context): HudMode {
        val value = preferences(context).getString(KEY_HUD_MODE, DEFAULT_HUD_MODE.name)
        return try {
            HudMode.valueOf(value ?: DEFAULT_HUD_MODE.name)
        } catch (e: IllegalArgumentException) {
            DEFAULT_HUD_MODE
        }
    }

    fun setHudMode(context: Context, mode: HudMode) {
        preferences(context).edit().putString(KEY_HUD_MODE, mode.name).apply()
    }

    fun isHudEnabled(context: Context): Boolean {
        return getHudMode(context) != HudMode.OFF
    }
}
