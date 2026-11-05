package com.androidvip.ddgui.data.dd

import com.androidvip.ddgui.domain.dd.DdExecutionResult
import com.androidvip.ddgui.domain.dd.DdRepository
import com.androidvip.ddgui.domain.dd.DdRequest
import com.androidvip.ddgui.domain.dd.RootAccess

class LibsuDdRepository internal constructor(
    private val shell: RootShellDataSource
) : DdRepository {
    override suspend fun checkRootAccess(): RootAccess =
        if (shell.hasRootAccess()) RootAccess.Granted else RootAccess.Denied

    override suspend fun execute(
        request: DdRequest,
        onOutput: (String) -> Unit
    ): DdExecutionResult {
        if (!shell.hasRootAccess()) return DdExecutionResult.RootDenied

        val command = try {
            DdCommandBuilder.build(request)
        } catch (exception: IllegalArgumentException) {
            return DdExecutionResult.InvalidRequest(exception.message ?: "Invalid dd request")
        }

        val result = shell.execute(command, onOutput)
        return DdExecutionResult.Completed(
            exitCode = result.exitCode,
            stdout = result.stdout,
            stderr = result.stderr
        )
    }
}
