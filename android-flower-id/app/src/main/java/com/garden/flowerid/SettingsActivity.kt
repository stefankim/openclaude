package com.garden.flowerid

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.api_key_setup_title)
        }

        val apiKeyInput = findViewById<EditText>(R.id.apiKeyInput)
        val saveButton = findViewById<Button>(R.id.saveButton)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        apiKeyInput.setText(prefs.getString("api_key", ""))

        saveButton.setOnClickListener {
            val key = apiKeyInput.text.toString().trim()
            if (key.isBlank()) {
                Toast.makeText(this, R.string.enter_api_key, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString("api_key", key).apply()
            Toast.makeText(this, R.string.api_key_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
