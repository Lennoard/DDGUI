package com.androidvip.ddgui.presentation.disclaimer

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DisclaimerViewModelTest {
    @Test
    fun `given accepted disclaimer when continuing then saves acceptance and emits effect`() = runTest {
        // Given
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val preferences = mockk<SharedPreferences> {
            every { edit() } returns editor
        }
        val viewModel = DisclaimerViewModel(preferences)
        viewModel.onEvent(DisclaimerUiEvent.AcceptanceChanged(true))

        // When
        viewModel.onEvent(DisclaimerUiEvent.Continue)
        val effect = viewModel.effects.first()

        // Then
        assertEquals(DisclaimerUiEffect.ContinueToApp, effect)
        verify { editor.putBoolean(DisclaimerViewModel.DISCLAIMER_PASSED_KEY, true) }
    }

    @Test
    fun `given unaccepted disclaimer when continuing then does not emit effect`() = runTest {
        // Given
        val preferences = mockk<SharedPreferences>(relaxed = true)
        val viewModel = DisclaimerViewModel(preferences)

        // When
        viewModel.onEvent(DisclaimerUiEvent.Continue)

        // Then
        assertEquals(false, viewModel.uiState.value.accepted)
        verify(exactly = 0) { preferences.edit() }
    }
}
