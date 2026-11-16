package com.arijit.pomodoro.system

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TorchManager(private val context: Context) {

    private val cameraManager by lazy {
        runCatching { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }.getOrNull()
    }

    private fun getBackCameraWithFlashId(): String? {
        val manager = cameraManager ?: return null
        return runCatching {
            manager.cameraIdList.firstOrNull { id ->
                val characteristics = manager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            }
        }.getOrNull()
    }

    fun blinkTorch(
        pulseCount: Int = 3,
        pulseDurationMs: Long = 150L,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
        onScreenFlashFallback: (() -> Unit)? = null
    ) {
        val cameraId = getBackCameraWithFlashId()
        if (cameraId == null) {
            onScreenFlashFallback?.invoke()
            return
        }

        scope.launch {
            runCatching {
                val manager = cameraManager ?: return@launch
                repeat(pulseCount) {
                    manager.setTorchMode(cameraId, true)
                    delay(pulseDurationMs)
                    manager.setTorchMode(cameraId, false)
                    delay(pulseDurationMs)
                }
            }.onFailure {
                // Torch busy, camera unavailable, or permission/security issue: fallback safely
                onScreenFlashFallback?.invoke()
            }
        }
    }
}

