package net.npg.wt2t.data.model

/** Lists supported persisted settings, their defaults, and accepted values. */
enum class AppSettings(
    internal val defaultValue: String,
) {
    DEFAULT_TARGET_MINUTES("0"),
    DEFAULT_TARGET_HOURS("8"),
    MERGE_THRESHOLD_MINUTES("5"),
    BOOKING_START_ADJUSTMENT_MINUTES("5"),
    LANGUAGE("de"),
    THEME_MODE("SYSTEM"),
    ;

    /** Returns whether [value] is a supported persisted representation. */
    internal fun isValidValue(value: String): Boolean = when (this) {
        DEFAULT_TARGET_MINUTES -> value.toIntOrNull() in DEFAULT_TARGET_MINUTES_RANGE
        DEFAULT_TARGET_HOURS -> value.toIntOrNull() in DEFAULT_TARGET_HOURS_RANGE
        MERGE_THRESHOLD_MINUTES -> value.toIntOrNull() in MERGE_THRESHOLD_MINUTES_RANGE
        BOOKING_START_ADJUSTMENT_MINUTES -> value.toIntOrNull() in BOOKING_START_ADJUSTMENT_MINUTES_RANGE
        LANGUAGE -> value in SUPPORTED_LANGUAGES
        THEME_MODE -> value in SUPPORTED_THEME_MODES
    }

    /** Parses an integer setting, falling back to this setting's documented default. */
    internal fun parseIntOrDefault(value: String?): Int =
        value?.takeIf(::isValidValue)?.toIntOrNull() ?: defaultValue.toInt()

    /** Parses a string setting, falling back to this setting's documented default. */
    internal fun parseStringOrDefault(value: String?): String =
        value?.takeIf(::isValidValue) ?: defaultValue

    companion object {
        /** Returns the supported setting for [key], or null for an unknown key. */
        internal fun fromKey(key: String): AppSettings? = entries.firstOrNull { it.name == key }

        private val DEFAULT_TARGET_MINUTES_RANGE = 0..59
        private val DEFAULT_TARGET_HOURS_RANGE = 0..24
        private val MERGE_THRESHOLD_MINUTES_RANGE = 0..1_440
        private val BOOKING_START_ADJUSTMENT_MINUTES_RANGE = 1..30
        private val SUPPORTED_LANGUAGES = setOf("de", "en")
        private val SUPPORTED_THEME_MODES = setOf("SYSTEM", "LIGHT", "DARK")
    }
}
