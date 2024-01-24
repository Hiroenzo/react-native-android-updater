package com.androidupdater

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log

import androidx.core.content.FileProvider

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.Promise
import com.facebook.react.modules.core.DeviceEventManagerModule

import com.azhon.appupdate.listener.OnDownloadListenerAdapter
import com.azhon.appupdate.manager.DownloadManager
import com.azhon.appupdate.util.ApkUtil

import java.io.File

class AndroidUpdaterModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {
  private val context: Context
  private var manager: DownloadManager? = null

  private val LOG_TAG = "AppUpdateLog"
  private val apkName = "release.apk"

  override fun getName() = "AndroidUpdater"

  init {
    context = reactContext
  }

  private fun sendEvent(
    reactContext: ReactContext,
    params: Int
  ) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("UpdateProgress", params)
  }

  /**
   * 开始下载更新
   */
  @ReactMethod
  fun downloadApk(params: ReadableMap, promise: Promise) {
    try {
      val url = params.getString("url")
      var md5 = params.getString("md5")
      val activity = currentActivity
      if (activity == null) {
        promise.reject("DownloadAPKError", "activity is null")
        return
      }
      if (url == null) {
        promise.reject("DownloadAPKError", "下载地址不能为空")
        return
      }
      if (md5 == null) {
        md5 = " "
      }
      manager = DownloadManager.Builder(activity)
        .apkUrl(url)
        .apkName(apkName)
        .smallIcon(getLauncherIconResourceId())
        .apkMD5(md5)
        .onDownloadListener(listenerAdapter)
        .build()
      manager!!.download()
      promise.resolve(true)
    } catch (ex: Exception) {
      Log.d(LOG_TAG, ex.toString())
      promise.reject("DownloadAPKError", ex.toString())
    }
  }

  /**
   * 取消下载
   */
  @ReactMethod
  fun cancelDownloadApk(promise: Promise) {
    try {
      if (manager == null) {
        promise.resolve("没有下载任务")
        return
      }
      manager!!.cancel()
      promise.resolve("取消成功")
    } catch (ex: Exception) {
      Log.d(LOG_TAG, ex.toString())
      promise.reject("CancelAPKDownloadError", ex.toString())
    }
  }

  /**
   * 手动安装程序
   */
  @ReactMethod
  fun installApk(promise: Promise) {
    try {
      val activity = currentActivity
      if (activity == null) {
        promise.reject("InstallAPKError", "activity is null")
        return
      }
      val intent = Intent(Intent.ACTION_VIEW)
      val filePath = activity.application.externalCacheDir!!.path + "/" + apkName
      val apk = File(filePath)
      if (apk.exists()) {
        //判断是否是AndroidN以及更高的版本
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
          FileProvider.getUriForFile(context, context.packageName + ".fileProvider", apk)
        } else {
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          Uri.fromFile(apk)
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        context.startActivity(intent)
        promise.resolve(true)
      } else {
        promise.reject("InstallAPKError", "安装文件不存在")
      }
    } catch (ex: Exception) {
      Log.d(LOG_TAG, ex.toString())
      promise.reject("InstallAPKError", ex.toString())
    }
  }

  /**
   * 删除已下载的apk文件
   */
  @ReactMethod
  fun deleteApk(promise: Promise) {
    try {
      val activity = currentActivity
      if (activity == null) {
        promise.reject("InstallAPKError", "activity is null")
        return
      }
      val filePath = activity.application.externalCacheDir!!.path + "/" + apkName
      val result = ApkUtil.deleteOldApk(context, filePath)
      promise.resolve(result)
    } catch (ex: Exception) {
      Log.d(LOG_TAG, ex.toString())
      promise.reject("DeleteAPKError", ex.toString())
    }
  }

  @ReactMethod
  fun addListener(eventName: String?) {
  }

  @ReactMethod
  fun removeListeners(count: Int?) {
  }

  /**
   * 监听下载进度
   */
  private val listenerAdapter: OnDownloadListenerAdapter = object : OnDownloadListenerAdapter() {
    override fun downloading(max: Int, progress: Int) {
      val curr = (progress / max.toDouble() * 100.0).toInt()
      sendEvent(context as ReactContext, curr)
    }
  }

  private fun getLauncherIconResourceId(): Int {
    return try {
      val packageName = context.packageName
      val packageManager: PackageManager = context.packageManager
      val applicationInfo: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
      applicationInfo.icon
    } catch (e: Exception) {
      0
    }
  }
}
