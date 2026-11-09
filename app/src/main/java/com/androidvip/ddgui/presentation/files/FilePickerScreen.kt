package com.androidvip.ddgui.presentation.files

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.androidvip.ddgui.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow

@Composable
fun FilePickerScreen(
    state: FilePickerUiState,
    effects: Flow<FilePickerEffect>,
    onEvent: (FilePickerEvent) -> Unit,
    onFileSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listingFailedMessage = stringResource(R.string.failed)
    val createFileFailedMessage = stringResource(R.string.create_file_fail)
    val createFolderFailedMessage = stringResource(R.string.create_folder_fail)
    val unexpectedFailureMessage = stringResource(R.string.unexpected_error)

    LaunchedEffect(
        effects,
        lifecycleOwner,
        listingFailedMessage,
        createFileFailedMessage,
        createFolderFailedMessage,
        unexpectedFailureMessage
    ) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effects.collect { effect ->
                when (effect) {
                    is FilePickerEffect.FileSelected -> onFileSelected(effect.path)
                    is FilePickerEffect.ShowMessage -> {
                        val message = when (effect.message) {
                            FilePickerMessage.ListingFailed -> listingFailedMessage
                            FilePickerMessage.CreateFileFailed -> createFileFailedMessage
                            FilePickerMessage.CreateFolderFailed -> createFolderFailedMessage
                            FilePickerMessage.UnexpectedFailure -> unexpectedFailureMessage
                        }
                        snackbarHostState.showSnackbar(message)
                    }
                }
            }
        }
    }

    FilePickerContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = onEvent,
        onDismiss = onDismiss
    )

    state.newEntryType?.let { type ->
        AlertDialog(
            onDismissRequest = { onEvent(FilePickerEvent.DismissCreateEntry) },
            title = {
                Text(
                    stringResource(
                        if (type == NewEntryType.File) R.string.new_file else R.string.new_folder
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = state.newEntryName,
                    onValueChange = { onEvent(FilePickerEvent.NewEntryNameChanged(it)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (state.newEntryName.isNotBlank()) {
                            when (type) {
                                NewEntryType.File -> {
                                    onEvent(FilePickerEvent.CreateFile(state.newEntryName))
                                }

                                NewEntryType.Folder -> {
                                    onEvent(FilePickerEvent.CreateFolder(state.newEntryName))
                                }
                            }
                        }
                    },
                    enabled = state.newEntryName.isNotBlank()
                ) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(FilePickerEvent.DismissCreateEntry) }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
