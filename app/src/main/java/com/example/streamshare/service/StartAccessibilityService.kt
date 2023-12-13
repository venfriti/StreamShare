package com.example.streamshare.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.streamshare.R

class StartAccessibilityService : AccessibilityService() {

    private var counter = true
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            val packageName = event.packageName?.toString()
            if (packageName == getString(R.string.target_app)) {
                counter = if (counter) {
                    launchAnotherApp()
                    false
                } else {
                    true
                }
            }
        }
    }

//    override fun onServiceConnected() {
//        val info = AccessibilityServiceInfo()
//        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
//        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
//        serviceInfo = info
//    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    private fun launchAnotherApp() {
        // Code to launch another application.
        val launchIntent = packageManager.getLaunchIntentForPackage("com.example.streamshare")
        if (launchIntent != null){
            startActivity(launchIntent)
        } else {
            Log.d("checker", "checker")
            Toast.makeText(baseContext, "Stream Share is not installed on this device", Toast.LENGTH_LONG).show()
        }

    }
}