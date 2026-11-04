package com.androidvip.ddgui.activities

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.IntDef
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.androidvip.ddgui.*
import com.androidvip.ddgui.databinding.ActivityMainBinding
import com.androidvip.ddgui.dd.DdCommandBuilder
import com.androidvip.ddgui.dd.DdConversion
import com.androidvip.ddgui.dd.DdRequest
import com.androidvip.ddgui.helpers.PickFileDialog
import com.google.android.material.snackbar.Snackbar
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell

class MainActivity : AppCompatActivity() {
    @State private var currentState: Int = STATE_IDLE
    private lateinit var binding: ActivityMainBinding
    private val content get() = binding.contentMain
    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentState) {
                    STATE_EXECUTING -> {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(android.R.string.dialog_alert_title)
                            .setMessage(R.string.ppending_operations_warning)
                            .setPositiveButton(android.R.string.yes) { _, _ -> finish() }
                            .setNegativeButton(android.R.string.no, null)
                            .show()
                    }

                    STATE_EXECUTED -> resetExecutedState()
                    else -> {
                        isEnabled = false
                        try {
                            onBackPressedDispatcher.onBackPressed()
                        } finally {
                            isEnabled = true
                        }
                    }
                }
            }
        })

        Shell.getShell { shell ->
            if (isFinishing || isDestroyed) return@getShell

            if (shell.isRoot) {
                setUpClickListeners()
            } else {
                AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_error)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.root_not_found)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        moveTaskToBack(true)
                        finish()
                    }
                    .show()
            }
        }
    }

    private fun resetExecutedState() {
        content.resultLayout.goAway()
        content.optionsLayout.show()
        binding.fab.show()
        content.resultProgress.hide()
        content.resultHeadline.text = getString(R.string.executing)

        arrayOf(
            content.conversionNoTruncate,
            content.conversionNoError,
            content.conversionSync,
            content.conversionFsync,
            content.conversionSwab
        ).forEach { it.isChecked = false }

        currentState = STATE_IDLE
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
        content.inputPick.setOnClickListener {
            PickFileDialog(this) {
                content.inputPath.setText(it.absolutePath)
            }
        }

        content.outputPick.setOnClickListener {
            PickFileDialog(this) {
                content.outputPath.setText(it.absolutePath)
            }
        }

        binding.fab.setOnClickListener {
            if (checkInputFields()) {
                executeDd()
            }
        }
    }
    
    private fun getConversions(): List<DdConversion> {
        return listOf(
            content.conversionNoTruncate to DdConversion.NO_TRUNCATE,
            content.conversionNoError to DdConversion.NO_ERROR,
            content.conversionSync to DdConversion.SYNC,
            content.conversionFsync to DdConversion.FSYNC,
            content.conversionSwab to DdConversion.SWAB
        ).filter { (view, _) ->
            view.isChecked
        }.map { (_, conversion) ->
            conversion
        }
    }
    
    private fun executeDd() {
        currentState = STATE_EXECUTING

        val inputPath = content.inputPath.text.toString()
        val outputPath = content.outputPath.text.toString()
        val command = DdCommandBuilder.build(
            DdRequest(inputPath, outputPath, getConversions())
        )

        content.resultLayout.show()
        content.optionsLayout.goAway()
        binding.fab.hide()
        content.resultProgress.show()
        content.resultHeadline.text = getString(R.string.executing)

        val callbackList: List<String> = object : CallbackList<String>() {
            @MainThread
            override fun onAddElement(s: String) {
                content.resultOutput.text = "${content.resultOutput.text}\n$s"
            }
        }

        Shell.cmd(command).to(callbackList, callbackList).submit { result ->
            content.resultHeadline.text = getString(
                if (result.isSuccess) R.string.finished else R.string.failed
            )
            content.resultProgress.goAway()

            currentState = STATE_EXECUTED
        }

        /* TODO:
            This sadly needs to be executed in its own shell
            otherwise it'll be pushed to the current shell's
            internal queue, not executing until the previous
            jobs finish instead of running concurrently with them.
         */
    }
    
    private fun checkInputFields(): Boolean {
        return when {
            content.inputPath.text.toString().isEmpty() -> {
                content.inputPath.snackbar(R.string.error_missing_input, Snackbar.LENGTH_LONG)
                false
            }

            content.outputPath.text.toString().isEmpty() -> {
                content.outputPath.snackbar(R.string.error_missing_output, Snackbar.LENGTH_LONG)
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
