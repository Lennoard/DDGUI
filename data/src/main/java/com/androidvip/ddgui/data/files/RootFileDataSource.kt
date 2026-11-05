package com.androidvip.ddgui.data.files

import com.topjohnwu.superuser.io.SuFile
import java.io.File
import java.io.IOException

internal sealed interface RootFileSourceResult<out T> {
    data class Success<T>(val value: T) : RootFileSourceResult<T>
    data object Failure : RootFileSourceResult<Nothing>
}

internal interface RootFileDataSource {
    fun list(path: String): RootFileSourceResult<List<File>>
    fun createFile(parentPath: String, name: String): RootFileSourceResult<File>
    fun createDirectory(parentPath: String, name: String): RootFileSourceResult<File>
}

internal class SuFileDataSource : RootFileDataSource {
    override fun list(path: String): RootFileSourceResult<List<File>> {
        return try {
            val entries = SuFile.open(path).listFiles()
                ?: return RootFileSourceResult.Failure
            RootFileSourceResult.Success(entries.toList())
        } catch (_: SecurityException) {
            RootFileSourceResult.Failure
        }
    }

    override fun createFile(
        parentPath: String,
        name: String
    ): RootFileSourceResult<File> = try {
        val file = child(parentPath, name)
        if (file.createNewFile()) {
            RootFileSourceResult.Success(file)
        } else {
            RootFileSourceResult.Failure
        }
    } catch (_: IOException) {
        RootFileSourceResult.Failure
    } catch (_: SecurityException) {
        RootFileSourceResult.Failure
    }

    override fun createDirectory(
        parentPath: String,
        name: String
    ): RootFileSourceResult<File> = try {
        val directory = child(parentPath, name)
        if (directory.mkdir()){
            RootFileSourceResult.Success(directory)
        } else {
            RootFileSourceResult.Failure
        }
    } catch (_: SecurityException) {
        RootFileSourceResult.Failure
    }

    private fun child(parentPath: String, name: String): SuFile =
        SuFile(File(parentPath, name).path)
}
