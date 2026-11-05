package com.androidvip.ddgui.presentation.files

import com.androidvip.ddgui.domain.files.RootFileEntry

fun RootFileEntry.toUiModel(): FileEntryUiModel = FileEntryUiModel(
    path = path,
    name = name,
    isDirectory = isDirectory,
    isFile = isFile
)
