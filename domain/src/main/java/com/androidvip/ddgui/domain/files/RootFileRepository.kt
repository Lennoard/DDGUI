package com.androidvip.ddgui.domain.files

interface RootFileRepository {
    suspend fun list(path: String): DirectoryListingResult
    suspend fun createFile(parentPath: String, name: String): FileCreationResult
    suspend fun createDirectory(parentPath: String, name: String): FileCreationResult
}
