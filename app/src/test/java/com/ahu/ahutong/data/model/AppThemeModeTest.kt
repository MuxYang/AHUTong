package com.ahu.ahutong.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppThemeModeTest {
    @Test
    fun parsesStoredValues() {
        assertEquals(AppThemeMode.FOLLOW_SYSTEM, AppThemeMode.fromStorage("follow_system"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromStorage("dark"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromStorage("light"))
    }

    @Test
    fun missingOrUnknownStoredValueFollowsSystem() {
        assertEquals(AppThemeMode.FOLLOW_SYSTEM, AppThemeMode.fromStorage(null))
        assertEquals(AppThemeMode.FOLLOW_SYSTEM, AppThemeMode.fromStorage("unknown"))
    }

    @Test
    fun followSystemResolvesFromSystemTheme() {
        assertTrue(AppThemeMode.FOLLOW_SYSTEM.resolve(systemIsDark = true))
        assertFalse(AppThemeMode.FOLLOW_SYSTEM.resolve(systemIsDark = false))
    }

    @Test
    fun explicitModesIgnoreSystemTheme() {
        assertTrue(AppThemeMode.DARK.resolve(systemIsDark = true))
        assertTrue(AppThemeMode.DARK.resolve(systemIsDark = false))
        assertFalse(AppThemeMode.LIGHT.resolve(systemIsDark = true))
        assertFalse(AppThemeMode.LIGHT.resolve(systemIsDark = false))
    }
}
