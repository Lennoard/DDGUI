package com.androidvip.ddgui.presentation.files

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.androidvip.ddgui.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.first
import java.io.File

@Composable
internal fun FilePickerContent(
    state: FilePickerUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (FilePickerEvent) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler {
        if (state.currentPath == "/") {
            onDismiss()
        } else {
            onEvent(FilePickerEvent.NavigateUp)
        }
    }

    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val wideLandscape = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
    ) && !windowSizeClass.isHeightAtLeastBreakpoint(
        WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
    )
    val compactHeight = !windowSizeClass.isHeightAtLeastBreakpoint(
        WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
    )
    val columnsState = rememberLazyListState()
    val previousColumnCount = remember { mutableIntStateOf(state.directoryHistory.size) }
    LaunchedEffect(state.directoryHistory, wideLandscape) {
        val columnCount = state.directoryHistory.size
        if (wideLandscape && columnCount > previousColumnCount.intValue) {
            val lastIndex = columnCount - 1
            snapshotFlow { columnsState.layoutInfo.totalItemsCount }
                .first { itemCount -> itemCount > lastIndex }
            columnsState.animateScrollToItem(lastIndex)
        }
        previousColumnCount.intValue = columnCount
    }

    val rootDescription = stringResource(R.string.root)
    val homeDescription = stringResource(R.string.home)
    val newFileDescription = stringResource(R.string.new_file)
    val newFolderDescription = stringResource(R.string.new_folder)

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                FilePickerToolbar(
                    compactHeight = compactHeight,
                    onBack = onDismiss,
                    breadcrumbs = {
                        if (!compactHeight) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .padding(start = 4.dp, end = 16.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onEvent(FilePickerEvent.NavigateUp) },
                                    enabled = state.currentPath != "/"
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_directory_up),
                                        contentDescription = stringResource(R.string.up)
                                    )
                                }
                                IconButton(
                                    onClick = { onEvent(FilePickerEvent.NavigateForward) },
                                    enabled = state.forwardPath != null
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_directory_forward),
                                        contentDescription = stringResource(R.string.forward)
                                    )
                                }
                                Breadcrumbs(
                                    path = state.currentPath,
                                    onNavigate = { onEvent(FilePickerEvent.NavigateToPath(it)) },
                                    modifier = Modifier.weight(1f),
                                    compactHeight = false
                                )
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (state.listingFailed) {
                    ListingFailure(
                        state = state,
                        onRetry = { onEvent(FilePickerEvent.Refresh) }
                    )
                } else if (wideLandscape) {
                    DirectoryColumns(
                        state = state,
                        listState = columnsState,
                        onEvent = onEvent,
                        compactHeight = compactHeight,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    if (state.entries.isEmpty() && !state.isLoading) {
                        EmptyDirectory(Modifier.fillMaxWidth())
                    }
                    FileEntryGrid(
                        modifier = Modifier.weight(1f),
                        entries = state.entries,
                        fileSizes = state.fileSizes,
                        onEvent = onEvent,
                        compactHeight = compactHeight
                    )
                }

                AnimatedVisibility(visible = state.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (compactHeight) 4.dp else 8.dp,
                            vertical = if (compactHeight) 0.dp else 4.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (compactHeight) {
                        IconButton(
                            onClick = { onEvent(FilePickerEvent.NavigateUp) },
                            enabled = state.currentPath != "/",
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_directory_up),
                                contentDescription = stringResource(R.string.up)
                            )
                        }
                        IconButton(
                            onClick = { onEvent(FilePickerEvent.NavigateForward) },
                            enabled = state.forwardPath != null,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_directory_forward),
                                contentDescription = stringResource(R.string.forward)
                            )
                        }
                        Breadcrumbs(
                            path = state.currentPath,
                            onNavigate = { onEvent(FilePickerEvent.NavigateToPath(it)) },
                            modifier = Modifier.weight(1f),
                            compactHeight = true
                        )
                    }
                    IconButton(
                        onClick = { onEvent(FilePickerEvent.NavigateToRoot) },
                        modifier = (if (compactHeight) Modifier.size(40.dp) else Modifier)
                            .semantics { contentDescription = rootDescription }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_root_folder_circle),
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = { onEvent(FilePickerEvent.NavigateToHome) },
                        modifier = (if (compactHeight) Modifier.size(40.dp) else Modifier)
                            .semantics { contentDescription = homeDescription }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_home_folder_circle),
                            contentDescription = null
                        )
                    }
                    if (!compactHeight) Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { onEvent(FilePickerEvent.ShowCreateFile) },
                        modifier = (if (compactHeight) Modifier.size(40.dp) else Modifier)
                            .semantics { contentDescription = newFileDescription }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_file_new),
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = { onEvent(FilePickerEvent.ShowCreateFolder) },
                        modifier = (if (compactHeight) Modifier.size(40.dp) else Modifier)
                            .semantics { contentDescription = newFolderDescription }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilePickerToolbar(
    compactHeight: Boolean,
    onBack: () -> Unit,
    breadcrumbs: @Composable () -> Unit
) {
    Column {
        if (compactHeight) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.back)
                    )
                }
                Text(
                    text = stringResource(R.string.file_picker_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            TopAppBar(
                title = { Text(stringResource(R.string.file_picker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
        if (!compactHeight) breadcrumbs()
        HorizontalDivider()
    }
}

@Composable
private fun Breadcrumbs(
    path: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    compactHeight: Boolean
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val breadcrumbs = remember(path) { buildBreadcrumbs(path) }
        breadcrumbs.forEachIndexed { index, (breadcrumbPath, label) ->
            if (index > 0) {
                Text(
                    text = "›",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
            TextButton(
                onClick = { onNavigate(breadcrumbPath) },
                contentPadding = PaddingValues(
                    horizontal = if (compactHeight) 2.dp else 4.dp,
                    vertical = 0.dp
                )
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = if (compactHeight) MaterialTheme.typography.labelMedium
                    else MaterialTheme.typography.labelLarge
                )
            }
        }
    }
    LaunchedEffect(path, scrollState.maxValue) {
        scrollState.scrollTo(scrollState.maxValue)
    }
}

private fun buildBreadcrumbs(path: String): List<Pair<String, String>> {
    val segments = path.split('/').filter(String::isNotEmpty)
    val breadcrumbs = mutableListOf("/" to "/")
    var accumulatedPath = ""
    segments.forEach { segment ->
        accumulatedPath += "/$segment"
        breadcrumbs += accumulatedPath to segment
    }
    return breadcrumbs
}

@Composable
private fun ListingFailure(state: FilePickerUiState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.failed), color = MaterialTheme.colorScheme.error)
        state.errorMessage?.takeIf(String::isNotBlank)?.let {
            Text(it, modifier = Modifier.padding(top = 4.dp))
        }
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun EmptyDirectory(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.empty_directory),
        modifier = modifier.padding(24.dp),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun DirectoryColumns(
    state: FilePickerUiState,
    listState: LazyListState,
    onEvent: (FilePickerEvent) -> Unit,
    compactHeight: Boolean,
    modifier: Modifier = Modifier
) {
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.directoryHistory, key = { it }) { path ->
            val isCurrentPath = path == state.currentPath
            val initiallyVisible = !isCurrentPath || state.directoryHistory.size == 1
            val visibility = remember(path) { MutableTransitionState(initiallyVisible) }
            LaunchedEffect(path) {
                visibility.targetState = true
            }
            AnimatedVisibility(
                visibleState = visibility,
                enter = fadeIn(tween(220)) +
                    slideInHorizontally(tween(280)) { fullWidth -> fullWidth / 3 },
                exit = fadeOut(tween(120)) +
                    slideOutHorizontally(tween(180)) { fullWidth -> fullWidth / 4 }
            ) {
                DirectoryColumn(
                    path = path,
                    entries = if (isCurrentPath) {
                        state.entries
                    } else {
                        state.directoryEntries[path] ?: persistentListOf()
                    },
                    fileSizes = state.fileSizes,
                    isLoading = isCurrentPath && state.isLoading,
                    onEvent = onEvent,
                    compactHeight = compactHeight
                )
            }
        }
    }
}

@Composable
private fun DirectoryColumn(
    path: String,
    entries: ImmutableList<FileEntryUiModel>,
    fileSizes: ImmutableMap<String, FileSizeUiState>,
    isLoading: Boolean,
    onEvent: (FilePickerEvent) -> Unit,
    compactHeight: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            Text(
                text = File(path).name.ifEmpty { path },
                modifier = Modifier.padding(
                    horizontal = if (compactHeight) 12.dp else 16.dp,
                    vertical = if (compactHeight) 5.dp else 12.dp
                ),
                style = if (compactHeight) MaterialTheme.typography.labelLarge
                else MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            HorizontalDivider()
            if (entries.isEmpty() && !isLoading) {
                EmptyDirectory(Modifier.fillMaxWidth())
            }
            FileEntryGrid(
                modifier = Modifier.weight(1f),
                entries = entries,
                fileSizes = fileSizes,
                onEvent = onEvent,
                columns = GridCells.Fixed(1),
                compactHeight = compactHeight
            )
            AnimatedVisibility(visible = isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FileEntryGrid(
    modifier: Modifier = Modifier,
    entries: ImmutableList<FileEntryUiModel>,
    fileSizes: ImmutableMap<String, FileSizeUiState>,
    onEvent: (FilePickerEvent) -> Unit,
    columns: GridCells = GridCells.Adaptive(minSize = 220.dp),
    compactHeight: Boolean = false,
) {
    LazyVerticalGrid(
        columns = columns,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries, key = FileEntryUiModel::path) { entry ->
            FileEntryItem(
                entry = entry,
                size = fileSizes[entry.path],
                onEvent = onEvent,
                compactHeight = compactHeight
            )
        }
    }
}

@Composable
private fun FileEntryItem(
    entry: FileEntryUiModel,
    size: FileSizeUiState?,
    onEvent: (FilePickerEvent) -> Unit,
    compactHeight: Boolean
) {
    val entryDescription = stringResource(
        if (entry.isDirectory) R.string.directory_entry else R.string.file_entry,
        entry.name
    )
    if (entry.isFile) {
        LaunchedEffect(entry.path) {
            onEvent(FilePickerEvent.LoadFileSize(entry.path))
        }
    }
    val sizeText = when (size) {
        is FileSizeUiState.Available ->
            Formatter.formatShortFileSize(LocalContext.current, size.bytes)

        FileSizeUiState.Unavailable -> stringResource(R.string.file_size_unavailable)
        FileSizeUiState.Loading, null -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEvent(FilePickerEvent.Select(entry)) }
            .semantics { contentDescription = entryDescription },
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (entry.isDirectory) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compactHeight) 10.dp else 12.dp,
                vertical = if (compactHeight) 8.dp else 14.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(
                    if (entry.isDirectory) R.drawable.ic_folder else R.drawable.ic_file
                ),
                modifier = Modifier.padding(end = 12.dp),
                contentDescription = null,
                tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    text = entry.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (entry.isFile && sizeText != null) {
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
