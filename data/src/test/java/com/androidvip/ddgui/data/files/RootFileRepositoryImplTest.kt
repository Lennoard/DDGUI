package com.androidvip.ddgui.data.files

import com.androidvip.ddgui.domain.files.DirectoryListingResult
import com.androidvip.ddgui.domain.files.FileBrowserFailure
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class RootFileRepositoryImplTest {
    @Test
    fun `given entries when listing directory then sorts directories before files`() = runTest {
        // Given
        val root = Files.createTempDirectory("ddgui-files-test").toFile()
        try {
            val directory = File(root, "z-directory").apply { mkdir() }
            val anotherDirectory = File(root, "a-directory").apply { mkdir() }
            val file = File(root, "a-file").apply { createNewFile() }
            val source = mockk<RootFileDataSource>()
            every { source.list(root.absolutePath) } returns
                RootFileSourceResult.Success(listOf(file, directory, anotherDirectory))
            val repository = RootFileRepositoryImpl(
                source,
                StandardTestDispatcher(testScheduler)
            )

            // When
            val result = repository.list(root.absolutePath)

            // Then
            assertTrue(result is DirectoryListingResult.Success)
            assertEquals(
                listOf("a-directory", "z-directory", "a-file"),
                (result as DirectoryListingResult.Success).entries.map { it.name }
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `given empty and failed listings when listing paths then keeps outcomes distinct`() = runTest {
        // Given
        val source = mockk<RootFileDataSource>()
        every { source.list("/empty") } returns RootFileSourceResult.Success(emptyList())
        every { source.list("/inaccessible") } returns RootFileSourceResult.Failure
        val repository = RootFileRepositoryImpl(
            source,
            StandardTestDispatcher(testScheduler)
        )

        // When
        val emptyResult = repository.list("/empty")
        val failedResult = repository.list("/inaccessible")

        // Then
        assertEquals(DirectoryListingResult.Success(emptyList()), emptyResult)
        assertEquals(
            DirectoryListingResult.Failure(FileBrowserFailure.ACCESS_DENIED_OR_NOT_DIRECTORY),
            failedResult
        )
    }
}
