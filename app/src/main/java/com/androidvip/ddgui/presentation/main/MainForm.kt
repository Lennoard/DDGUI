package com.androidvip.ddgui.presentation.main

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import com.androidvip.ddgui.R
import com.androidvip.ddgui.domain.dd.DdConversion

@Composable
internal fun MainForm(
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
            if (state.executionReview != null && !state.showWarningDialog) {
                ExecutionReviewCard(
                    review = state.executionReview,
                    onConfirm = { onEvent(MainUiEvent.ConfirmExecution) },
                    onCancel = { onEvent(MainUiEvent.CancelExecutionReview) }
                )
            } else {
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
    keyboardController: SoftwareKeyboardController?,
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
    val label = stringResource(conversion.toLabelRes())
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
