package com.androidvip.ddgui.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IntDef
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.androidvip.ddgui.*
import com.androidvip.ddgui.helpers.PickFileDialog
import com.google.android.material.snackbar.Snackbar
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    @State private var currentState: Int = STATE_IDLE
    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR)

        GlobalScope.launch {
            val isRooted = Shell.rootAccess()
            val activity = this@MainActivity

            runSafeOnUiThread {
                if (isRooted) {
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
                    .setMessage(R.string.ppending_operations_warning)
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

                arrayOf(
                    conversionNoTruncate,
                    conversionNoError,
                    conversionSync,
                    conversionFsync
                ).forEach { it.isChecked = false }

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
                prefs.edit { putBoolean("disclaimer_pass", false) }
                startActivity(Intent(this, DisclaimerActivity::class.java))
                finish()
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

    private fun setUpClickListeners() {
        inputPick.setOnClickListener {
            PickFileDialog(this) {
                inputPath.setText(it.absolutePath)
            }
        }

        outputPick.setOnClickListener {
            PickFileDialog(this) {
                outputPath.setText(it.absolutePath)
            }
        }

        fab.setOnClickListener {
            if (checkInputFields()) {
                executeDd()
            }
        }
    }
    
    private fun getConversions() : String {
        return arrayOf(
            conversionNoTruncate,
            conversionNoError,
            conversionSync,
            conversionFsync
        ).filter {
            it.isChecked
        }.joinToString(",") {
            it.tag as String
        }
    }
    
    private fun executeDd() {
        currentState = STATE_EXECUTING

        val `if` = inputPath.text.toString().trim()
        val of = outputPath.text.toString().trim()
        val conv = getConversions()

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

        Shell.su("dd if=$`if` of=$of conv=$conv").to(callbackList).submit {
            resultHeadline.text = getString(R.string.finished)
            resultProgress.goAway()

            currentState = STATE_EXECUTED
        }

        /* TODO:
            This sadly needs to be executed in its own shell
            otherwise it'll be pushed to the current shell's
            internal queue, not executing until the previous
            jobs finish instead of running concurrently with them.
         */
        /*
            In case you're wondering, sending this signal to a dd
            process causes it to print its progress
            Shell.su("watch -n1 'kill -USR1 \$(pgrep ^dd)'").submit()
         */
    }
    
    private fun checkInputFields(): Boolean {
        return when {
            inputPath.text.toString().isEmpty() -> {
                inputPath.snackbar(R.string.error_missing_input, Snackbar.LENGTH_LONG)
                false
            }

            outputPath.text.toString().isEmpty() -> {
                outputPath.snackbar(R.string.error_missing_output, Snackbar.LENGTH_LONG)
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
