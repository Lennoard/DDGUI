package com.androidvip.ddgui.presentation.disclaimer

import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

@Immutable
data class DisclaimerUiState(val accepted: Boolean = false)

@Immutable
sealed interface DisclaimerUiEffect {
    data object ContinueToApp : DisclaimerUiEffect
}

@Immutable
sealed interface DisclaimerUiEvent {
    data class AcceptanceChanged(val accepted: Boolean) : DisclaimerUiEvent
    data object Continue : DisclaimerUiEvent
}

class DisclaimerViewModel(private val preferences: SharedPreferences) : ViewModel() {
    private val _uiState = MutableStateFlow(DisclaimerUiState())
    val uiState = _uiState.asStateFlow()

    private val effectChannel = Channel<DisclaimerUiEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun onEvent(event: DisclaimerUiEvent) {
        when (event) {
            is DisclaimerUiEvent.AcceptanceChanged ->
                _uiState.update { it.copy(accepted = event.accepted) }

            DisclaimerUiEvent.Continue -> continueToApp()
        }
    }

    private fun continueToApp() {
        if (!_uiState.value.accepted) return
        preferences.edit { putBoolean(DISCLAIMER_PASSED_KEY, true) }
        effectChannel.trySend(DisclaimerUiEffect.ContinueToApp)
    }

    companion object {
        const val DISCLAIMER_PASSED_KEY = "disclaimer_pass"
    }
}
