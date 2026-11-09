package com.androidvip.ddgui.presentation.main

import androidx.compose.runtime.Immutable
import com.androidvip.ddgui.domain.dd.DdConversion
import com.androidvip.ddgui.domain.dd.DdRequest
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
data class MainUiState(
    val rootStatus: RootUiStatus = RootUiStatus.Checking,
    val rootErrorMessage: String? = null,
    val inputPath: String = "",
    val outputPath: String = "",
    val noTruncate: Boolean = false,
    val noError: Boolean = false,
    val sync: Boolean = false,
    val fsync: Boolean = false,
    val swab: Boolean = false,
    val executionStatus: ExecutionUiStatus = ExecutionUiStatus.Idle,
    val resultMessage: ResultUiMessage? = null,
    val outputText: String = "",
    val pickerTarget: PickerTarget? = null,
    val executionReview: ExecutionReviewUiModel? = null,
    val showWarningDialog: Boolean = true
)

@Immutable
data class ExecutionReviewUiModel(
    val inputPath: String,
    val outputPath: String,
    val conversions: ImmutableList<DdConversion>
)

enum class RootUiStatus {
    Checking,
    Ready,
    Denied,
    Failed
}

enum class ExecutionUiStatus {
    Idle,
    Running,
    Succeeded,
    Failed
}

enum class ResultUiMessage {
    RootDenied,
    InvalidRequest,
    UnexpectedFailure
}

enum class PickerTarget {
    Input,
    Output
}

enum class MainUiMessage {
    MissingInput,
    MissingOutput,
    SettingsUnavailable
}

@Immutable
sealed interface MainUiEffect {
    data class ShowMessage(val message: MainUiMessage) : MainUiEffect
    data object NavigateToSettings : MainUiEffect
}

@Immutable
sealed interface MainUiEvent {
    data class InputChanged(val path: String) : MainUiEvent
    data class OutputChanged(val path: String) : MainUiEvent
    data class ConversionChanged(
        val conversion: DdConversion,
        val enabled: Boolean
    ) : MainUiEvent

    data class OpenPicker(val target: PickerTarget) : MainUiEvent
    data object DismissPicker : MainUiEvent
    data class FileSelected(val path: String) : MainUiEvent
    data object Execute : MainUiEvent
    data object ConfirmExecution : MainUiEvent
    data object CancelExecutionReview : MainUiEvent
    data object OpenSettings : MainUiEvent
    data class WarningDialogChanged(val enabled: Boolean) : MainUiEvent
    data object ResetAfterExecution : MainUiEvent
    data object CheckRootAccess : MainUiEvent
}

fun MainUiState.toDdRequest(): DdRequest = DdRequest(
    inputPath = inputPath.trim(),
    outputPath = outputPath.trim(),
    conversions = buildList {
        if (noTruncate) add(DdConversion.NO_TRUNCATE)
        if (noError) add(DdConversion.NO_ERROR)
        if (sync) add(DdConversion.SYNC)
        if (fsync) add(DdConversion.FSYNC)
        if (swab) add(DdConversion.SWAB)
    }
)

fun DdRequest.toExecutionReviewUiModel(): ExecutionReviewUiModel =
    ExecutionReviewUiModel(
        inputPath = inputPath,
        outputPath = outputPath,
        conversions = conversions.toPersistentList()
    )
