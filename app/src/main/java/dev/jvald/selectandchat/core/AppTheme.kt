package dev.jvald.selectandchat.core

/**
 * What the user picked in settings. Distinct from "is the system in dark mode" — only
 * [SYSTEM] and [DYNAMIC] defer to that; the rest pin an appearance regardless.
 */
enum class AppTheme {
    /** Green palette, light or dark to match the system. */
    SYSTEM,
    LIGHT,

    /** Neutral near-black. */
    DARK,

    /** True black, so OLED pixels switch off. */
    AMOLED,

    /** The green-tinted dark the app is designed around. */
    GREEN,

    /** Material You colours pulled from the wallpaper. */
    DYNAMIC,
    ;

    /** Whether this choice resolves to a dark appearance right now. */
    fun isDark(systemDark: Boolean) = when (this) {
        LIGHT -> false
        DARK, AMOLED, GREEN -> true
        SYSTEM, DYNAMIC -> systemDark
    }

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
