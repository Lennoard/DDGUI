package com.androidvip.ddgui.domain.dd

enum class DdConversion(val argument: String) {
    NO_TRUNCATE("notrunc"),
    NO_ERROR("noerror"),
    SYNC("sync"),
    FSYNC("fsync"),
    SWAB("swab")
}

data class DdRequest(
    val inputPath: String,
    val outputPath: String,
    val conversions: List<DdConversion>
)

sealed interface RootAccess {
    data object Granted : RootAccess
    data object Denied : RootAccess
}

sealed interface DdExecutionResult {
    data class Completed(
        val exitCode: Int,
        val stdout: List<String>,
        val stderr: List<String>
    ) : DdExecutionResult

    data object RootDenied : DdExecutionResult
    data class InvalidRequest(val message: String) : DdExecutionResult
}

interface DdRepository {
    suspend fun checkRootAccess(): RootAccess

    suspend fun execute(
        request: DdRequest,
        onOutput: (String) -> Unit
    ): DdExecutionResult
}
