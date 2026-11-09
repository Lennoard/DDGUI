package com.androidvip.ddgui.data.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.androidvip.ddgui.domain.settings.DestructiveOperationSettingsRepository

class SharedPreferencesDestructiveOperationSettingsRepository(
    private val preferences: SharedPreferences
) : DestructiveOperationSettingsRepository {
    override fun isWarningDialogEnabled(): Boolean =
        preferences.getBoolean(WARNING_DIALOG_ENABLED_KEY, true)

    override fun setWarningDialogEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(WARNING_DIALOG_ENABLED_KEY, enabled) }
    }

    private companion object {
        const val WARNING_DIALOG_ENABLED_KEY = "destructive_operation_warning_dialog_enabled"
    }
}
