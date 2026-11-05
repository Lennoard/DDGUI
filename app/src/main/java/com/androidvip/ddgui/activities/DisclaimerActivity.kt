package com.androidvip.ddgui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.preference.PreferenceManager
import com.androidvip.ddgui.presentation.disclaimer.DisclaimerRoute
import com.androidvip.ddgui.presentation.disclaimer.DisclaimerViewModel
import com.androidvip.ddgui.presentation.theme.DDGuiTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class DisclaimerActivity : ComponentActivity() {
    private val disclaimerViewModel: DisclaimerViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(DisclaimerViewModel.DISCLAIMER_PASSED_KEY, false)
        ) {
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
