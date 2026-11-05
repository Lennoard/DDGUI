package com.androidvip.ddgui.presentation.files

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidvip.ddgui.domain.files.DirectoryListingResult
import com.androidvip.ddgui.domain.files.FileCreationResult
import com.androidvip.ddgui.domain.files.RootFileRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class FilePickerViewModel(private val fileRepository: RootFileRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FilePickerUiState())
    val uiState = _uiState.asStateFlow()

    private val effectChannel = Channel<FilePickerEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private var isOpen = false
    private var requestId = 0L
    private var listingJob: Job? = null

    fun onEvent(event: FilePickerEvent) {
        when (event) {
            FilePickerEvent.Open -> open()
            FilePickerEvent.Close -> close()
            FilePickerEvent.Refresh -> refresh()
            FilePickerEvent.NavigateToRoot -> navigate(path = "/", forwardPath = null)
            FilePickerEvent.NavigateToHome -> navigateToHome()
            FilePickerEvent.NavigateUp -> navigateUp()
            FilePickerEvent.NavigateForward -> navigateForward()
            is FilePickerEvent.Select -> select(event.entry)
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
        navigate(path = "/", forwardPath = null)
    }

    private fun close() {
        isOpen = false
        requestId++
        listingJob?.cancel()
        listingJob = null
        _uiState.update {
            it.copy(
                isLoading = false,
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
        navigate(path = Environment.getExternalStorageDirectory().absolutePath, forwardPath = null)
    }

    private fun navigateUp() {
        val state = _uiState.value
        val parent = File(state.currentPath).parent ?: state.currentPath
        navigate(path = parent, forwardPath = state.currentPath)
    }

    private fun navigateForward() {
        val forwardPath = _uiState.value.forwardPath ?: return
        navigate(path = forwardPath, forwardPath = null)
    }

    private fun select(entry: FileEntryUiModel) {
        if (!entry.isDirectory && entry.isFile) {
            emit(FilePickerEffect.FileSelected(entry.path))
        } else {
            navigate(path = entry.path, forwardPath = _uiState.value.forwardPath)
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

    private fun navigate(path: String, forwardPath: String?) {
        _uiState.update {
            it.copy(
                currentPath = path,
                forwardPath = forwardPath,
                entries = persistentListOf(),
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
                            _uiState.update {
                                it.copy(
                                    currentPath = path,
                                    forwardPath = forwardPath,
                                    entries = result.entries.map { entry -> entry.toUiModel() }
                                        .toPersistentList(),
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
                            navigate(result.entry.path, forwardPath = null)
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

    private fun emit(effect: FilePickerEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }
}
