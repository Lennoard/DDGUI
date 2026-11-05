package com.androidvip.ddgui.helpers

import com.androidvip.ddgui.domain.files.RootFileEntry

interface OnFileSelectedListener {
    fun onFileSelected(newFile: RootFileEntry)
}