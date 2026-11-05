package com.androidvip.ddgui.presentation.files

import com.androidvip.ddgui.domain.files.DirectoryListingResult
import com.androidvip.ddgui.domain.files.RootFileEntry
import com.androidvip.ddgui.domain.files.RootFileRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilePickerViewModelTest {
    @Test
    fun `given listed file when selected then emits its path`() = runTest {
        // Given
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = mockk<RootFileRepository>()
            coEvery { repository.list("/") } returns DirectoryListingResult.Success(
                listOf(
                    RootFileEntry(
                        path = "/backup.img",
                        name = "backup.img",
                        isDirectory = false,
                        isFile = true
                    )
                )
            )
            val viewModel = FilePickerViewModel(repository)

            // When
            viewModel.onEvent(FilePickerEvent.Open)
            val loaded = viewModel.uiState.first { !it.isLoading && it.entries.isNotEmpty() }
            viewModel.onEvent(FilePickerEvent.Select(loaded.entries.single()))
            val effect = viewModel.effects.first()

            // Then
            assertEquals(FilePickerEffect.FileSelected("/backup.img"), effect)
            viewModel.onEvent(FilePickerEvent.Close)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `given a newer directory request when older request completes then keeps newer entries`() = runTest {
        // Given
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val rootResult = CompletableDeferred<DirectoryListingResult>()
            val childResult = CompletableDeferred<DirectoryListingResult>()
            val repository = mockk<RootFileRepository>()
            coEvery { repository.list("/") } coAnswers { rootResult.await() }
            coEvery { repository.list("/child") } coAnswers { childResult.await() }
            val viewModel = FilePickerViewModel(repository)

            // When
            viewModel.onEvent(FilePickerEvent.Open)
            viewModel.onEvent(
                FilePickerEvent.Select(
                    FileEntryUiModel(
                    path = "/child",
                    name = "child",
                    isDirectory = true,
                    isFile = false
                    )
                )
            )
            childResult.complete(
                DirectoryListingResult.Success(
                    listOf(
                        RootFileEntry(
                            path = "/child/current.img",
                            name = "current.img",
                            isDirectory = false,
                            isFile = true
                        )
                    )
                )
            )
            viewModel.uiState.first { it.entries.any { entry -> entry.name == "current.img" } }
            rootResult.complete(
                DirectoryListingResult.Success(
                    listOf(
                        RootFileEntry(
                            path = "/stale.img",
                            name = "stale.img",
                            isDirectory = false,
                            isFile = true
                        )
                    )
                )
            )

            // Then
            val finalState = viewModel.uiState.value
            assertEquals("/child", finalState.currentPath)
            assertEquals(listOf("current.img"), finalState.entries.map { it.name })
            viewModel.onEvent(FilePickerEvent.Close)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
