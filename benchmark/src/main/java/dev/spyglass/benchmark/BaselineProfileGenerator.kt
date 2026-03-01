package dev.spyglass.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = "dev.spyglass.android",
            includeInStartupProfile = true,
        ) {
            // Cold start
            pressHome()
            startActivityAndWait()

            // Navigate to Browse
            device.findObject(
                androidx.test.uiautomator.By.text("Browse")
            )?.click()
            device.waitForIdle()

            // Navigate to Search
            device.findObject(
                androidx.test.uiautomator.By.text("Search")
            )?.click()
            device.waitForIdle()
        }
    }
}
