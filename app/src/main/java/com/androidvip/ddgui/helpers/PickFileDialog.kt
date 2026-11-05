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
import com.androidvip.ddgui.domain.files.DirectoryListingResult
import com.androidvip.ddgui.domain.files.FileCreationResult
import com.androidvip.ddgui.domain.files.RootFileEntry
import com.androidvip.ddgui.domain.files.RootFileRepository
import com.androidvip.ddgui.runSafeOnUiThread
import com.androidvip.ddgui.toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

class PickFileDialog(
    activity: Activity,
    private val fileRepository: RootFileRepository,
    val fileSelectedCallback: (String) -> Unit
) : OnFileSelectedListener {
    private var fileBrowserAdapter: FileBrowserAdapter = FileBrowserAdapter(emptyList(), activity, this)
    private var dialog: Dialog
    private val weakActivity : WeakReference<Activity> = WeakReference(activity)
    var currentPath = "/"
    var forwardPath: String? = null

    init {
        dialog = Dialog(weakActivity.get()!!).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_pick_files)
            setCancelable(true)

            this.findViewById<ImageView>(R.id.pickFilesRootFolder).setOnClickListener {
                currentPath = "/"
                forwardPath = null
                refreshList()
                this.findViewById<ImageView>(R.id.pickFilesForward)
                    ?.setColorFilter(Color.argb(128, 0, 0, 0))
            }

            this.findViewById<ImageView>(R.id.pickFilesHomeFolder).setOnClickListener {
                currentPath = Environment.getExternalStorageDirectory().toString()
                forwardPath = null
                refreshList()
                this.findViewById<ImageView>(R.id.pickFilesForward)
                    ?.setColorFilter(Color.argb(128, 0, 0, 0))
            }

            this.findViewById<ImageView>(R.id.pickFilesLevelUp).setOnClickListener {
                runCatching {
                    forwardPath = currentPath
                    currentPath = File(currentPath).parent ?: currentPath
                    refreshList()

                    this.findViewById<ImageView>(R.id.pickFilesForward)
                        ?.setColorFilter(Color.argb(255, 255, 255, 255))
                }
            }

            this.findViewById<ImageView>(R.id.pickFilesForward).setOnClickListener {
                runCatching {
                    if (forwardPath != null) {
                        currentPath = forwardPath!!
                        forwardPath = null
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
            val result = fileRepository.list(currentPath)

            weakActivity.get().runSafeOnUiThread {
                swipeLayout?.isRefreshing = false
                when (result) {
                    is DirectoryListingResult.Success -> {
                        dialog.findViewById<TextView>(R.id.pickFilesCurrentPath)?.text = currentPath
                        fileBrowserAdapter.updateData(result.entries)
                    }
                    is DirectoryListingResult.Failure -> {
                        weakActivity.get()?.toast(R.string.failed)
                    }
                }

                dialog.findViewById<ImageView>(R.id.pickFilesNewFolder).setOnClickListener {
                    EditDialog(weakActivity.get()!!) {
                        GlobalScope.launch {
                            when (fileRepository.createDirectory(currentPath, it)) {
                                is FileCreationResult.Created -> {
                                    currentPath = File(currentPath, it).path
                                    refreshList()
                                }
                                is FileCreationResult.Failure -> {
                                    weakActivity.get()?.toast(R.string.create_folder_fail)
                                }
                            }
                        }

                    }.buildApplying {
                        setTitle(R.string.new_folder)
                        show()
                    }
                }

                dialog.findViewById<ImageView>(R.id.pickFilesNewFile).setOnClickListener {
                    EditDialog(weakActivity.get()!!) {
                        GlobalScope.launch {
                            when (val result = fileRepository.createFile(currentPath, it)) {
                                is FileCreationResult.Created -> {
                                    fileSelectedCallback(result.entry.path)
                                    dialog.dismiss()
                                }
                                is FileCreationResult.Failure -> {
                                    weakActivity.get()?.toast(R.string.create_file_fail)
                                }
                            }
                        }

                    }.buildApplying {
                        setTitle(R.string.new_file)
                        show()
                    }
                }
            }
        }
    }

    override fun onFileSelected(newFile: RootFileEntry) {
        if (!newFile.isDirectory && newFile.isFile) {
            fileSelectedCallback(newFile.path)
            dialog.dismiss()
        } else {
            currentPath = newFile.path
            refreshList()

            if (forwardPath != null) {
                dialog.findViewById<ImageView>(R.id.pickFilesForward)
                    ?.setColorFilter(Color.argb(255, 255, 255, 255))
            } else {
                dialog.findViewById<ImageView>(R.id.pickFilesForward)
                    ?.setColorFilter(Color.argb(128, 0, 0, 0))
            }
        }
    }
}