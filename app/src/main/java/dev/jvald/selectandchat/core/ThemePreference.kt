package dev.jvald.selectandchat.core

/**
 * What the user picked in Settings. Distinct from "is the system in dark mode": only
 * [SYSTEM] defers to that, and it keeps deferring, so a device that flips to dark at
 * sunset takes the app with it without anything being written to disk.
 *
 * The three dark options are the same Material 3 scheme built over three different
 * neutral tonal palettes — green-tinted, plain grey, and one pinned to black — so the
 * accents and every contrast pairing stay identical across them.
 */
enum class ThemePreference {
    SYSTEM,
    LIGHT,

    /** Neutral grey surfaces, green only on the accents. */
    DARK,

    /** The green-tinted dark the app is designed around; what [SYSTEM] uses. */
    APP_DEFAULT,

    /** Black surfaces, so OLED pixels switch off entirely. */
    AMOLED,
    ;

    /** Whether this choice resolves to a dark appearance right now. */
    fun isDark(systemDark: Boolean) = when (this) {
        LIGHT -> false
        DARK, APP_DEFAULT, AMOLED -> true
        SYSTEM -> systemDark
    }

    companion object {
        /**
         * Unknown names fall back to [SYSTEM]. That includes the wallpaper-coloured theme
         * an earlier build offered, and "GREEN", which is what [APP_DEFAULT] was called
         * before it had a name that says what it is — both followed the system's dark
         * setting onto the same scheme anyway.
         */
        fun from(name: String?) = when (name) {
            null -> SYSTEM
            else -> entries.firstOrNull { it.name == name } ?: SYSTEM
        }
    }
}
