package com.androidvip.ddgui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.androidvip.ddgui.presentation.disclaimer.DisclaimerViewModel
import com.androidvip.ddgui.presentation.files.FilePickerViewModel
import com.androidvip.ddgui.presentation.main.MainRoute
import com.androidvip.ddgui.presentation.main.MainViewModel
import com.androidvip.ddgui.presentation.theme.DDGuiTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : androidx.activity.ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModel()
    private val filePickerViewModel: FilePickerViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DDGuiTheme {
                MainRoute(
                    mainViewModel = mainViewModel,
                    filePickerViewModel = filePickerViewModel,
                    onExit = {
                        moveTaskToBack(true)
                        finish()
                    },
                    onDisclaimer = {
                        PreferenceManager.getDefaultSharedPreferences(this)
                            .edit {
                                putBoolean(DisclaimerViewModel.DISCLAIMER_PASSED_KEY, false)
                            }
                        startActivity(Intent(this, DisclaimerActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
