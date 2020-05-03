package com.androidvip.ddgui.activities

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.androidvip.ddgui.*
import com.androidvip.ddgui.adapters.FileBrowserAdapter
import com.androidvip.ddgui.helpers.FileChangedListener
import com.google.android.material.snackbar.Snackbar
import com.topjohnwu.superuser.CallbackList
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
    @State private var currentState: Int = STATE_IDLE
    private var currentFilePath = File("/")
    private lateinit var fileBrowserAdapter: FileBrowserAdapter
    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private val pickFileDialog: Dialog by lazy {
        Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_pick_files)
            setCancelable(true)

            this.findViewById<ImageView>(R.id.pickFilesHome).setOnClickListener {
                currentFilePath = File("/")
                refreshList()
            }

            this.findViewById<ImageView>(R.id.pickFilesLevelUp).setOnClickListener {
                runCatching {
                    currentFilePath = currentFilePath.parentFile
                    refreshList()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR)

        fun setUpClickListeners() {
            inputPick.setOnClickListener {
                fileBrowserAdapter.container = inputPath
                showPickFileDialog()
            }

            outputPick.setOnClickListener {
                fileBrowserAdapter.container = outputPath
                showPickFileDialog()
            }

            fab.setOnClickListener {
                if (checkInputFields()) {
                    currentState = STATE_EXECUTING

                    val `if` = inputPath.text.toString().trim()
                    val `out` = outputPath.text.toString().trim()

                    resultLayout.show()
                    optionsLayout.goAway()
                    fab.hide()
                    resultProgress.show()
                    resultHeadline.text = getString(R.string.executing)

                    val callbackList: List<String> = object : CallbackList<String>() {
                        @MainThread
                        override fun onAddElement(s: String) {
                            resultOutput.text = "${resultOutput.text}\n$s"
                        }
                    }

                    Shell.su("dd if=$`if` of=$`out`").to(callbackList).submit {
                        resultHeadline.text = getString(R.string.finished)
                        resultProgress.goAway()

                        currentState = STATE_EXECUTED
                    }
                    Shell.su("watch -n1 'kill -USR1 \$(pgrep ^dd)'").submit()
                }
            }
        }

        GlobalScope.launch {
            val isRooted = Shell.rootAccess()
            val activity = this@MainActivity

            runSafeOnUiThread {
                if (isRooted) {
                    fileBrowserAdapter = FileBrowserAdapter(arrayOf(), activity, inputPath, activity)
                    setUpClickListeners()
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

    override fun onBackPressed() {
        when (currentState) {
            STATE_EXECUTING -> {
                AlertDialog.Builder(this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage("There are operations pending, do you really want to quit now?")
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        super.onBackPressed()
                    }
                    .setNegativeButton(android.R.string.no) { _, _ ->  }
                    .show()
            }

            STATE_EXECUTED -> {
                resultLayout.goAway()
                optionsLayout.show()
                fab.show()
                resultProgress.hide()
                resultHeadline.text = getString(R.string.executing)

                currentState = STATE_IDLE
            }

            else -> super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        runCatching {
            Shell.getCachedShell()?.close()
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
            if (newFile.absolutePath.isNotEmpty()) {
                currentFilePath = newFile
                refreshList()
            } else {
                toast("Invalid path")
            }
        } else {
            container.setText(newFile.absolutePath)
            pickFileDialog.dismiss()
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
        if (currentFilePath.absolutePath == "/") {
            refreshList()
        }
    }

    private suspend fun getCurrentPathFiles() : Array<File>? = withContext(Dispatchers.IO) {
        runCatching {
            SuFile.open(currentFilePath.absolutePath).listFiles()
        }.getOrDefault(arrayOf())
    }

    private fun refreshList() {
        val swipeLayout: SwipeRefreshLayout? = pickFileDialog.findViewById(R.id.pickFilesSwipeLayout)
        swipeLayout?.isRefreshing = true

        GlobalScope.launch {
            val files = getCurrentPathFiles()

            runSafeOnUiThread {
                swipeLayout?.isRefreshing = false
                files?.let {
                    pickFileDialog.findViewById<TextView>(R.id.pickFilesCurrentPath)?.text = currentFilePath.absolutePath
                    fileBrowserAdapter.updateData(it)
                }
            }
        }
    }

    private fun checkInputFields(): Boolean {
        return when {
            inputPath.text.toString().isEmpty() -> {
                Snackbar.make(inputPath, R.string.error_missing_input, Snackbar.LENGTH_LONG).show()
                false
            }

            outputPath.text.toString().isEmpty() -> {
                Snackbar.make(outputPath, R.string.error_missing_output, Snackbar.LENGTH_LONG).show()
                false
            }

            else -> true
        }
    }

    companion object {
        @IntDef(STATE_IDLE, STATE_EXECUTING, STATE_EXECUTED)
        @Retention(AnnotationRetention.SOURCE)
        annotation class State

        const val STATE_IDLE = 0
        const val STATE_EXECUTING = 1
        const val STATE_EXECUTED = 2
    }
}
