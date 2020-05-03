package com.androidvip.ddgui

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

suspend inline fun Activity?.runSafeOnUiThread(crossinline uiBlock: () -> Unit) {
    this?.let {
        if (!it.isFinishing) {
            withContext(Dispatchers.Main) {
                runCatching(uiBlock)
            }
        }
    }
}
