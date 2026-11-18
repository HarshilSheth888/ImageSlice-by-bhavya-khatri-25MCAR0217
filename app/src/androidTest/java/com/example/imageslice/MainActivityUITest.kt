package com.example.imageslice

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityUITest {

    @Test
    fun testInitialUIState() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Verify toolbar and buttons are displayed
            onView(withId(R.id.toolbarMain)).check(matches(isDisplayed()))
            onView(withId(R.id.btnSelectImage)).check(matches(isDisplayed()))
            onView(withId(R.id.btnLoadSample)).check(matches(isDisplayed()))

            // Verify grid view image is visible
            onView(withId(R.id.gridImageView)).check(matches(isDisplayed()))

            // Verify default row and column labels (3x3)
            onView(withId(R.id.tvRowsLabel)).check(matches(withText("Horizontal Divisions (Rows): 3")))
            onView(withId(R.id.tvColsLabel)).check(matches(withText("Vertical Divisions (Columns): 3")))

            // View selected slice button should be disabled until a box is picked
            onView(withId(R.id.btnViewSlice)).check(matches(not(isEnabled())))

            // View all slices in grid button should be enabled
            onView(withId(R.id.btnViewAllGrid)).check(matches(isEnabled()))
        }
    }

    @Test
    fun testTapGridSelectsBoxAndEnablesViewSliceButton() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Tap the center of the grid image
            onView(withId(R.id.gridImageView)).perform(click())

            // Button should now be enabled
            onView(withId(R.id.btnViewSlice)).check(matches(isEnabled()))
        }
    }

    @Test
    fun testOpenAllSlicesGridView() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Click Explore All Slices (GridView)
            onView(withId(R.id.btnViewAllGrid)).perform(click())

            // Verify GridView in SliceGridActivity is displayed
            onView(withId(R.id.gridViewSlices)).check(matches(isDisplayed()))
        }
    }
}
