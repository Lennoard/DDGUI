package com.androidvip.ddgui.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.androidvip.ddgui.R

@Composable
internal fun ExecutionReviewDialog(
    state: MainUiState,
    onEvent: (MainUiEvent) -> Unit
) {
    val executionReview = state.executionReview
    if (executionReview != null && state.showWarningDialog) {
        AlertDialog(
            onDismissRequest = {
                onEvent(MainUiEvent.CancelExecutionReview)
            },
            title = { Text(stringResource(R.string.destructive_review_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExecutionReviewDetails(executionReview)
                    WarningDialogPreference(
                        checked = state.showWarningDialog,
                        onCheckedChange = {
                            onEvent(MainUiEvent.WarningDialogChanged(it))
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onEvent(MainUiEvent.ConfirmExecution) }) {
                    Text(stringResource(R.string.confirm_write))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(MainUiEvent.CancelExecutionReview) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
internal fun ExecutionReviewCard(
    review: ExecutionReviewUiModel,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.destructive_review_title),
                style = MaterialTheme.typography.headlineSmall
            )
            ExecutionReviewDetails(review)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
                Button(onClick = onConfirm) {
                    Text(stringResource(R.string.confirm_write))
                }
            }
        }
    }
}

@Composable
private fun ExecutionReviewDetails(review: ExecutionReviewUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.destructive_review_warning),
            color = MaterialTheme.colorScheme.error
        )
        ReviewPath(stringResource(R.string.review_source), review.inputPath)
        ReviewPath(stringResource(R.string.review_destination), review.outputPath)
        Text(
            text = stringResource(R.string.review_conversions),
            style = MaterialTheme.typography.titleSmall
        )
        if (review.conversions.isEmpty()) {
            Text(stringResource(R.string.review_no_conversions))
        } else {
            review.conversions.forEach { conversion ->
                Text(stringResource(conversion.toLabelRes()))
            }
        }
    }
}

@Composable
private fun ReviewPath(label: String, path: String) {
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall)
        SelectionContainer {
            Text(path, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun WarningDialogPreference(
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
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = stringResource(R.string.show_write_warning),
            modifier = Modifier.weight(1f)
        )
    }
}
