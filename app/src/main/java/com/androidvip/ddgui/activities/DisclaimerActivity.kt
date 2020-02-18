package com.androidvip.ddgui.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.androidvip.ddgui.R
import kotlinx.android.synthetic.main.activity_disclaimer.*

class DisclaimerActivity : AppCompatActivity() {
    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disclaimer)

        if (prefs.getBoolean("disclaimer_pass", false)) {
            startMainActivity()
        }

        disclaimerAccept.setOnCheckedChangeListener { _, isChecked ->
            disclaimerNext.isEnabled = isChecked
        }

        disclaimerNext.setOnClickListener {
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        prefs.edit {
            putBoolean("disclaimer_pass", true)
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
