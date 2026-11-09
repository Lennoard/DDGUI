package com.androidvip.ddgui.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.androidvip.ddgui.R
import com.androidvip.ddgui.domain.dd.DdConversion
import com.androidvip.ddgui.presentation.files.FilePickerDialog
import com.androidvip.ddgui.presentation.files.FilePickerEvent
import com.androidvip.ddgui.presentation.files.FilePickerViewModel

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
    var pendingExitConfirmation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(mainViewModel, lifecycleOwner, missingInputMessage, missingOutputMessage) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            mainViewModel.effects.collect { effect ->
                val message = when (effect) {
                    is MainUiEffect.ShowMessage -> when (effect.message) {
                        MainUiMessage.MissingInput -> missingInputMessage
                        MainUiMessage.MissingOutput -> missingOutputMessage
                    }
                }
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    BackHandler(enabled = state.pickerTarget == null) {
        when (state.executionStatus) {
            ExecutionUiStatus.Running -> pendingExitConfirmation = true
            ExecutionUiStatus.Succeeded, ExecutionUiStatus.Failed ->
                mainViewModel.onEvent(MainUiEvent.ResetAfterExecution)
            ExecutionUiStatus.Idle -> onExit()
        }
    }

    MainScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = { event ->
            mainViewModel.onEvent(event)
            when (event) {
                is MainUiEvent.OpenPicker -> filePickerViewModel.onEvent(FilePickerEvent.Open)
                MainUiEvent.DismissPicker -> filePickerViewModel.onEvent(FilePickerEvent.Close)
                else -> Unit
            }
        },
        onSettings = {},
        onDisclaimer = onDisclaimer,
        onExit = onExit
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
}

@Composable
private fun MainScreen(
    state: MainUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (MainUiEvent) -> Unit,
    onSettings: () -> Unit,
    onDisclaimer: () -> Unit,
    onExit: () -> Unit
) {
    val executeDescription = stringResource(R.string.execute)
    Scaffold(
        topBar = {
            MainTopBar(
                onSettings = onSettings,
                onDisclaimer = onDisclaimer,
                onExit = onExit
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.rootStatus == RootUiStatus.Ready &&
                state.executionStatus == ExecutionUiStatus.Idle
            ) {
                FloatingActionButton(
                    onClick = { onEvent(MainUiEvent.Execute) },
                    modifier = Modifier.semantics {
                        contentDescription = executeDescription
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null
                    )
                }
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when (state.rootStatus) {
                RootUiStatus.Checking -> LoadingPanel()
                RootUiStatus.Denied -> Unit
                RootUiStatus.Failed -> RootFailurePanel(
                    message = state.rootErrorMessage,
                    onRetry = { onEvent(MainUiEvent.CheckRootAccess) }
                )
                RootUiStatus.Ready -> when (state.executionStatus) {
                    ExecutionUiStatus.Idle -> MainForm(
                        state = state,
                        onEvent = onEvent
                    )

                    ExecutionUiStatus.Running,
                    ExecutionUiStatus.Succeeded,
                    ExecutionUiStatus.Failed -> ResultPanel(
                        state = state,
                        onReset = { onEvent(MainUiEvent.ResetAfterExecution) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    onSettings: () -> Unit,
    onDisclaimer: () -> Unit,
    onExit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(stringResource(R.string.options))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_settings)) },
                        onClick = {
                            expanded = false
                            onSettings()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.disclaimer)) },
                        onClick = {
                            expanded = false
                            onDisclaimer()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.exit)) },
                        onClick = {
                            expanded = false
                            onExit()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun MainForm(
    state: MainUiState,
    onEvent: (MainUiEvent) -> Unit
) {
    val outputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val expandedLayout = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
    ) && windowSizeClass.isHeightAtLeastBreakpoint(
        WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
    )
    val mediumLayout = !expandedLayout && windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    )
    val horizontalPadding = when {
        expandedLayout -> 32.dp
        mediumLayout -> 24.dp
        else -> 16.dp
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 1200.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.basic),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (expandedLayout) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        modifier = Modifier.weight(1.4f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InputPathCard(state.inputPath, onEvent, outputFocusRequester)
                        OutputPathCard(
                            state.outputPath,
                            onEvent,
                            outputFocusRequester,
                            keyboardController
                        )
                    }
                    ConversionCard(
                        state = state,
                        onEvent = onEvent,
                        twoColumns = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (mediumLayout) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputPathCard(
                        state.inputPath,
                        onEvent,
                        outputFocusRequester,
                        modifier = Modifier.weight(1f)
                    )
                    OutputPathCard(
                        state.outputPath,
                        onEvent,
                        outputFocusRequester,
                        keyboardController,
                        modifier = Modifier.weight(1f)
                    )
                }
                ConversionCard(state = state, onEvent = onEvent, twoColumns = true)
            } else {
                InputPathCard(state.inputPath, onEvent, outputFocusRequester)
                OutputPathCard(
                    state.outputPath,
                    onEvent,
                    outputFocusRequester,
                    keyboardController
                )
                ConversionCard(state = state, onEvent = onEvent, twoColumns = false)
            }
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun InputPathCard(
    path: String,
    onEvent: (MainUiEvent) -> Unit,
    outputFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    PathCard(
        title = stringResource(R.string.input),
        value = path,
        onValueChanged = { onEvent(MainUiEvent.InputChanged(it)) },
        onPick = { onEvent(MainUiEvent.OpenPicker(PickerTarget.Input)) },
        modifier = modifier,
        imeAction = ImeAction.Next,
        onImeAction = { outputFocusRequester.requestFocus() }
    )
}

