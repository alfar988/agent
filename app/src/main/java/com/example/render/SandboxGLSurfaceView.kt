package com.example.render

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Build
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

/**
 * Custom OpenGL ES 3.0 GLSurfaceView supporting Android touch + PC mouse/pointer capture and keyboard events.
 */
class SandboxGLSurfaceView(
    context: Context,
    val renderer: SandboxGLRenderer
) : GLSurfaceView(context) {

    var onKeyAction: ((Int, Boolean) -> Boolean)? = null
    var onMouseLookDelta: ((Float, Float) -> Unit)? = null
    var onMouseClick: ((Int) -> Unit)? = null
    var onMouseButtonAction: ((Int, Boolean) -> Unit)? = null
    var onMouseScroll: ((Float) -> Unit)? = null

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 24, 0)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) || event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE)) {
            if (event.actionMasked == MotionEvent.ACTION_SCROLL) {
                val vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                onMouseScroll?.invoke(vScroll)
                return true
            }
            if (event.actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasPointerCapture()) {
                    val dx = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)
                    val dy = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)
                    onMouseLookDelta?.invoke(dx, dy)
                    return true
                }
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onCapturedPointerEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_MOVE || event.actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
            onMouseLookDelta?.invoke(event.x, event.y)
            return true
        }
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS) {
            onMouseButtonAction?.invoke(event.buttonState, true)
            onMouseClick?.invoke(event.buttonState)
            return true
        }
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_BUTTON_RELEASE) {
            onMouseButtonAction?.invoke(event.buttonState, false)
            return true
        }
        return super.onCapturedPointerEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (onKeyAction?.invoke(keyCode, true) == true) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (onKeyAction?.invoke(keyCode, false) == true) return true
        return super.onKeyUp(keyCode, event)
    }

    fun togglePointerCapture(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (enable) {
                requestFocus()
                requestPointerCapture()
            } else if (hasPointerCapture()) {
                releasePointerCapture()
            }
        }
    }
}
