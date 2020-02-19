package com.androidvip.ddgui.helpers

import android.widget.EditText
import java.io.File

interface FileChangedListener {
    fun onFileChanged(newFile: File, container: EditText)

}