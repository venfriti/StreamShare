package com.example.streamshare

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.streamshare.service.DisplayService
import com.pedro.common.ConnectChecker


class MainActivity : AppCompatActivity(), ConnectChecker {


    private lateinit var startStopButton: Button
    private lateinit var enterUrl: EditText

    private val REQUEST_CODE_STREAM = 179 //random num

    private val REQUEST_CODE_RECORD = 180 //random num



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_example_rtmp)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        startStopButton = findViewById(R.id.start_stop_button)
        enterUrl = findViewById(R.id.enter_url)

        startStopButton.setOnClickListener {
            startStopStream()
        }

        val displayService: DisplayService? = DisplayService.Companion.INSTANCE
        //No streaming/recording start service
        if (displayService == null) {
            startService(Intent(this, DisplayService::class.java))
        }
        if (displayService != null && displayService.isStreaming()) {
            startStopButton.setText(R.string.stop_button)
        } else {
            startStopButton.setText(R.string.start_button)
        }
    }

    private fun startStopStream() {
        val displayService: DisplayService? = DisplayService.Companion.INSTANCE
        if (displayService != null) {
            if (!displayService.isStreaming()) {
                startStopButton.setText(R.string.stop_button)
                startActivityIfNeeded(displayService.sendIntent()!!, REQUEST_CODE_STREAM)
            } else {
                startStopButton.setText(R.string.start_button)
                displayService.stopStream()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val displayService: DisplayService? = DisplayService.INSTANCE
        if (displayService != null && !displayService.isStreaming() && !displayService.isRecording()) {
            //stop service only if no streaming or recording
            stopService(Intent(this, DisplayService::class.java))
        }
    }

    override fun onConnectionStarted(url: String) {}

    override fun onConnectionSuccess() {
        Toast.makeText(this, "Connection success", Toast.LENGTH_SHORT).show()
    }

    override fun onConnectionFailed(reason: String) {
        Toast.makeText(this, "Connection failed. $reason", Toast.LENGTH_SHORT)
            .show()
        DisplayService.INSTANCE?.stopStream()
        startStopButton.setText(R.string.start_button)
    }

    override fun onNewBitrate(bitrate: Long) {}

    override fun onDisconnect() {
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
    }

    override fun onAuthError() {
        Toast.makeText(this, "Auth error", Toast.LENGTH_SHORT).show()
    }

    override fun onAuthSuccess() {
        Toast.makeText(this, "Auth success", Toast.LENGTH_SHORT).show()
    }

}