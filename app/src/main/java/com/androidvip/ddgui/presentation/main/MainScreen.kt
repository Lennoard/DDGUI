package com.androidvip.ddgui.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.androidvip.ddgui.R

@Composable
internal fun MainScreen(
    state: MainUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (MainUiEvent) -> Unit,
    onDisclaimer: () -> Unit,
    onExit: () -> Unit
) {
    val executeDescription = stringResource(R.string.execute)
    Scaffold(
        topBar = {
            MainTopBar(
                onSettings = { onEvent(MainUiEvent.OpenSettings) },
                onDisclaimer = onDisclaimer,
                onExit = onExit
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.rootStatus == RootUiStatus.Ready &&
                state.executionStatus == ExecutionUiStatus.Idle &&
                state.executionReview == null
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
            IconButton(onClick = onSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.action_settings)
                )
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = stringResource(R.string.options)
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
