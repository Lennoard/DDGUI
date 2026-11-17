package com.androidvip.ddgui.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.androidvip.ddgui.presentation.disclaimer.DisclaimerRoute
import com.androidvip.ddgui.presentation.disclaimer.DisclaimerViewModel
import com.androidvip.ddgui.presentation.theme.DDGuiTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class DisclaimerActivity : ComponentActivity() {
    private val preferences: SharedPreferences by inject()
    private val disclaimerViewModel: DisclaimerViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (preferences.getBoolean(DisclaimerViewModel.DISCLAIMER_PASSED_KEY, false)) {
            startMainActivity()
            return
        }

        setContent {
            DDGuiTheme {
                DisclaimerRoute(
                    viewModel = disclaimerViewModel,
                    onContinue = ::startMainActivity
                )
            }
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
