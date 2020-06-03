package com.androidvip.ddgui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias SnackbarDuration = BaseTransientBottomBar.Duration

fun View.goAway() { this.visibility = View.GONE }
fun View.hide() { this.visibility = View.INVISIBLE }
fun View.show(): View {
    this.visibility = View.VISIBLE
    return this
}

fun Context?.toast(messageRes: Int, length: Int = Toast.LENGTH_SHORT) {
    if (this == null) return
    toast(getString(messageRes), length)
}

fun Context?.toast(message: String?, length: Int = Toast.LENGTH_SHORT) {
    if (message == null || this == null) return
    val ctx = this

    // Just in case :P
    GlobalScope.launch(Dispatchers.Main) {
        Toast.makeText(ctx, message, length).show()
    }
}

fun <E> MutableCollection<E>.addIf(condition: Boolean, e: E) {
    if (condition) this.add(e)
}

fun View.snackbar(@StringRes stringRes: Int, @SnackbarDuration duration: Int = Snackbar.LENGTH_SHORT) {
    snackbar(context.getString(stringRes), duration)
}

fun View.snackbar(msg: String, @SnackbarDuration duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, msg, duration).apply {
        view.setBackgroundColor(Color.parseColor("#cfd8dc"))
        setTextColor(Color.parseColor("#DE000000"))
        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).apply {
            typeface = Typeface.MONOSPACE
        }
        show()
    }
}

suspend inline fun Activity?.runSafeOnUiThread(crossinline uiBlock: () -> Unit) {
    this?.let {
        if (!it.isFinishing) {
            withContext(Dispatchers.Main) {
                runCatching(uiBlock)
            }
        }
    }
}
