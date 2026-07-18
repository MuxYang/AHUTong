package com.ahu.ahutong.data.model

enum class AppThemeMode(val storageValue: String) {
    FOLLOW_SYSTEM("follow_system"),
    DARK("dark"),
    LIGHT("light");

    fun resolve(systemIsDark: Boolean): Boolean = when (this) {
        FOLLOW_SYSTEM -> systemIsDark
        DARK -> true
        LIGHT -> false
    }

    companion object {
        fun fromStorage(value: String?): AppThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: FOLLOW_SYSTEM
    }
}
