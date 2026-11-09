package com.androidvip.ddgui.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androidvip.ddgui.R

@Composable
internal fun ResultPanel(
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
internal fun LoadingPanel() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(stringResource(R.string.checking_root), modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
internal fun RootFailurePanel(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
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
