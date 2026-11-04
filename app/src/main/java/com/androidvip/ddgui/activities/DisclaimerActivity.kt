package com.androidvip.ddgui.activities

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.androidvip.ddgui.databinding.ActivityDisclaimerBinding

class DisclaimerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDisclaimerBinding
    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        binding = ActivityDisclaimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (prefs.getBoolean("disclaimer_pass", false)) {
            startMainActivity()
        }

        binding.disclaimerAccept.setOnCheckedChangeListener { _, isChecked ->
            binding.disclaimerNext.isEnabled = isChecked
        }

        binding.disclaimerNext.setOnClickListener {
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
