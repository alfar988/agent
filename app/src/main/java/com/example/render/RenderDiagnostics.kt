package com.example.render

import java.util.concurrent.atomic.AtomicReference

data class RenderDiagnosticsSnapshot(
    val eglContextReady: Boolean = false,
    val glVendor: String = "Pending",
    val glRenderer: String = "Pending",
    val glVersion: String = "OpenGL ES 3.0",
    val viewportWidth: Int = 0,
    val viewportHeight: Int = 0,
    val fps: Int = 0,
    val frameTimeMs: Float = 0f,
    val drawCalls: Int = 0,
    val culledObjects: Int = 0,
    val particleCount: Int = 0,
    val shaderCompiledOk: Boolean = false,
    val shadowFboComplete: Boolean = false,
    val scaleFboComplete: Boolean = true,
    val scaleFboActive: Boolean = false,
    val assetsUploaded: Boolean = false,
    val characterGlbLoaded: Boolean = false,
    val propGlbLoadedCount: Int = 0,
    val glbValidationErrorCount: Int = 0,
    val lastGlErrorCode: Int = 0,
    val statusMessage: String = "Initializing EGL"
)

class RenderDiagnosticsTracker {
    val snapshotRef = AtomicReference(RenderDiagnosticsSnapshot())

    fun update(transform: (RenderDiagnosticsSnapshot) -> RenderDiagnosticsSnapshot) {
        while (true) {
            val prev = snapshotRef.get()
            val next = transform(prev)
            if (snapshotRef.compareAndSet(prev, next)) break
        }
    }
}
