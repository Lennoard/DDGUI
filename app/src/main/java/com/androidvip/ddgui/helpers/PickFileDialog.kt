package com.androidvip.ddgui.helpers

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.os.Environment
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.androidvip.ddgui.R
import com.androidvip.ddgui.adapters.FileBrowserAdapter
import com.androidvip.ddgui.runSafeOnUiThread
import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference

class PickFileDialog(activity: Activity, val fileSelectedCallback: (File) -> Unit) : OnFileSelectedListener {
    private var fileBrowserAdapter: FileBrowserAdapter = FileBrowserAdapter(arrayOf(), activity, this)
    private var dialog: Dialog
    private val weakActivity : WeakReference<Activity> = WeakReference(activity)
    var currentFile = File("/")
    var forwardFile: File? = null

    init {
        dialog = Dialog(weakActivity.get()!!).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_pick_files)
            setCancelable(true)

            this.findViewById<ImageView>(R.id.pickFilesRootFolder).setOnClickListener {
                currentFile = SuFile.open("/")
                forwardFile = null
                refreshList()
                this.findViewById<ImageView>(R.id.pickFilesForward)
                    ?.setColorFilter(Color.argb(128, 0, 0, 0))
            }

            this.findViewById<ImageView>(R.id.pickFilesHomeFolder).setOnClickListener {
                currentFile = SuFile.open(Environment.getExternalStorageDirectory().toString())
                forwardFile = null
                refreshList()
                this.findViewById<ImageView>(R.id.pickFilesForward)
                    ?.setColorFilter(Color.argb(128, 0, 0, 0))
            }

            this.findViewById<ImageView>(R.id.pickFilesLevelUp).setOnClickListener {
                runCatching {
                    forwardFile = currentFile
                    currentFile = currentFile.parentFile ?: currentFile
                    refreshList()

                    this.findViewById<ImageView>(R.id.pickFilesForward)
                        ?.setColorFilter(Color.argb(255, 255, 255, 255))
                }
            }

            this.findViewById<ImageView>(R.id.pickFilesForward).setOnClickListener {
                runCatching {
                    if (forwardFile != null) {
                        currentFile = forwardFile!!
                        forwardFile = null
                        refreshList()
                        this.findViewById<ImageView>(R.id.pickFilesForward)
                            ?.setColorFilter(Color.argb(128, 0, 0, 0))
                    }
                }
            }

            this.findViewById<SwipeRefreshLayout>(R.id.pickFilesSwipeLayout)?.apply {
                setColorSchemeResources(R.color.colorAccent)
                setProgressBackgroundColorSchemeResource(R.color.colorPrimary)
                setOnRefreshListener { refreshList() }
            }

            this.findViewById<RecyclerView>(R.id.pickFilesRecyclerView)?.apply {
                setHasFixedSize(true)
                addItemDecoration(DividerItemDecoration(this.context, LinearLayout.VERTICAL))
                layoutManager = LinearLayoutManager(this.context)
                adapter = fileBrowserAdapter
            }
        }

        show()
    }

    private fun show() {
        refreshList()
        weakActivity.get()?.let {
            if (!it.isFinishing) {
                dialog.show()
            }
        }
    }

    private fun refreshList() {
        val swipeLayout: SwipeRefreshLayout? = dialog.findViewById(R.id.pickFilesSwipeLayout)
        swipeLayout?.isRefreshing = true

        GlobalScope.launch {
            val files = getCurrentPathFiles()

            weakActivity.get().runSafeOnUiThread {
                swipeLayout?.isRefreshing = false
                files?.let {
                    dialog.findViewById<TextView>(R.id.pickFilesCurrentPath)?.text = currentFile.absolutePath
                    fileBrowserAdapter.updateData(it)
                }
            }
        }
    }

    private suspend fun getCurrentPathFiles() : Array<File>? = withContext(Dispatchers.IO) {
        runCatching {
            SuFile.open(currentFile.absolutePath).listFiles()
        }.getOrDefault(arrayOf())
    }

    override fun onFileSelected(newFile: File) {
        if (!newFile.isDirectory && (newFile.isFile || newFile.exists())) {
            fileSelectedCallback(newFile)
            dialog.dismiss()
        } else {
            currentFile = newFile
            refreshList()

            if (forwardFile != null) {
                dialog.findViewById<ImageView>(R.id.pickFilesForward)
                    ?.setColorFilter(Color.argb(255, 255, 255, 255))
            } else {
                dialog.findViewById<ImageView>(R.id.pickFilesForward)
                    ?.setColorFilter(Color.argb(128, 0, 0, 0))
            }
        }
    }
}