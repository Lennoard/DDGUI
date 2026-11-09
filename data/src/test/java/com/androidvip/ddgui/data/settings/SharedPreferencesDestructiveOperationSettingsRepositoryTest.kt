package com.androidvip.ddgui.data.settings

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreferencesDestructiveOperationSettingsRepositoryTest {
    @Test
    fun `given no saved value when reading warning preference then defaults to enabled`() {
        // Given
        val preferences = mockk<SharedPreferences>()
        every {
            preferences.getBoolean("destructive_operation_warning_dialog_enabled", true)
        } returns true
        val repository = SharedPreferencesDestructiveOperationSettingsRepository(preferences)

        // When
        val enabled = repository.isWarningDialogEnabled()

        // Then
        assertTrue(enabled)
        verify {
            preferences.getBoolean("destructive_operation_warning_dialog_enabled", true)
        }
    }

    @Test
    fun `given warning disabled when saving preference then persists disabled state`() {
        // Given
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val preferences = mockk<SharedPreferences> {
            every { edit() } returns editor
        }
        val repository = SharedPreferencesDestructiveOperationSettingsRepository(preferences)

        // When
        repository.setWarningDialogEnabled(false)

        // Then
        verify {
            editor.putBoolean("destructive_operation_warning_dialog_enabled", false)
        }
    }
}
