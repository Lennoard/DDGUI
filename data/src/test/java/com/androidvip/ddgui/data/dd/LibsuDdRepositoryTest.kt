package com.androidvip.ddgui.data.dd

import com.androidvip.ddgui.domain.dd.DdConversion
import com.androidvip.ddgui.domain.dd.DdExecutionResult
import com.androidvip.ddgui.domain.dd.DdRequest
import com.androidvip.ddgui.domain.dd.RootAccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibsuDdRepositoryTest {
    @Test
    fun `given root denied when checking and executing then denies without running command`() = runTest {
        // Given
        val shell = mockk<RootShellDataSource>()
        coEvery { shell.hasRootAccess() } returns false
        val repository = LibsuDdRepository(shell)

        // When
        val access = repository.checkRootAccess()
        val result = repository.execute(request, {})

        // Then
        assertEquals(RootAccess.Denied, access)
        assertEquals(DdExecutionResult.RootDenied, result)
        coVerify(exactly = 0) { shell.execute(any(), any()) }
    }

    @Test
    fun `given root and valid request when executing then returns shell output and exit status`() = runTest {
        // Given
        val shell = mockk<RootShellDataSource>()
        val expectedCommand = "dd if='/source image.img' of='/destination.img' conv=sync"
        coEvery { shell.hasRootAccess() } returns true
        coEvery { shell.execute(any(), any()) } coAnswers {
            secondArg<(String) -> Unit>().invoke("streamed line")
            ShellCommandResult(
                exitCode = 3,
                stdout = listOf("partial output"),
                stderr = listOf("write failed")
            )
        }
        val repository = LibsuDdRepository(shell)
        val output = mutableListOf<String>()

        // When
        val result = repository.execute(request) { output.add(it) }

        // Then
        assertEquals(listOf("streamed line"), output)
        assertEquals(
            DdExecutionResult.Completed(
                exitCode = 3,
                stdout = listOf("partial output"),
                stderr = listOf("write failed")
            ),
            result
        )
        coVerify(exactly = 1) {
            shell.execute(
                expectedCommand,
                any()
            )
        }
    }

    @Test
    fun `given invalid request when executing then rejects without calling shell`() = runTest {
        // Given
        val shell = mockk<RootShellDataSource>()
        coEvery { shell.hasRootAccess() } returns true
        val repository = LibsuDdRepository(shell)

        // When
        val result = repository.execute(request.copy(inputPath = ""), {})

        // Then
        assertTrue(result is DdExecutionResult.InvalidRequest)
        coVerify(exactly = 0) { shell.execute(any(), any()) }
    }

    private val request = DdRequest(
        inputPath = "/source image.img",
        outputPath = "/destination.img",
        conversions = listOf(DdConversion.SYNC)
    )
}
