package com.example.streamshare.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class StartAccessibilityService : AccessibilityService() {

    private var counter = true
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName == "com.example.datastructures") {
                // The target app is opened, launch another app.
                counter = if (counter) {
                    launchAnotherApp()
                    false
                } else {
                    true
                }
            }
        }
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    private fun launchAnotherApp() {
        // Code to launch another application.
        val launchIntent = packageManager.getLaunchIntentForPackage("com.example.streamshare")
        if (launchIntent != null){
            startActivity(launchIntent)
        } else {
            Toast.makeText(baseContext, "Stream Share is not installed on this device", Toast.LENGTH_LONG).show()
        }

    }
}