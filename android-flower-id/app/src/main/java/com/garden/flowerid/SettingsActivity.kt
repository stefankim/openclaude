package com.garden.flowerid

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.api_key_setup_title)
        }

        setupLanguageButtons()

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

    private fun setupLanguageButtons() {
        val englishButton = findViewById<Button>(R.id.languageEnglishButton)
        val slovakButton = findViewById<Button>(R.id.languageSlovakButton)

        updateLanguageButtonStyles(englishButton, slovakButton)

        englishButton.setOnClickListener {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
        slovakButton.setOnClickListener {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("sk"))
        }
    }

    private fun isSlovakActive(): Boolean {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            return appLocales[0]?.language == "sk"
        }
        return resources.configuration.locales[0].language == "sk"
    }

    private fun updateLanguageButtonStyles(englishButton: Button, slovakButton: Button) {
        val slovakActive = isSlovakActive()
        val selectedColor = getColor(R.color.garden_green)

        englishButton.backgroundTintList = if (!slovakActive) android.content.res.ColorStateList.valueOf(selectedColor) else null
        englishButton.setTextColor(if (!slovakActive) 0xFFFFFFFF.toInt() else getColor(R.color.garden_green))

        slovakButton.backgroundTintList = if (slovakActive) android.content.res.ColorStateList.valueOf(selectedColor) else null
        slovakButton.setTextColor(if (slovakActive) 0xFFFFFFFF.toInt() else getColor(R.color.garden_green))
    }
}
