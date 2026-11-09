package com.androidvip.ddgui.domain.settings

interface DestructiveOperationSettingsRepository {
    fun isWarningDialogEnabled(): Boolean
    fun setWarningDialogEnabled(enabled: Boolean)
}
