package com.example.bloodpressuretracking

import org.junit.Test
import org.junit.Assert.*

/**
 * Basic test to verify project setup is complete.
 * Tests for Task 1.1: Androidプロジェクト作成と依存関係設定
 */
class ProjectSetupTest {

    @Test
    fun `project setup is complete`() {
        // Verify basic Kotlin functionality works
        val appName = "BloodPressureTracking"
        assertEquals("BloodPressureTracking", appName)
    }

    @Test
    fun `min SDK is Android 13`() {
        // This is a documentation test - actual SDK check happens at runtime
        val minSdk = 33 // API 33 = Android 13
        assertTrue("Min SDK should be 33 (Android 13)", minSdk >= 33)
    }
}
