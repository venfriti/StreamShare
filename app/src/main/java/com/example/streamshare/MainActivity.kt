package com.example.streamshare

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.streamshare.RecordCheck.recording
import com.example.streamshare.service.DisplayService
import com.example.streamshare.service.StartAccessibilityService
import com.example.streamshare.ui.theme.StreamShareTheme
import com.example.streamshare.utils.Constants
import com.google.android.material.snackbar.Snackbar
import com.pedro.common.ConnectChecker
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity(), ConnectChecker {

    private lateinit var startStopButton: Button
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

        displayServiceResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.e("MainActivity", result.resultCode.toString())
            val reCode = result.resultCode
            if (result.data != null && (reCode == RESULT_OK)
                ) {
                val data: Intent? = result.data
                val displayService = DisplayService.INSTANCE
                if (displayService != null){
                    val endpoint: String = enterUrl.text.toString()
//                    val endpoint: String = Constants.RTMP_URL
                    displayService.prepareStreamRtp(endpoint, reCode, data!!)
                    displayService.startStreamRtp(endpoint)
                }
            } else {
                Toast.makeText(this, getString(R.string.start_permission_denied), Toast.LENGTH_SHORT).show()
                startStopButton.setText(R.string.start_button)
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



    private fun showPermissionSnackBar(){
        Snackbar.make(
            findViewById(
                R.id.activity_example_rtmp
            ),
            "You need to enable required permissions to use this service",
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.settings) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")

                // Start the settings activity
                startActivity(intent)
            }.show()
    }

    private fun hideSoftKeyboard() {
        val imm =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(startStopButton.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        enabled = isAccessibilityServiceEnabled(
            applicationContext,
            StartAccessibilityService::class.java
        )
        val displayService: DisplayService? = DisplayService.INSTANCE
        if (displayService != null && displayService.isStreaming()) {
            startStopButton.setText(R.string.stop_button)
        } else {
            startStopButton.setText(R.string.start_button)
        }
    }


    private fun startStopStream() {
        if (hasPermissions(applicationContext)){
            val displayService: DisplayService? = DisplayService.INSTANCE
            if (displayService != null) {
                if (!displayService.isStreaming()) {
                    hideSoftKeyboard()
                    val displayIntent = displayService.sendIntent()
                    displayServiceResultLauncher.launch(displayIntent)
                } else {
                    startStopButton.setText(R.string.start_button)
                    displayService.stopStream()
                }
            }
        }
        else {
            showPermissionSnackBar()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val displayService: DisplayService? = DisplayService.INSTANCE
        if (displayService != null && !displayService.isStreaming() && !displayService.isRecording()) {
            //stop service only if no streaming or recording
            stopService(Intent(this, DisplayService::class.java))
        }
        recording = false
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

    override fun onConnectionStarted(url: String) {
    }

    override fun onConnectionSuccess() {
        Toast.makeText(this, getString(R.string.connection_success), Toast.LENGTH_SHORT).show()

    }

    override fun onConnectionFailed(reason: String) {
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

    @Composable
    fun StreamShareLayout(){
        var urlInput by remember { (mutableStateOf("")) }

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 40.dp)
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.enter_url),
                modifier = Modifier
                    .padding(bottom = 16.dp, top = 40.dp)
                    .align(alignment = Alignment.Start)
            )
            EditUrlField(
                R.string.hint_url,
                R.drawable.notification_icon,
                urlInput,
                onValueChanged = {urlInput = it},
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .fillMaxWidth()
            )
        }
    }

    @Composable
    fun EditUrlField(
        @StringRes label: Int,
        @DrawableRes leadingIcon: Int,
        value: String,
        onValueChanged: (String) -> Unit,
        keyboardOptions: KeyboardOptions,
        modifier: Modifier = Modifier
    ){
        TextField(
            value = value,
            onValueChange = onValueChanged,
            leadingIcon = { Icon(
                painter = (painterResource(id = leadingIcon)), null,
                modifier = Modifier.size(32.dp)
            ) },
            label = { Text(stringResource(id = label))},
            singleLine = true,
            keyboardOptions = keyboardOptions,
            modifier = modifier
        )
    }

    @Preview(showSystemUi = true)
    @Composable
    fun StreamShareLayoutPreview() {
        StreamShareTheme {
            Surface(
                Modifier.fillMaxSize()
            ) {
                StreamShareLayout()
            }
        }
    }


}