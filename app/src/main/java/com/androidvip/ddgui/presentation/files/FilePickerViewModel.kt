package com.androidvip.ddgui.presentation.files

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidvip.ddgui.domain.files.DirectoryListingResult
import com.androidvip.ddgui.domain.files.FileCreationResult
import com.androidvip.ddgui.domain.files.FileSizeResult
import com.androidvip.ddgui.domain.files.RootFileRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FilePickerViewModel(private val fileRepository: RootFileRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FilePickerUiState())
    val uiState = _uiState.asStateFlow()

    private val effectChannel = Channel<FilePickerEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private var isOpen = false
    private var requestId = 0L
    private var listingJob: Job? = null
    private val fileSizeJobs = mutableMapOf<String, Job>()

    fun onEvent(event: FilePickerEvent) {
        when (event) {
            FilePickerEvent.Open -> open()
            FilePickerEvent.Close -> close()
            FilePickerEvent.Refresh -> refresh()
            FilePickerEvent.NavigateToRoot -> navigate(
                path = "/",
                forwardPath = null,
                directoryHistory = persistentListOf("/")
            )
            FilePickerEvent.NavigateToHome -> navigateToHome()
            FilePickerEvent.NavigateUp -> navigateUp()
            FilePickerEvent.NavigateForward -> navigateForward()
            is FilePickerEvent.NavigateToPath -> navigateToPath(event.path)
            is FilePickerEvent.Select -> select(event.entry)
            is FilePickerEvent.LoadFileSize -> loadFileSize(event.path)
            FilePickerEvent.ShowCreateFile -> showCreateEntry(NewEntryType.File)
            FilePickerEvent.ShowCreateFolder -> showCreateEntry(NewEntryType.Folder)
            is FilePickerEvent.NewEntryNameChanged ->
                _uiState.update { it.copy(newEntryName = event.name) }

            FilePickerEvent.DismissCreateEntry -> dismissCreateEntry()
            is FilePickerEvent.CreateFile -> createFromDialog(event.name, isDirectory = false)
            is FilePickerEvent.CreateFolder -> createFromDialog(event.name, isDirectory = true)
        }
    }

    private fun open() {
        isOpen = true
        dismissCreateEntry()
        navigate(path = "/", forwardPath = null, directoryHistory = persistentListOf("/"))
    }

    private fun close() {
        isOpen = false
        requestId++
        listingJob?.cancel()
        listingJob = null
        fileSizeJobs.values.forEach(Job::cancel)
        fileSizeJobs.clear()
        _uiState.update {
            it.copy(
                isLoading = false,
                fileSizes = persistentMapOf(),
                newEntryType = null,
                newEntryName = ""
            )
        }
    }

    private fun refresh() {
        val state = _uiState.value
        load(path = state.currentPath, forwardPath = state.forwardPath)
    }

    private fun navigateToHome() {
        val path = Environment.getExternalStorageDirectory().absolutePath
        navigate(path, forwardPath = null, directoryHistory = persistentListOf(path))
    }

    private fun navigateUp() {
        val state = _uiState.value
        val parent = parentPath(state.currentPath)
        val history = if (state.directoryHistory.lastOrNull() == state.currentPath &&
            state.directoryHistory.size > 1
        ) {
            state.directoryHistory.toPersistentList().removingAt(state.directoryHistory.lastIndex)
        } else {
            persistentListOf(parent)
        }
        navigate(path = parent, forwardPath = state.currentPath, directoryHistory = history)
    }

    private fun navigateForward() {
        val state = _uiState.value
        val forwardPath = state.forwardPath ?: return
        navigate(
            path = forwardPath,
            forwardPath = null,
            directoryHistory = state.directoryHistory.toPersistentList().adding(forwardPath)
        )
    }

    private fun navigateToPath(path: String) {
        val history = _uiState.value.directoryHistory
        val index = history.indexOf(path)
        val destinationHistory = if (index >= 0) {
            history.take(index + 1).toPersistentList()
        } else {
            persistentListOf(path)
        }
        navigate(path, forwardPath = null, directoryHistory = destinationHistory)
    }

    private fun select(entry: FileEntryUiModel) {
        if (!entry.isDirectory && entry.isFile) {
            emit(FilePickerEffect.FileSelected(entry.path))
        } else {
            val state = _uiState.value
            val parentPath = parentPath(entry.path)
            val parentIndex = state.directoryHistory.indexOf(parentPath)
            val history = if (parentIndex >= 0) {
                state.directoryHistory.take(parentIndex + 1).toPersistentList()
            } else {
                persistentListOf(parentPath)
            }
            navigate(
                path = entry.path,
                forwardPath = state.forwardPath,
                directoryHistory = history.adding(entry.path)
            )
        }
    }

    private fun loadFileSize(path: String) {
        val state = _uiState.value
        if (state.fileSizes.containsKey(path) || fileSizeJobs.containsKey(path)) return

        _uiState.update {
            it.copy(
                fileSizes = it.fileSizes.toPersistentMap().putting(path, FileSizeUiState.Loading)
            )
        }
        fileSizeJobs[path] = viewModelScope.launch {
            try {
                val sizeState = when (val result = fileRepository.fileSize(path)) {
                    is FileSizeResult.Success -> FileSizeUiState.Available(result.bytes)
                    FileSizeResult.Failure -> FileSizeUiState.Unavailable
                }
                if (isOpen) {
                    _uiState.update {
                        it.copy(
                            fileSizes = it.fileSizes.toPersistentMap().putting(path, sizeState)
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                if (isOpen) {
                    _uiState.update {
                        it.copy(
                            fileSizes = it.fileSizes.toPersistentMap().putting(
                                path,
                                FileSizeUiState.Unavailable
                            )
                        )
                    }
                }
            } finally {
                if (fileSizeJobs[path] === coroutineContext[Job]) {
                    fileSizeJobs.remove(path)
                }
            }
        }
    }

    private fun showCreateEntry(type: NewEntryType) {
        _uiState.update { it.copy(newEntryType = type, newEntryName = "") }
    }

    private fun dismissCreateEntry() {
        _uiState.update { it.copy(newEntryType = null, newEntryName = "") }
    }

    private fun createFromDialog(name: String, isDirectory: Boolean) {
        if (name.isBlank()) return
        dismissCreateEntry()
        create(name, isDirectory)
    }

    private fun navigate(
        path: String,
        forwardPath: String?,
        directoryHistory: kotlinx.collections.immutable.ImmutableList<String>
    ) {
        _uiState.update {
            it.copy(
                currentPath = path,
                forwardPath = forwardPath,
                directoryHistory = directoryHistory,
                entries = it.directoryEntries[path] ?: persistentListOf(),
                listingFailed = false,
                errorMessage = null
            )
        }
        load(path, forwardPath)
    }

    private fun load(path: String, forwardPath: String?) {
        if (!isOpen) return
        val thisRequest = ++requestId
        listingJob?.cancel()
        _uiState.update { it.copy(isLoading = true, listingFailed = false) }
        listingJob = viewModelScope.launch {
            try {
                when (val result = fileRepository.list(path)) {
                    is DirectoryListingResult.Success -> {
                        if (isCurrent(thisRequest, path)) {
                            val entries = result.entries.map { entry -> entry.toUiModel() }
                                .toPersistentList()
                            _uiState.update {
                                it.copy(
                                    currentPath = path,
                                    forwardPath = forwardPath,
                                    entries = entries,
                                    directoryEntries = it.directoryEntries.toPersistentMap().putting(path, entries),
                                    isLoading = false,
                                    listingFailed = false,
                                    errorMessage = null
                                )
                            }
                        }
                    }

                    is DirectoryListingResult.Failure -> {
                        if (isCurrent(thisRequest, path)) {
                            _uiState.update {
                                it.copy(
                                    currentPath = path,
                                    forwardPath = forwardPath,
                                    entries = persistentListOf(),
                                    isLoading = false,
                                    listingFailed = true,
                                    errorMessage = null
                                )
                            }
                            emit(FilePickerEffect.ShowMessage(FilePickerMessage.ListingFailed))
                        }
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (isCurrent(thisRequest, path)) {
                    _uiState.update {
                        it.copy(
                            currentPath = path,
                            forwardPath = forwardPath,
                            entries = persistentListOf(),
                            isLoading = false,
                            listingFailed = true,
                            errorMessage = exception.message
                        )
                    }
                    emit(FilePickerEffect.ShowMessage(FilePickerMessage.UnexpectedFailure))
                }
            }
        }
    }

    private fun create(name: String, isDirectory: Boolean) {
        val state = _uiState.value
        val thisRequest = requestId
        viewModelScope.launch {
            try {
                val result = if (isDirectory) {
                    fileRepository.createDirectory(state.currentPath, name)
                } else {
                    fileRepository.createFile(state.currentPath, name)
                }
                if (!isCurrent(thisRequest, state.currentPath)) return@launch
                when (result) {
                    is FileCreationResult.Created -> {
                        if (isDirectory) {
                            navigate(
                                path = result.entry.path,
                                forwardPath = null,
                                directoryHistory = state.directoryHistory
                                    .toPersistentList()
                                    .adding(result.entry.path)
                            )
                        } else {
                            emit(FilePickerEffect.FileSelected(result.entry.path))
                        }
                    }

                    is FileCreationResult.Failure -> emit(
                        FilePickerEffect.ShowMessage(
                            if (isDirectory) {
                                FilePickerMessage.CreateFolderFailed
                            } else {
                                FilePickerMessage.CreateFileFailed
                            }
                        )
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                if (isCurrent(thisRequest, state.currentPath)) {
                    emit(FilePickerEffect.ShowMessage(FilePickerMessage.UnexpectedFailure))
                }
            }
        }
    }

    private fun isCurrent(expectedRequestId: Long, expectedPath: String): Boolean =
        isOpen && expectedRequestId == requestId &&
            expectedPath == _uiState.value.currentPath

    private fun parentPath(path: String): String {
        val normalized = path.trimEnd('/')
        if (normalized.isEmpty()) return "/"

        val separator = normalized.lastIndexOf('/')
        return if (separator <= 0) "/" else normalized.substring(0, separator)
    }

    private fun emit(effect: FilePickerEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }
}
