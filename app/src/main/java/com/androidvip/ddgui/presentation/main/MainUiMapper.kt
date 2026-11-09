package com.androidvip.ddgui.presentation.main

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.androidvip.ddgui.R
import com.androidvip.ddgui.domain.dd.DdConversion
import com.androidvip.ddgui.domain.dd.DdExecutionResult
import com.androidvip.ddgui.domain.dd.RootAccess

@Immutable
data class ExecutionResultUiModel(
    val status: ExecutionUiStatus,
    val message: ResultUiMessage? = null,
    val errorText: String? = null
)

fun RootAccess.toUiStatus(): RootUiStatus = when (this) {
    RootAccess.Granted -> RootUiStatus.Ready
    RootAccess.Denied -> RootUiStatus.Denied
}

fun DdExecutionResult.toUiModel(): ExecutionResultUiModel = when (this) {
    is DdExecutionResult.Completed -> ExecutionResultUiModel(
        status = if (exitCode == 0) ExecutionUiStatus.Succeeded else ExecutionUiStatus.Failed
    )

    DdExecutionResult.RootDenied -> ExecutionResultUiModel(
        status = ExecutionUiStatus.Failed,
        message = ResultUiMessage.RootDenied
    )

    is DdExecutionResult.InvalidRequest -> ExecutionResultUiModel(
        status = ExecutionUiStatus.Failed,
        message = ResultUiMessage.InvalidRequest,
        errorText = message
    )
}

@StringRes
internal fun DdConversion.toLabelRes(): Int = when (this) {
    DdConversion.NO_TRUNCATE -> R.string.conversion_no_truncate
    DdConversion.NO_ERROR -> R.string.conversion_no_error
    DdConversion.SYNC -> R.string.conversion_sync
    DdConversion.FSYNC -> R.string.conversionFsync
    DdConversion.SWAB -> R.string.conversionSwab
}
