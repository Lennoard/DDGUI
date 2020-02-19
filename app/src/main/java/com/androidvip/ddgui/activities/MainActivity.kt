package com.androidvip.ddgui.activities

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.androidvip.ddgui.R
import com.androidvip.ddgui.adapters.FileBrowserAdapter
import com.androidvip.ddgui.helpers.FileChangedListener
import com.androidvip.ddgui.runSafeOnUiThread
import com.androidvip.ddgui.toast
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class MainActivity : AppCompatActivity(), FileChangedListener {
    private var currentPath = "/"
    private lateinit var fileBrowserAdapter: FileBrowserAdapter
    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private val pickFileDialog: Dialog by lazy {
        Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_pick_files)
            setCancelable(true)
            setOnDismissListener {
                currentPath = "/"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        GlobalScope.launch {
            val isRooted = Shell.rootAccess()
            val activity = this@MainActivity

            runSafeOnUiThread {
                if (isRooted) {
                    fileBrowserAdapter = FileBrowserAdapter(arrayOf(), activity, inputPath, activity)
                    inputPick.setOnClickListener {
                        fileBrowserAdapter.container = inputPath
                        showPickFileDialog()
                    }

                    outputPick.setOnClickListener {
                        fileBrowserAdapter.container = outputPath
                        showPickFileDialog()
                    }
                } else {
                    AlertDialog.Builder(activity)
                        .setIcon(R.drawable.ic_error)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.root_not_found)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            moveTaskToBack(true)
                            finish()
                        }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_disclaimer -> {
                prefs.edit().putBoolean("disclaimer_pass", false).apply().also {
                    startActivity(Intent(this, DisclaimerActivity::class.java))
                    finish()
                }
                true
            }
            R.id.action_exit -> {
                moveTaskToBack(true)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onFileChanged(newFile: File, container: EditText) {
        if (newFile.isDirectory) {
            val newPath = newFile.absolutePath

            if (newPath.isNotEmpty()) {
                currentPath = newPath
                refreshList()
            } else {
                toast("Invalid path")
            }
        } else {
            container.setText(newFile.absolutePath)
            pickFileDialog.dismiss()
            currentPath = "/"
        }
    }

    private fun showPickFileDialog() {
        val swipeLayout: SwipeRefreshLayout? = pickFileDialog.findViewById(R.id.pickFilesSwipeLayout)
        swipeLayout?.apply {
            setColorSchemeResources(R.color.colorAccent)
            setProgressBackgroundColorSchemeResource(R.color.colorPrimary)
            setOnRefreshListener { refreshList() }
        }

        pickFileDialog.findViewById<RecyclerView>(R.id.pickFilesRecyclerView)?.apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(this.context, LinearLayout.VERTICAL))
            layoutManager = LinearLayoutManager(this.context)
            adapter = fileBrowserAdapter
        }

        pickFileDialog.show()
        refreshList()
    }

    private suspend fun getCurrentPathFiles() : Array<File>? = withContext(Dispatchers.IO) {
        runCatching {
            SuFile.open(currentPath).listFiles()
        }.getOrDefault(arrayOf<File>())
    }

    private fun refreshList() {
        val swipeLayout: SwipeRefreshLayout? = pickFileDialog.findViewById(R.id.pickFilesSwipeLayout)
        swipeLayout?.isRefreshing = true

        GlobalScope.launch {
            val files = getCurrentPathFiles()

            runSafeOnUiThread {
                swipeLayout?.isRefreshing = false
                files?.let {
                    fileBrowserAdapter.updateData(it)
                }
            }
        }
    }
}
