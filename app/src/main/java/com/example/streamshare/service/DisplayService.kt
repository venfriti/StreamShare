/*
 * Copyright (C) 2023 pedroSG94.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.streamshare.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.pedro.common.ConnectChecker
import com.pedro.library.base.DisplayBase
import com.pedro.library.rtmp.RtmpDisplay
import com.pedro.library.rtsp.RtspDisplay
import com.example.streamshare.R
import com.example.streamshare.utils.Constants


/**
 * Basic RTMP/RTSP service streaming implementation with camera2
 */
class DisplayService : Service() {

  override fun onCreate() {
    super.onCreate()
    INSTANCE = this
    Log.i(TAG, "RTP Display service create")
    notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
      notificationManager?.createNotificationChannel(channel)
    }
  }

  private fun keepAliveTrick() {
    val notification = NotificationCompat.Builder(this, channelId)
      .setSmallIcon(R.drawable.notification_icon)
      .setSilent(true)
      .setOngoing(false)
      .build()
    startForeground(1, notification)
  }

  override fun onBind(p0: Intent?): IBinder? {
    return null
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    INSTANCE = this
    Log.i(TAG, "RTP Display service started")
    displayBase = RtmpDisplay(baseContext, true, connectChecker)
    displayBase?.glInterface?.setForceRender(true)
    return START_STICKY
  }

  companion object {
    private const val TAG = "DisplayService"
    private const val channelId = "rtpDisplayStreamChannel"
    const val notifyId = 123456
    var INSTANCE: DisplayService? = null
  }

  private var notificationManager: NotificationManager? = null
  private var displayBase: DisplayBase? = null

  fun sendIntent(): Intent? {
    return displayBase?.sendIntent()
  }

  fun isStreaming(): Boolean {
    return displayBase?.isStreaming ?: false
  }

  fun isRecording(): Boolean {
    return displayBase?.isRecording ?: false
  }

  fun stopStream() {
    if (displayBase?.isStreaming == true) {
      displayBase?.stopStream()
      notificationManager?.cancel(notifyId)
    }
  }

  private val connectChecker = object : ConnectChecker {
    override fun onConnectionStarted(url: String) {
      showNotification("Stream connection started")
    }

    override fun onConnectionSuccess() {
      showNotification("Stream started")
      switchBack()
      Log.e(TAG, "RTP service destroy")
    }

    override fun onNewBitrate(bitrate: Long) {

    }

    override fun onConnectionFailed(reason: String) {
      showNotification("Stream connection failed")
      INSTANCE?.stopStream()
      Toast.makeText(applicationContext, getString(R.string.connection_failed, reason), Toast.LENGTH_SHORT)
        .show()
      Log.e(TAG, "RTP service destroy")
    }

    override fun onDisconnect() {
      showNotification("Stream stopped")
    }

    override fun onAuthError() {
      showNotification("Stream auth error")
    }

    override fun onAuthSuccess() {
      showNotification("Stream auth success")
    }
  }

  private fun switchBack() {
    val originalAppLaunchIntent = packageManager.getLaunchIntentForPackage(Constants.APP_PACKAGE_NAME)
    originalAppLaunchIntent?.let {
      startActivity(it)
    }
  }

  private fun showNotification(text: String) {
    val notification = NotificationCompat.Builder(baseContext, channelId)
      .setSmallIcon(R.drawable.notification_icon)
      .setContentTitle("RTP Display Stream")
      .setContentText(text)
      .setOngoing(false)
      .build()
    notificationManager?.notify(notifyId, notification)
  }

  override fun onDestroy() {
    super.onDestroy()
    Log.i(TAG, "RTP Display service destroy")
    stopStream()
    INSTANCE = null
    notificationManager?.cancelAll()
  }

  fun prepareStreamRtp(endpoint: String, resultCode: Int, data: Intent) {
    keepAliveTrick()
    stopStream()
    if (endpoint.startsWith("rtmp")) {
      displayBase = RtmpDisplay(baseContext, true, connectChecker)
      displayBase?.setIntentResult(resultCode, data)
    } else {
      displayBase = RtspDisplay(baseContext, true, connectChecker)
      displayBase?.setIntentResult(resultCode, data)
    }
    displayBase?.glInterface?.setForceRender(true)
  }

  fun startStreamRtp(endpoint: String) {
    if (displayBase?.isStreaming != true) {
      if (displayBase?.prepareVideo() == true && displayBase?.prepareAudio() == true) {
        displayBase?.startStream(endpoint)
      }
    } else {
      showNotification("You are already streaming :(")
    }
  }
}