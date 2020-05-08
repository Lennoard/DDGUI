package com.androidvip.ddgui.helpers

import android.app.Activity
import android.content.DialogInterface
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.androidvip.ddgui.R


class EditDialog(private val activity: Activity, val onConfirm: (String) -> Unit) {
    var title = "Edit"
    var message: String? = null
    var inputText: String? = ""
    var onCancelListener = DialogInterface.OnClickListener { _, _ -> }

    fun setTitle(@StringRes title: Int) {
        this.title = activity.getString(title)
    }

    fun show() {
        if (activity.isFinishing) return

        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_input, null)

        val editText = dialogView?.findViewById<EditText>(R.id.dialogTextInput)
        editText?.setText(if (inputText == null) "" else inputText)

        val dialog = AlertDialog.Builder(activity).apply {
            setTitle(title)
            setView(dialogView)
            this@EditDialog.message?.let { setMessage(it) }

            setNegativeButton(android.R.string.cancel, onCancelListener)
            setPositiveButton(android.R.string.ok) { _, _ -> onConfirm(editText?.text.toString()) }
        }


        dialog.show()
    }

    fun buildApplying(block: EditDialog.() -> Unit): EditDialog {
        this.block()
        return this
    }

}