package com.cleanown.fun_test

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

val TAG = "cle--"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val txt = findViewById<TextView>(R.id.device_text)
        val btn = findViewById<Button>(R.id.device_btn)

        btn.setOnClickListener {
//            getAdvertisingId {
//                txt.text = "google ad id: ${it}"
//                Log.d(TAG, "getAdvertisingId: ${it}")

//                val cpu = Build.getRadioVersion()
//                Log.i(TAG+"CPUInfo", "CPU: $cpu")
//                getCpuInfo()
//
//                Thread {
//                    getGpuInfo()
//                }.start()
//            }
            try {
                val oaid = OAIDTool.getAIString(applicationContext)
                txt.text = "google ad id: ${oaid}"
                Log.d(TAG, "oaid:${oaid}")
            } catch (e: Exception) {
                txt.text = "google ad id: ${e.message}"
                Log.d(TAG, "onCreate-e: ${e.message}")
            }
        }
    }

    fun getAdvertisingId(onIdReceived: (String) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)
                val advertisingId = adInfo.id
                // 在主线程中处理获取到的广告标识符
                launch(Dispatchers.Main) {
                    if (advertisingId != null) {
                        onIdReceived(advertisingId)
                    } else {
                        onIdReceived("unknown")
                    }
                }
            } catch (e: GooglePlayServicesNotAvailableException) {
                Log.d(TAG, "getAdvertisingId-GooglePlayServicesNotAvailableException: ${e.errorCode}")
                e.printStackTrace()
                // 在主线程中处理获取失败的情况
                launch(Dispatchers.Main) {
                    onIdReceived("GooglePlayServicesNotAvailableException")
                }
            } catch (e: Exception) {
                Log.d(TAG, "getAdvertisingId-Exception: ${e.localizedMessage}")
                e.printStackTrace()
                // 在主线程中处理获取失败的情况
                launch(Dispatchers.Main) {
                    onIdReceived("Exception")
                }
            }
        }
    }

    fun getGpuInfo() {
        // 1. 获取默认的 EGL display
        val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.e(TAG+"GPUInfo", "Unable to get EGL14 display")
            return
        }

        // 2. 初始化 EGL display
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            Log.e(TAG+"GPUInfo", "Unable to initialize EGL14")
            return
        }

        // 3. 配置 EGL
        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_STENCIL_SIZE, 8,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, configs.size, numConfigs, 0)) {
            Log.e(TAG+"GPUInfo", "Unable to find a suitable EGLConfig")
            return
        }
        val eglConfig = configs[0]

        // 4. 创建一个 EGL context
        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        val eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        if (eglContext == null || eglContext == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG+"GPUInfo", "Unable to create EGL context")
            return
        }

        // 5. 创建一个 offscreen EGL surface
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, 1,
            EGL14.EGL_HEIGHT, 1,
            EGL14.EGL_NONE
        )
        val eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttribs, 0)
        if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.e(TAG+"GPUInfo", "Unable to create EGL surface")
            return
        }

        // 6. 绑定 EGL context 和 surface
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            Log.e(TAG+"GPUInfo", "Unable to make EGL context current")
            return
        }

        // 7. 获取 GPU 渲染器信息
        val renderer = GLES20.glGetString(GLES20.GL_RENDERER)
        val vendor = GLES20.glGetString(GLES20.GL_VENDOR)
        val versionString = GLES20.glGetString(GLES20.GL_VERSION)

        Log.i(TAG+"GPUInfo", "Renderer: $renderer")
        Log.i(TAG+"GPUInfo", "Vendor: $vendor")
        Log.i(TAG+"GPUInfo", "Version: $versionString")

        // 8. 清理资源
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
        EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglTerminate(eglDisplay)
    }

    fun getCpuInfo() {
        val hardware = Build.HARDWARE
        Log.d(TAG, "getCpuInfo-hardware: ${hardware}")
        var hardwareInfo = ""
        try {
            val reader = BufferedReader(FileReader("/proc/cpuinfo"))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("Hardware") == true) {
                    try {
                        hardwareInfo = line!!.substring(line!!.indexOf(":") + 1).trim()
                    } catch (e: Exception) {
                        Log.d(TAG, "getCpuInfo: 获取CPU信息错误")
                    }
                    break
                }
            }
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Log.d(TAG, "getCpuInfo: ${hardwareInfo}")
    }
}