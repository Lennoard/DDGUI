package com.androidvip.ddgui.presentation.main

import com.androidvip.ddgui.domain.dd.DdConversion
import com.androidvip.ddgui.domain.dd.DdExecutionResult
import com.androidvip.ddgui.domain.dd.DdRepository
import com.androidvip.ddgui.domain.dd.DdRequest
import com.androidvip.ddgui.domain.dd.RootAccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
            val viewModel = MainViewModel(repository)

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
            coEvery { repository.checkRootAccess() } returns RootAccess.Granted
            coEvery { repository.execute(any(), any()) } coAnswers {
                secondArg<(String) -> Unit>().invoke("copy complete")
                DdExecutionResult.Completed(0, listOf("copy complete"), emptyList())
            }
            val viewModel = MainViewModel(repository)

            // When
            viewModel.uiState.first { it.rootStatus == RootUiStatus.Ready }
            viewModel.onEvent(MainUiEvent.InputChanged("/source.img"))
            viewModel.onEvent(MainUiEvent.OutputChanged("/destination.img"))
            viewModel.onEvent(MainUiEvent.ConversionChanged(DdConversion.SYNC, true))
            viewModel.onEvent(MainUiEvent.Execute)
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
}
