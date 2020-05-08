package com.androidvip.ddgui.helpers

import com.topjohnwu.superuser.io.SuFile
import java.io.File

interface OnFileSelectedListener {
    fun onFileSelected(newFile: File)
}