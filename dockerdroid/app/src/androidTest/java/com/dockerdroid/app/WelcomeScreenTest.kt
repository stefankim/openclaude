package com.dockerdroid.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dockerdroid.app.ui.screens.WelcomeScreen
import com.dockerdroid.app.ui.theme.DockerDroidTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun welcome_shows_title_and_continues() {
        var continued = false
        rule.setContent {
            DockerDroidTheme { WelcomeScreen(onContinue = { continued = true }) }
        }

        rule.onNodeWithText("DockerDroid").assertIsDisplayed()
        rule.onNodeWithText("Get started").performClick()
        assertTrue(continued)
    }
}
