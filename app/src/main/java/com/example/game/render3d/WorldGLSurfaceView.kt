package com.example.game.render3d

import android.content.Context
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.game.engine.GameEngine
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

object GraphicsPipelineInfo {
    @Volatile
    var glRendererString: String = ""

    fun hasVulkanHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
    }

    fun pipelineBadge(context: Context): String {
        val vulkan = hasVulkanHardware(context)
        val renderer = glRendererString
        return when {
            renderer.contains("ANGLE", ignoreCase = true) || renderer.contains("Vulkan", ignoreCase = true) ->
                "VULKAN / ANGLE 3D • 4x MSAA"
            vulkan -> "VULKAN / ANGLE READY • 4x MSAA"
            else -> "GLES 3.0 3D • 4x MSAA"
        }
    }
}

private class MsaaEglConfigChooser : GLSurfaceView.EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        val msaaSpec = intArrayOf(
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 24,
            EGL10.EGL_SAMPLE_BUFFERS, 1,
            EGL10.EGL_SAMPLES, 4,
            EGL10.EGL_RENDERABLE_TYPE, 0x0040, // EGL_OPENGL_ES3_BIT_KHR
            EGL10.EGL_NONE
        )
        chooseBest(egl, display, msaaSpec)?.let { return it }

        val msaa2xSpec = intArrayOf(
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 24,
            EGL10.EGL_SAMPLE_BUFFERS, 1,
            EGL10.EGL_SAMPLES, 2,
            EGL10.EGL_RENDERABLE_TYPE, 0x0004, // EGL_OPENGL_ES2_BIT fallback
            EGL10.EGL_NONE
        )
        chooseBest(egl, display, msaa2xSpec)?.let { return it }

        val fallbackSpec = intArrayOf(
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16,
            EGL10.EGL_RENDERABLE_TYPE, 0x0004,
            EGL10.EGL_NONE
        )
        return chooseBest(egl, display, fallbackSpec)
            ?: throw IllegalStateException("No compatible EGLConfig found")
    }

    private fun chooseBest(egl: EGL10, display: EGLDisplay, spec: IntArray): EGLConfig? {
        val numConfig = IntArray(1)
        if (!egl.eglChooseConfig(display, spec, null, 0, numConfig) || numConfig[0] <= 0) {
            return null
        }
        val configs = arrayOfNulls<EGLConfig>(numConfig[0])
        if (!egl.eglChooseConfig(display, spec, configs, configs.size, numConfig)) {
            return null
        }
        return configs.firstOrNull { it != null }
    }
}

@Composable
fun WorldGLSurfaceView(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val glSurfaceView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(MsaaEglConfigChooser())
            preserveEGLContextOnPause = true
            setRenderer(WorldGLRenderer(engine))
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glSurfaceView.onResume()
                Lifecycle.Event.ON_PAUSE -> glSurfaceView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glSurfaceView.onPause()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    engine.rotateCamera(
                        deltaYawRad = dragAmount.x * 0.0055f,
                        deltaPitchRad = dragAmount.y * 0.0040f
                    )
                }
            }
    ) {
        AndroidView(
            factory = { glSurfaceView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
