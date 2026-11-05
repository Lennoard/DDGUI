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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.androidvip.ddgui.*
import com.androidvip.ddgui.databinding.ActivityMainBinding
import com.androidvip.ddgui.domain.dd.DdConversion
import com.androidvip.ddgui.domain.dd.DdExecutionResult
import com.androidvip.ddgui.domain.dd.DdRepository
import com.androidvip.ddgui.domain.dd.DdRequest
import com.androidvip.ddgui.domain.dd.RootAccess
import com.androidvip.ddgui.helpers.PickFileDialog
import com.androidvip.ddgui.domain.files.RootFileRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private val ddRepository: DdRepository by inject()
    private val rootFileRepository: RootFileRepository by inject()
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

        lifecycleScope.launch {
            when (ddRepository.checkRootAccess()) {
                RootAccess.Granted -> setUpClickListeners()
                RootAccess.Denied -> AlertDialog.Builder(this@MainActivity)
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
            PickFileDialog(this, rootFileRepository) {
                content.inputPath.setText(it)
            }
        }

        content.outputPick.setOnClickListener {
            PickFileDialog(this, rootFileRepository) {
                content.outputPath.setText(it)
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

        val inputPath = content.inputPath.text.toString().trim()
        val outputPath = content.outputPath.text.toString().trim()
        content.resultLayout.show()
        content.optionsLayout.goAway()
        binding.fab.hide()
        content.resultProgress.show()
        content.resultHeadline.text = getString(R.string.executing)
        content.resultOutput.text = ""

        lifecycleScope.launch {
            val result = ddRepository.execute(
                DdRequest(inputPath, outputPath, getConversions())
            ) { line ->
                content.resultOutput.append("\n$line")
            }

            when (result) {
                is DdExecutionResult.Completed -> {
                    content.resultHeadline.text = getString(
                        if (result.exitCode == 0) R.string.finished else R.string.failed
                    )
                    content.resultProgress.goAway()
                    currentState = STATE_EXECUTED
                }

                DdExecutionResult.RootDenied -> {
                    content.resultHeadline.text = getString(R.string.failed)
                    content.resultOutput.text = getString(R.string.root_not_found)
                    content.resultProgress.goAway()
                    currentState = STATE_EXECUTED
                }

                is DdExecutionResult.InvalidRequest -> {
                    content.resultHeadline.text = getString(R.string.failed)
                    content.resultOutput.text = result.message
                    content.resultProgress.goAway()
                    currentState = STATE_EXECUTED
                }
            }
        }
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
