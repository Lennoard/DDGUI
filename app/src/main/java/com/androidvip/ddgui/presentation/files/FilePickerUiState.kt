package com.androidvip.ddgui.presentation.files

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class FileEntryUiModel(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val isFile: Boolean
)

@Immutable
data class FilePickerUiState(
    val currentPath: String = "/",
    val forwardPath: String? = null,
    val directoryHistory: ImmutableList<String> = persistentListOf("/"),
    val entries: ImmutableList<FileEntryUiModel> = persistentListOf(),
    val directoryEntries: ImmutableMap<String, ImmutableList<FileEntryUiModel>> =
        persistentMapOf(),
    val fileSizes: ImmutableMap<String, FileSizeUiState> = persistentMapOf(),
    val isLoading: Boolean = false,
    val listingFailed: Boolean = false,
    val errorMessage: String? = null,
    val newEntryType: NewEntryType? = null,
    val newEntryName: String = ""
)

@Immutable
sealed interface FileSizeUiState {
    data object Loading : FileSizeUiState
    data class Available(val bytes: Long) : FileSizeUiState
    data object Unavailable : FileSizeUiState
}

enum class NewEntryType {
    File,
    Folder
}

enum class FilePickerMessage {
    ListingFailed,
    CreateFileFailed,
    CreateFolderFailed,
    UnexpectedFailure
}

@Immutable
sealed interface FilePickerEffect {
    data class FileSelected(val path: String) : FilePickerEffect
    data class ShowMessage(val message: FilePickerMessage) : FilePickerEffect
}

@Immutable
sealed interface FilePickerEvent {
    data object Open : FilePickerEvent
    data object Close : FilePickerEvent
    data object Refresh : FilePickerEvent
    data object NavigateToRoot : FilePickerEvent
    data object NavigateToHome : FilePickerEvent
    data object NavigateUp : FilePickerEvent
    data object NavigateForward : FilePickerEvent
    data class NavigateToPath(val path: String) : FilePickerEvent
    data class Select(val entry: FileEntryUiModel) : FilePickerEvent
    data class LoadFileSize(val path: String) : FilePickerEvent
    data object ShowCreateFile : FilePickerEvent
    data object ShowCreateFolder : FilePickerEvent
    data class NewEntryNameChanged(val name: String) : FilePickerEvent
    data object DismissCreateEntry : FilePickerEvent
    data class CreateFile(val name: String) : FilePickerEvent
    data class CreateFolder(val name: String) : FilePickerEvent
}
