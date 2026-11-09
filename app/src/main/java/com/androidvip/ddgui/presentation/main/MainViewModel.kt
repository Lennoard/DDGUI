package com.androidvip.ddgui.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidvip.ddgui.domain.dd.DdConversion
import com.androidvip.ddgui.domain.dd.DdRepository
import com.androidvip.ddgui.domain.dd.DdRequest
import com.androidvip.ddgui.domain.settings.DestructiveOperationSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val ddRepository: DdRepository,
    private val settingsRepository: DestructiveOperationSettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val effectChannel = Channel<MainUiEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private var pendingRequest: DdRequest? = null

    init {
        loadSettings()
        onEvent(MainUiEvent.CheckRootAccess)
    }

    fun onEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.InputChanged ->
                _uiState.update { it.copy(inputPath = event.path) }

            is MainUiEvent.OutputChanged ->
                _uiState.update { it.copy(outputPath = event.path) }

            is MainUiEvent.ConversionChanged -> setConversion(event.conversion, event.enabled)
            is MainUiEvent.OpenPicker ->
                _uiState.update { it.copy(pickerTarget = event.target) }

            MainUiEvent.DismissPicker ->
                _uiState.update { it.copy(pickerTarget = null) }

            is MainUiEvent.FileSelected -> onFileSelected(event.path)
            MainUiEvent.Execute -> requestExecution()
            MainUiEvent.ConfirmExecution -> confirmExecution()
            MainUiEvent.CancelExecutionReview -> cancelExecutionReview()
            MainUiEvent.OpenSettings ->
                effectChannel.trySend(MainUiEffect.NavigateToSettings)

            is MainUiEvent.WarningDialogChanged -> setWarningDialog(event.enabled)
            MainUiEvent.ResetAfterExecution -> resetAfterExecution()
            MainUiEvent.CheckRootAccess -> checkRootAccess()
        }
    }

    private fun loadSettings() {
        try {
            _uiState.update { it.copy(showWarningDialog = settingsRepository.isWarningDialogEnabled()) }
        } catch (exception: Exception) {
            _uiState.update { it.copy(showWarningDialog = true) }
            emit(MainUiMessage.SettingsUnavailable)
        }
    }

    private fun setWarningDialog(enabled: Boolean) {
        try {
            settingsRepository.setWarningDialogEnabled(enabled)
            _uiState.update { it.copy(showWarningDialog = enabled) }
        } catch (exception: Exception) {
            emit(MainUiMessage.SettingsUnavailable)
        }
    }

    private fun checkRootAccess() {
        _uiState.update { it.copy(rootStatus = RootUiStatus.Checking) }
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        rootStatus = ddRepository.checkRootAccess().toUiStatus(),
                        rootErrorMessage = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        rootStatus = RootUiStatus.Failed,
                        rootErrorMessage = exception.message
                    )
                }
            }
        }
    }

    private fun setConversion(conversion: DdConversion, enabled: Boolean) {
        _uiState.update { state ->
            when (conversion) {
                DdConversion.NO_TRUNCATE -> state.copy(noTruncate = enabled)
                DdConversion.NO_ERROR -> state.copy(noError = enabled)
                DdConversion.SYNC -> state.copy(sync = enabled)
                DdConversion.FSYNC -> state.copy(fsync = enabled)
                DdConversion.SWAB -> state.copy(swab = enabled)
            }
        }
    }

    private fun onFileSelected(path: String) {
        _uiState.update { state ->
            when (state.pickerTarget) {
                PickerTarget.Input -> state.copy(inputPath = path, pickerTarget = null)
                PickerTarget.Output -> state.copy(outputPath = path, pickerTarget = null)
                null -> state
            }
        }
    }

    private fun requestExecution() {
        val current = _uiState.value
        when {
            current.inputPath.isBlank() -> emit(MainUiMessage.MissingInput)
            current.outputPath.isBlank() -> emit(MainUiMessage.MissingOutput)
            current.rootStatus != RootUiStatus.Ready -> return
            current.executionStatus == ExecutionUiStatus.Running -> return
            current.executionReview != null -> return
            else -> {
                val request = current.toDdRequest()
                pendingRequest = request
                _uiState.update {
                    it.copy(executionReview = request.toExecutionReviewUiModel())
                }
            }
        }
    }

    private fun confirmExecution() {
        val request = pendingRequest ?: return
        val state = _uiState.value
        if (state.rootStatus != RootUiStatus.Ready ||
            state.executionStatus != ExecutionUiStatus.Idle ||
            state.executionReview == null
        ) {
            cancelExecutionReview()
            return
        }
        pendingRequest = null
        _uiState.update { it.copy(executionReview = null) }
        execute(request)
    }

    private fun cancelExecutionReview() {
        pendingRequest = null
        _uiState.update { it.copy(executionReview = null) }
    }

    private fun resetAfterExecution() {
        _uiState.update { state ->
            if (state.executionStatus == ExecutionUiStatus.Running) {
                state
            } else {
                state.copy(
                    noTruncate = false,
                    noError = false,
                    sync = false,
                    fsync = false,
                    swab = false,
                    executionReview = null,
                    executionStatus = ExecutionUiStatus.Idle,
                    resultMessage = null,
                    outputText = ""
                )
            }
        }
    }

    private fun execute(request: DdRequest) {
        _uiState.update {
            it.copy(
                executionStatus = ExecutionUiStatus.Running,
                resultMessage = null,
                outputText = ""
            )
        }
        viewModelScope.launch {
            try {
                val result = ddRepository.execute(request) { line ->
                    _uiState.update { state ->
                        state.copy(outputText = appendLine(state.outputText, line))
                    }
                }.toUiModel()
                _uiState.update { state ->
                    state.copy(
                        executionStatus = result.status,
                        resultMessage = result.message,
                        outputText = result.errorText ?: state.outputText
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        executionStatus = ExecutionUiStatus.Failed,
                        resultMessage = ResultUiMessage.UnexpectedFailure,
                        outputText = exception.message.orEmpty()
                    )
                }
            }
        }
    }

    private fun emit(message: MainUiMessage) {
        viewModelScope.launch {
            effectChannel.send(MainUiEffect.ShowMessage(message))
        }
    }

    private fun appendLine(existing: String, line: String): String =
        if (existing.isEmpty()) line else "$existing\n$line"
}
