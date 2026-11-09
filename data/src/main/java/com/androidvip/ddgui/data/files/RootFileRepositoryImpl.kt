package com.androidvip.ddgui.data.files

import com.androidvip.ddgui.domain.files.DirectoryListingResult
import com.androidvip.ddgui.domain.files.FileBrowserFailure
import com.androidvip.ddgui.domain.files.FileCreationResult
import com.androidvip.ddgui.domain.files.FileSizeResult
import com.androidvip.ddgui.domain.files.RootFileEntry
import com.androidvip.ddgui.domain.files.RootFileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

class RootFileRepositoryImpl internal constructor(
    private val source: RootFileDataSource,
    private val ioDispatcher: CoroutineDispatcher
) : RootFileRepository {
    override suspend fun list(path: String): DirectoryListingResult = withContext(ioDispatcher) {
        when (val result = source.list(path)) {
            is RootFileSourceResult.Success -> DirectoryListingResult.Success(
                result.value
                    .filter { it.exists() || it.isDirectory }
                    .map { it.toEntry() }
                    .sortedWith(
                        compareByDescending<RootFileEntry> { it.isDirectory }
                            .thenBy { it.name.lowercase() }
                    )
            )

            RootFileSourceResult.Failure -> {
                DirectoryListingResult.Failure(FileBrowserFailure.ACCESS_DENIED_OR_NOT_DIRECTORY)
            }
        }
    }

    override suspend fun fileSize(path: String): FileSizeResult = withContext(ioDispatcher) {
        when (val result = source.fileSize(path)) {
            is RootFileSourceResult.Success -> FileSizeResult.Success(result.value)
            RootFileSourceResult.Failure -> FileSizeResult.Failure
        }
    }

    override suspend fun createFile(
        parentPath: String,
        name: String
    ): FileCreationResult = withContext(ioDispatcher) {
        source.createFile(parentPath, name).toCreationResult()
    }

    override suspend fun createDirectory(
        parentPath: String,
        name: String
    ): FileCreationResult = withContext(ioDispatcher) {
        source.createDirectory(parentPath, name).toCreationResult()
    }

    private fun RootFileSourceResult<File>.toCreationResult(): FileCreationResult =
        when (this) {
            is RootFileSourceResult.Success -> FileCreationResult.Created(value.toEntry())
            RootFileSourceResult.Failure -> {
                FileCreationResult.Failure(FileBrowserFailure.CREATE_FAILED)
            }
        }

    private fun File.toEntry() = RootFileEntry(
        path = absolutePath,
        name = name,
        isDirectory = isDirectory,
        isFile = isFile
    )
}