@Composable
private fun OutputPathCard(
    path: String,
    onEvent: (MainUiEvent) -> Unit,
    outputFocusRequester: FocusRequester,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    modifier: Modifier = Modifier
) {
    PathCard(
        title = stringResource(R.string.output),
        value = path,
        onValueChanged = { onEvent(MainUiEvent.OutputChanged(it)) },
        onPick = { onEvent(MainUiEvent.OpenPicker(PickerTarget.Output)) },
        modifier = modifier,
        imeAction = ImeAction.Done,
        focusRequester = outputFocusRequester,
        onImeAction = { keyboardController?.hide() }
    )
}

@Composable
private fun ConversionCard(
    state: MainUiState,
    onEvent: (MainUiEvent) -> Unit,
    twoColumns: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.conversions),
                style = MaterialTheme.typography.titleMedium
            )
            if (twoColumns) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        ConversionOption(state, onEvent, DdConversion.NO_TRUNCATE)
                        ConversionOption(state, onEvent, DdConversion.NO_ERROR)
                        ConversionOption(state, onEvent, DdConversion.SYNC)
                    }
                    Column(Modifier.weight(1f)) {
                        ConversionOption(state, onEvent, DdConversion.FSYNC)
                        ConversionOption(state, onEvent, DdConversion.SWAB)
                    }
                }
            } else {
                ConversionOption(state, onEvent, DdConversion.NO_TRUNCATE)
                ConversionOption(state, onEvent, DdConversion.NO_ERROR)
                ConversionOption(state, onEvent, DdConversion.SYNC)
                ConversionOption(state, onEvent, DdConversion.FSYNC)
                ConversionOption(state, onEvent, DdConversion.SWAB)
            }
        }
    }
}

@Composable
private fun ConversionOption(
    state: MainUiState,
    onEvent: (MainUiEvent) -> Unit,
    conversion: DdConversion
) {
    val label = when (conversion) {
        DdConversion.NO_TRUNCATE -> stringResource(R.string.conversion_no_truncate)
        DdConversion.NO_ERROR -> stringResource(R.string.conversion_no_error)
        DdConversion.SYNC -> stringResource(R.string.conversion_sync)
        DdConversion.FSYNC -> stringResource(R.string.conversionFsync)
        DdConversion.SWAB -> stringResource(R.string.conversionSwab)
    }
    val checked = when (conversion) {
        DdConversion.NO_TRUNCATE -> state.noTruncate
        DdConversion.NO_ERROR -> state.noError
        DdConversion.SYNC -> state.sync
        DdConversion.FSYNC -> state.fsync
        DdConversion.SWAB -> state.swab
    }
    ConversionCheck(
        text = label,
        checked = checked,
        onCheckedChange = { onEvent(MainUiEvent.ConversionChanged(conversion, it)) }
    )
}

@Composable
private fun PathCard(
    title: String,
    value: String,
    onValueChanged: (String) -> Unit,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction,
    focusRequester: FocusRequester? = null,
    onImeAction: () -> Unit
) {
    Card(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
                    ),
                label = { Text(stringResource(R.string.path)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onImeAction() },
                    onDone = { onImeAction() }
                )
            )
            Button(onClick = onPick, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.pick))
            }
        }
    }
}

@Composable
private fun ConversionCheck(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(text = text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ResultPanel(
    state: MainUiState,
    onReset: () -> Unit
) {
    val title = when (state.executionStatus) {
        ExecutionUiStatus.Running -> stringResource(R.string.executing)
        ExecutionUiStatus.Succeeded -> stringResource(R.string.finished)
        ExecutionUiStatus.Failed -> stringResource(R.string.failed)
        ExecutionUiStatus.Idle -> return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (state.executionStatus == ExecutionUiStatus.Running) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
        state.resultMessage?.let { message ->
            val text = when (message) {
                ResultUiMessage.RootDenied -> stringResource(R.string.root_not_found)
                ResultUiMessage.InvalidRequest -> stringResource(R.string.failed)
                ResultUiMessage.UnexpectedFailure -> stringResource(R.string.unexpected_error)
            }
            Text(
                text = text,
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
        if (state.outputText.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            SelectionContainer {
                Text(
                    text = state.outputText,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (state.executionStatus != ExecutionUiStatus.Running) {
            TextButton(onClick = onReset, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.new_operation))
            }
        }
    }
}

@Composable
private fun LoadingPanel() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(stringResource(R.string.checking_root), modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun RootFailurePanel(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.unexpected_error),
            color = MaterialTheme.colorScheme.error
        )
        if (!message.isNullOrBlank()) {
            Text(message, modifier = Modifier.padding(top = 8.dp))
        }
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}
