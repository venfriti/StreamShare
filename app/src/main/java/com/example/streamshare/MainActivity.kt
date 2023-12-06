package com.example.streamshare

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.streamshare.service.DisplayService
import com.example.streamshare.service.StartAccessibilityService
import com.google.android.material.snackbar.Snackbar
import com.pedro.common.ConnectChecker
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity(), ConnectChecker {

    private lateinit var startStopButton: Button
    private lateinit var backButton: Button
    private lateinit var checkButton: Button
    private lateinit var enterUrl: EditText
    private var enabled by Delegates.notNull<Boolean>()

    private lateinit var displayServiceResultLauncher: ActivityResultLauncher<Intent>

    private val PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private val PERMISSIONS_A_13 = arrayOf(
        Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
        Manifest.permission.POST_NOTIFICATIONS
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.activity_example_rtmp)
        ) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestPermissions()

        startStopButton = findViewById(R.id.start_stop_button)
        backButton = findViewById(R.id.back_button)
        checkButton = findViewById(R.id.check_button)
        enterUrl = findViewById(R.id.enter_url)

        enabled = isAccessibilityServiceEnabled(applicationContext, StartAccessibilityService::class.java)
        if (!enabled){
            showSnackBar()
        }

        startStopButton.setOnClickListener {
            if (enabled) {
                startStopStream()
            } else {
                showSnackBar()
            }
        }

        backButton.setOnClickListener {
            switchBack()
        }

        checkButton.setOnClickListener {
            if(enabled){
                Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
            }
        }


        displayServiceResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.e("MainActivity", result.resultCode.toString())
            val reCode = result.resultCode
            if (result.data != null && (reCode == RESULT_OK)
                ) {
                // Handle the result here
                val data: Intent? = result.data
                val displayService = DisplayService.INSTANCE
                if (displayService != null){
                    val endpoint: String = enterUrl.text.toString()
                    displayService.prepareStreamRtp(endpoint, reCode, data!!)
                    displayService.startStreamRtp(endpoint)
                    switchBack()
                }
            } else {
                Toast.makeText(this, getString(R.string.start_permission_denied), Toast.LENGTH_SHORT).show()
                startStopButton.setText(R.string.start_button)
                switchBack()
            }
        }

        val displayService: DisplayService? = DisplayService.INSTANCE
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

    private fun showSnackBar() {
        Snackbar.make(
            findViewById(
                R.id.activity_example_rtmp
            ),
            R.string.permission_denied_explanation,
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.settings) {
                openAccessibilitySettings()
            }.show()
    }

    override fun onResume() {
        super.onResume()
        enabled = isAccessibilityServiceEnabled(applicationContext, StartAccessibilityService::class.java)
    }


    private fun startStopStream() {
        val displayService: DisplayService? = DisplayService.INSTANCE
        if (displayService != null) {
            if (!displayService.isStreaming()) {
                startStopButton.setText(R.string.stop_button)
                val displayIntent = displayService.sendIntent()
                displayServiceResultLauncher.launch(displayIntent)

            } else {
                startStopButton.setText(R.string.start_button)
                displayService.stopStream()
            }
        }
    }
    private fun switchBack() {
        val originalAppLaunchIntent = packageManager.getLaunchIntentForPackage("com.example.datastructures")
        originalAppLaunchIntent?.let {
            startActivity(it)
            finish()
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

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermissions(this)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_A_13, 1)
            }
        } else {
            if (!hasPermissions(this)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, 1)
            }
        }
    }

//    private fun showPermissionsErrorAndRequest() {
//        Toast.makeText(this, "You need permissions before", Toast.LENGTH_SHORT).show()
//        requestPermissions()
//    }

    private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
        if (context != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    private fun isAccessibilityServiceEnabled(context: Context, accessibilityService: Class<*>?): Boolean {
        val expectedComponentName = ComponentName(context, accessibilityService!!)
        val enabledServicesSetting =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
                ?: return false
        val colonSplitter = SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) return true
        }
        return false
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun hasPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermissions(context, *PERMISSIONS_A_13)
        } else {
            hasPermissions(context, *PERMISSIONS)
        }
    }

    override fun onConnectionStarted(url: String) {}

    override fun onConnectionSuccess() {
        Toast.makeText(this, getString(R.string.connection_success), Toast.LENGTH_SHORT).show()
    }

    override fun onConnectionFailed(reason: String) {
        Toast.makeText(this, getString(R.string.connection_failed, reason), Toast.LENGTH_SHORT)
            .show()
        DisplayService.INSTANCE?.stopStream()
        startStopButton.setText(R.string.start_button)
    }

    override fun onNewBitrate(bitrate: Long) {}

    override fun onDisconnect() {
        Toast.makeText(this, getString(R.string.disconnected), Toast.LENGTH_SHORT).show()
    }

    override fun onAuthError() {
        Toast.makeText(this, getString(R.string.auth_error), Toast.LENGTH_SHORT).show()
    }

    override fun onAuthSuccess() {
        Toast.makeText(this, getString(R.string.auth_success), Toast.LENGTH_SHORT).show()
    }

}