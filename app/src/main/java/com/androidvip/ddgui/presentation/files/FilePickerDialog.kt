package com.androidvip.ddgui.presentation.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.androidvip.ddgui.R
import kotlinx.coroutines.flow.Flow

@Composable
fun FilePickerDialog(
    state: FilePickerUiState,
    effects: Flow<FilePickerEffect>,
    onEvent: (FilePickerEvent) -> Unit,
    onFileSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    val listingFailedMessage = stringResource(R.string.failed)
    val createFileFailedMessage = stringResource(R.string.create_file_fail)
    val createFolderFailedMessage = stringResource(R.string.create_folder_fail)
    val unexpectedFailureMessage = stringResource(R.string.unexpected_error)
    val upDescription = stringResource(R.string.up)
    val forwardDescription = stringResource(R.string.forward)
    val rootDescription = stringResource(R.string.root)
    val homeDescription = stringResource(R.string.home)
    val newFileDescription = stringResource(R.string.new_file)
    val newFolderDescription = stringResource(R.string.new_folder)

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 320.dp, max = 760.dp)
                .fillMaxHeight(0.88f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onEvent(FilePickerEvent.NavigateUp) },
                            modifier = Modifier.semantics {
                                contentDescription = upDescription
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_directory_up),
                                contentDescription = null
                            )
                        }
                        IconButton(
                            onClick = { onEvent(FilePickerEvent.NavigateForward) },
                            enabled = state.forwardPath != null,
                            modifier = Modifier.semantics {
                                contentDescription = forwardDescription
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_directory_forward),
                                contentDescription = null
                            )
                        }
                        Text(
                            text = state.currentPath,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { onEvent(FilePickerEvent.Refresh) }) {
                            Text(stringResource(R.string.refresh))
                        }
                    }
                    HorizontalDivider()

                    if (state.listingFailed) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(R.string.failed),
                                color = MaterialTheme.colorScheme.error
                            )
                            state.errorMessage?.takeIf(String::isNotBlank)?.let {
                                Text(it, modifier = Modifier.padding(top = 4.dp))
                            }
                            TextButton(onClick = { onEvent(FilePickerEvent.Refresh) }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    } else if (state.entries.isEmpty() && !state.isLoading) {
                        Text(
                            text = stringResource(R.string.empty_directory),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    LazyColumn(Modifier.weight(1f)) {
                        items(state.entries, key = FileEntryUiModel::path) { entry ->
                            val entryDescription = stringResource(
                                if (entry.isDirectory) R.string.directory_entry else R.string.file_entry,
                                entry.name
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEvent(FilePickerEvent.Select(entry)) }
                                    .semantics {
                                        contentDescription = entryDescription
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (entry.isDirectory) R.drawable.ic_folder else R.drawable.ic_file
                                    ),
                                    modifier = Modifier.padding(end = 16.dp),
                                    contentDescription = null,
                                    tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = entry.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            HorizontalDivider()
                        }
                    }

                    if (state.isLoading) {
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onEvent(FilePickerEvent.NavigateToRoot) },
                            modifier = Modifier.semantics {
                                contentDescription = rootDescription
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_root_folder_circle),
                                contentDescription = null
                            )
                        }
                        IconButton(
                            onClick = { onEvent(FilePickerEvent.NavigateToHome) },
                            modifier = Modifier.semantics {
                                contentDescription = homeDescription
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_home_folder_circle),
                                contentDescription = null
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                onEvent(FilePickerEvent.ShowCreateFile)
                            },
                            modifier = Modifier.semantics {
                                contentDescription = newFileDescription
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_file_new),
                                contentDescription = null
                            )
                        }
                        IconButton(
                            onClick = {
                                onEvent(FilePickerEvent.ShowCreateFolder)
                            },
                            modifier = Modifier.semantics {
                                contentDescription = newFolderDescription
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_folder_new),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }

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
                    singleLine = true,
                    label = { Text(stringResource(R.string.path)) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (state.newEntryName.isNotBlank()) {
                            if (type == NewEntryType.File) {
                                onEvent(FilePickerEvent.CreateFile(state.newEntryName))
                            } else {
                                onEvent(FilePickerEvent.CreateFolder(state.newEntryName))
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
