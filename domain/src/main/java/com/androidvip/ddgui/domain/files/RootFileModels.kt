package com.androidvip.ddgui.domain.files

data class RootFileEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val isFile: Boolean
)

enum class FileBrowserFailure {
    ACCESS_DENIED_OR_NOT_DIRECTORY,
    CREATE_FAILED
}

sealed interface DirectoryListingResult {
    data class Success(val entries: List<RootFileEntry>) : DirectoryListingResult
    data class Failure(val reason: FileBrowserFailure) : DirectoryListingResult
}

sealed interface FileCreationResult {
    data class Created(val entry: RootFileEntry) : FileCreationResult
    data class Failure(val reason: FileBrowserFailure) : FileCreationResult
}
