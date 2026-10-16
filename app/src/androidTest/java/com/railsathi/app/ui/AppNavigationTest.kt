package com.railsathi.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.railsathi.app.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real app UI (MainActivity, navigation, ViewModels, repositories, Room) with the
 * sample API and an in-memory database supplied by TestModules.kt.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun waitForText(text: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun startsOnHome() {
        composeRule.onNodeWithText("RailSathi").assertIsDisplayed()
        composeRule.onNodeWithText("Train number or name").assertIsDisplayed()
    }

    @Test
    fun sampleModeBanner_isVisible() {
        composeRule.onNodeWithText("Sample data mode: nothing shown here is real railway data.").assertIsDisplayed()
    }

    @Test
    fun bottomNavigation_switchesBetweenTopLevelScreens() {
        composeRule.onNodeWithTag("nav_status").performClick()
        composeRule.onNodeWithText("Live running status").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_seats").performClick()
        composeRule.onNodeWithText("Seat availability").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_pnr").performClick()
        composeRule.onNodeWithText("10-digit PNR number").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_saved").performClick()
        composeRule.onNodeWithText("Favourite trains").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.onNodeWithText("Train number or name").assertIsDisplayed()
    }

    @Test
    fun searchingByTrainNumber_opensLiveStatus_withSampleDataLabel() {
        composeRule.onNodeWithText("Train number or name").performTextInput("12951")
        composeRule.onNodeWithContentDescription("Search trains").performClick()

        waitForText("SAMPLE DATA: not a real train position.")
        composeRule.onNodeWithText("Live running status").assertIsDisplayed()
    }

    @Test
    fun pnr_invalidNumber_showsValidationMessage() {
        composeRule.onNodeWithTag("nav_pnr").performClick()
        composeRule.onNodeWithText("10-digit PNR number").performTextInput("123")
        composeRule.onNodeWithText("Check PNR").performClick()

        composeRule.onNodeWithText("Enter a valid 10-digit PNR number.").assertIsDisplayed()
    }

    @Test
    fun pnr_validNumber_showsSampleBooking() {
        composeRule.onNodeWithTag("nav_pnr").performClick()
        composeRule.onNodeWithText("10-digit PNR number").performTextInput("1234567890")
        composeRule.onNodeWithText("Check PNR").performClick()

        waitForText("SAMPLE DATA: this is not a real booking.")
    }

    @Test
    fun availability_missingStations_showsValidationMessages() {
        composeRule.onNodeWithTag("nav_seats").performClick()
        composeRule.onNodeWithText("Check availability").performClick()

        composeRule.onNodeWithText("Enter the source station code.").assertIsDisplayed()
        composeRule.onNodeWithText("Enter the destination station code.").assertIsDisplayed()
    }

    @Test
    fun favouriting_aTrain_addsItToTheSavedTab() {
        composeRule.onNodeWithText("Train number or name").performTextInput("12951")
        composeRule.onNodeWithContentDescription("Search trains").performClick()
        waitForText("SAMPLE DATA: not a real train position.")

        composeRule.onNodeWithContentDescription("Add Mumbai Central - New Delhi Rajdhani to favourites").performClick()
        composeRule.onNodeWithTag("nav_saved").performClick()

        waitForText("Mumbai Central - New Delhi Rajdhani")
    }
}
