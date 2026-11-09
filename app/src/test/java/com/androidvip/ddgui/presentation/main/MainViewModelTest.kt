package com.androidvip.ddgui.presentation.main

import com.androidvip.ddgui.domain.dd.DdConversion
import com.androidvip.ddgui.domain.dd.DdExecutionResult
import com.androidvip.ddgui.domain.dd.DdRepository
import com.androidvip.ddgui.domain.dd.DdRequest
import com.androidvip.ddgui.domain.dd.RootAccess
import com.androidvip.ddgui.domain.settings.DestructiveOperationSettingsRepository
import io.mockk.every
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @Test
    fun `given empty input when executing then emits validation without calling repository`() = runTest {
        // Given
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = mockk<DdRepository>()
            coEvery { repository.checkRootAccess() } returns RootAccess.Granted
            val settings = mockk<DestructiveOperationSettingsRepository>()
            every { settings.isWarningDialogEnabled() } returns true
            val viewModel = MainViewModel(repository, settings)

            // When
            viewModel.uiState.first { it.rootStatus == RootUiStatus.Ready }
            viewModel.onEvent(MainUiEvent.OutputChanged("/destination.img"))
            viewModel.onEvent(MainUiEvent.Execute)
            val effect = viewModel.effects.first()

            // Then
            assertEquals(
                MainUiEffect.ShowMessage(MainUiMessage.MissingInput),
                effect
            )
            coVerify(exactly = 0) { repository.execute(any(), any()) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `given valid paths when execution succeeds then exposes success and output`() = runTest {
        // Given
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = mockk<DdRepository>()
            val settings = mockk<DestructiveOperationSettingsRepository>()
            coEvery { repository.checkRootAccess() } returns RootAccess.Granted
            every { settings.isWarningDialogEnabled() } returns true
            coEvery { repository.execute(any(), any()) } coAnswers {
                secondArg<(String) -> Unit>().invoke("copy complete")
                DdExecutionResult.Completed(0, listOf("copy complete"), emptyList())
            }
            val viewModel = MainViewModel(repository, settings)

            // When
            viewModel.uiState.first { it.rootStatus == RootUiStatus.Ready }
            viewModel.onEvent(MainUiEvent.InputChanged("/source.img"))
            viewModel.onEvent(MainUiEvent.OutputChanged("/destination.img"))
            viewModel.onEvent(MainUiEvent.ConversionChanged(DdConversion.SYNC, true))
            viewModel.onEvent(MainUiEvent.Execute)
            val review = viewModel.uiState.first { it.executionReview != null }
            assertEquals("/source.img", review.executionReview?.inputPath)
            coVerify(exactly = 0) { repository.execute(any(), any()) }

            viewModel.onEvent(MainUiEvent.ConfirmExecution)
            val result = viewModel.uiState.first {
                it.executionStatus == ExecutionUiStatus.Succeeded
            }

            // Then
            assertEquals("copy complete", result.outputText)
            coVerify(exactly = 1) {
                repository.execute(
                    DdRequest(
                        "/source.img",
                        "/destination.img",
                        listOf(DdConversion.SYNC)
                    ),
                    any()
                )
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `given warning dialog disabled when requesting execution then review still requires confirmation`() = runTest {
        // Given
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = mockk<DdRepository>()
            val settings = mockk<DestructiveOperationSettingsRepository>()
            coEvery { repository.checkRootAccess() } returns RootAccess.Granted
            coEvery {
                repository.execute(any(), any())
            } returns DdExecutionResult.Completed(0, emptyList(), emptyList())
            every { settings.isWarningDialogEnabled() } returns false
            val viewModel = MainViewModel(repository, settings)

            // When
            viewModel.uiState.first { it.rootStatus == RootUiStatus.Ready }
            viewModel.onEvent(MainUiEvent.InputChanged("/source.img"))
            viewModel.onEvent(MainUiEvent.OutputChanged("/destination.img"))
            viewModel.onEvent(MainUiEvent.Execute)
            val review = viewModel.uiState.first { it.executionReview != null }

            // Then
            assertEquals(false, review.showWarningDialog)
            assertEquals("/destination.img", review.executionReview?.outputPath)
            coVerify(exactly = 0) { repository.execute(any(), any()) }

            viewModel.onEvent(MainUiEvent.ConfirmExecution)
            viewModel.uiState.first { it.executionStatus == ExecutionUiStatus.Succeeded }
            coVerify(exactly = 1) { repository.execute(any(), any()) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `given reviewed operation when cancelling then clears review without executing`() = runTest {
        // Given
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = mockk<DdRepository>()
            val settings = mockk<DestructiveOperationSettingsRepository>()
            coEvery { repository.checkRootAccess() } returns RootAccess.Granted
            every { settings.isWarningDialogEnabled() } returns true
            val viewModel = MainViewModel(repository, settings)

            // When
            viewModel.uiState.first { it.rootStatus == RootUiStatus.Ready }
            viewModel.onEvent(MainUiEvent.InputChanged("/source.img"))
            viewModel.onEvent(MainUiEvent.OutputChanged("/destination.img"))
            viewModel.onEvent(MainUiEvent.Execute)
            viewModel.uiState.first { it.executionReview != null }
            viewModel.onEvent(MainUiEvent.CancelExecutionReview)

            // Then
            assertEquals(null, viewModel.uiState.value.executionReview)
            coVerify(exactly = 0) { repository.execute(any(), any()) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `given warning preference changed when saving succeeds then updates setting state`() = runTest {
        // Given
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = mockk<DdRepository>()
            val settings = mockk<DestructiveOperationSettingsRepository>()
            coEvery { repository.checkRootAccess() } returns RootAccess.Granted
            every { settings.isWarningDialogEnabled() } returns true
            every { settings.setWarningDialogEnabled(false) } just Runs
            val viewModel = MainViewModel(repository, settings)

            // When
            viewModel.onEvent(MainUiEvent.WarningDialogChanged(false))

            // Then
            assertEquals(false, viewModel.uiState.value.showWarningDialog)
            io.mockk.verify { settings.setWarningDialogEnabled(false) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `given settings action when opened then emits navigation effect`() = runTest {
        // Given
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = mockk<DdRepository>()
            val settings = mockk<DestructiveOperationSettingsRepository>()
            coEvery { repository.checkRootAccess() } returns RootAccess.Granted
            every { settings.isWarningDialogEnabled() } returns true
            val viewModel = MainViewModel(repository, settings)

            // When
            viewModel.onEvent(MainUiEvent.OpenSettings)
            val effect = viewModel.effects.first()

            // Then
            assertEquals(MainUiEffect.NavigateToSettings, effect)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
