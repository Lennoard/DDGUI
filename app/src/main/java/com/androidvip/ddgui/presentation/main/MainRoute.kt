package com.androidvip.ddgui.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.androidvip.ddgui.R
import com.androidvip.ddgui.presentation.files.FilePickerDialog
import com.androidvip.ddgui.presentation.files.FilePickerEvent
import com.androidvip.ddgui.presentation.files.FilePickerViewModel

private const val MAIN_DESTINATION = "main"
private const val SETTINGS_DESTINATION = "settings"

private val navigationBackStackSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() }
)

@Composable
fun MainRoute(
    mainViewModel: MainViewModel,
    filePickerViewModel: FilePickerViewModel,
    onExit: () -> Unit,
    onDisclaimer: () -> Unit
) {
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()
    val pickerState by filePickerViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val missingInputMessage = stringResource(R.string.error_missing_input)
    val missingOutputMessage = stringResource(R.string.error_missing_output)
    val settingsUnavailableMessage = stringResource(R.string.settings_unavailable)
    var pendingExitConfirmation by rememberSaveable { mutableStateOf(false) }
    val backStack = rememberSaveable(saver = navigationBackStackSaver) {
        mutableStateListOf(MAIN_DESTINATION)
    }

    LaunchedEffect(
        mainViewModel,
        lifecycleOwner,
        missingInputMessage,
        missingOutputMessage,
        settingsUnavailableMessage
    ) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            mainViewModel.effects.collect { effect ->
                when (effect) {
                    is MainUiEffect.ShowMessage -> {
                        val message = when (effect.message) {
                            MainUiMessage.MissingInput -> missingInputMessage
                            MainUiMessage.MissingOutput -> missingOutputMessage
                            MainUiMessage.SettingsUnavailable -> settingsUnavailableMessage
                        }
                        snackbarHostState.showSnackbar(message)
                    }

                    MainUiEffect.NavigateToSettings -> {
                        if (backStack.lastOrNull() != SETTINGS_DESTINATION) {
                            backStack.add(SETTINGS_DESTINATION)
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = state.pickerTarget == null && backStack.size == 1) {
        when {
            state.executionReview != null ->
                mainViewModel.onEvent(MainUiEvent.CancelExecutionReview)

            state.executionStatus == ExecutionUiStatus.Running ->
                pendingExitConfirmation = true

            state.executionStatus == ExecutionUiStatus.Succeeded ||
                state.executionStatus == ExecutionUiStatus.Failed ->
                mainViewModel.onEvent(MainUiEvent.ResetAfterExecution)

            else -> onExit()
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                MAIN_DESTINATION -> NavEntry(key) {
                    MainScreen(
                        state = state,
                        snackbarHostState = snackbarHostState,
                        onEvent = { event ->
                            mainViewModel.onEvent(event)
                            when (event) {
                                is MainUiEvent.OpenPicker ->
                                    filePickerViewModel.onEvent(FilePickerEvent.Open)

                                MainUiEvent.DismissPicker ->
                                    filePickerViewModel.onEvent(FilePickerEvent.Close)

                                else -> Unit
                            }
                        },
                        onDisclaimer = onDisclaimer,
                        onExit = onExit
                    )
                }

                SETTINGS_DESTINATION -> NavEntry(key) {
                    SettingsScreen(
                        state = state,
                        onEvent = mainViewModel::onEvent,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                else -> error("Unknown navigation destination: $key")
            }
        }
    )

    if (state.pickerTarget != null) {
        FilePickerDialog(
            state = pickerState,
            effects = filePickerViewModel.effects,
            onEvent = filePickerViewModel::onEvent,
            onFileSelected = { path ->
                mainViewModel.onEvent(MainUiEvent.FileSelected(path))
                filePickerViewModel.onEvent(FilePickerEvent.Close)
            },
            onDismiss = {
                mainViewModel.onEvent(MainUiEvent.DismissPicker)
            }
        )
    }

    if (pendingExitConfirmation && state.executionStatus == ExecutionUiStatus.Running) {
        AlertDialog(
            onDismissRequest = { pendingExitConfirmation = false },
            title = { Text(stringResource(android.R.string.dialog_alert_title)) },
            text = { Text(stringResource(R.string.ppending_operations_warning)) },
            confirmButton = {
                TextButton(onClick = onExit) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingExitConfirmation = false }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

    if (state.rootStatus == RootUiStatus.Denied) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(android.R.string.dialog_alert_title)) },
            text = { Text(stringResource(R.string.root_not_found)) },
            confirmButton = {
                TextButton(onClick = onExit) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }

    ExecutionReviewDialog(state = state, onEvent = mainViewModel::onEvent)
}
