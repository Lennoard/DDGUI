package com.androidvip.ddgui.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.preference.PreferenceManager
import com.androidvip.ddgui.R
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {
    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        inFlagsChipGroup.forEach {
            val chip = it as Chip

            chip.setOnCheckedChangeListener { _, _ ->

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
}
