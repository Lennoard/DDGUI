package com.androidvip.ddgui.data.dd

import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

internal data class ShellCommandResult(
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>
)

internal interface RootShellDataSource {
    suspend fun hasRootAccess(): Boolean

    suspend fun execute(command: String, onOutput: (String) -> Unit): ShellCommandResult
}

internal class LibsuRootShellDataSource(
    private val ioDispatcher: CoroutineDispatcher
) : RootShellDataSource {
    override suspend fun hasRootAccess(): Boolean = withContext(ioDispatcher) {
        Shell.getShell().isRoot
    }

    override suspend fun execute(
        command: String,
        onOutput: (String) -> Unit
    ): ShellCommandResult = suspendCancellableCoroutine { continuation ->
        val stdout = outputCallback(onOutput)
        val stderr = outputCallback(onOutput)

        Shell.cmd(command)
            .to(stdout, stderr)
            .submit { shellResult ->
                if (continuation.isActive) {
                    continuation.resume(
                        ShellCommandResult(
                            exitCode = shellResult.code,
                            stdout = shellResult.out.toList(),
                            stderr = shellResult.err.toList()
                        )
                    )
                }
            }
    }

    private fun outputCallback(
        onOutput: (String) -> Unit
    ): CallbackList<String> = object : CallbackList<String>() {
        override fun onAddElement(element: String) {
            onOutput(element)
        }
    }
}
