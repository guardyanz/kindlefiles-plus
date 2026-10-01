package io.github.guardyanz.kindlefiles

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Runs real Activity + Compose navigation on an Android 9 runtime without a physical device. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w800dp-h1280dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun firstLaunchExplainsStorageAndNavigatesWithoutPermissions() {
        compose.onNodeWithText("Connect a folder").assertIsDisplayed()
        compose.onNodeWithText("Pick downloads / files").assertIsDisplayed()
        compose.onNodeWithText("Favorites").performClick()
        compose.onNodeWithText("Keep your go-to folders close").assertIsDisplayed()
        compose.onNodeWithText("File tools").performClick()
        compose.onNodeWithText("Find duplicates").assertIsDisplayed()
        compose.onNodeWithText("Scan current folder").assertIsNotEnabled()
    }

    @Test fun aboutShowsProvenanceAndCanBeDismissed() {
        compose.onNodeWithContentDescription("About FolderKit").performClick()
        compose.onNodeWithText("About FolderKit").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Connect a folder").assertIsDisplayed()
    }
}
