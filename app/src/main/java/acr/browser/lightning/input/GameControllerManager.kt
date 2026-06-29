package acr.browser.lightning.input

import android.content.Context
import android.os.Build
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.RequiresApi

/**
 * Manages gamepad/controller input for handheld devices.
 * Supports D-pad navigation, analog stick input, and button mappings.
 */
class GameControllerManager(private val context: Context) {

    private var listeners: MutableList<GameControllerListener> = mutableListOf()

    interface GameControllerListener {
        fun onDPadPressed(direction: DPadDirection)
        fun onAnalogStickMoved(x: Float, y: Float, isLeftStick: Boolean)
        fun onButtonPressed(buttonCode: GameButtonCode)
        fun onButtonReleased(buttonCode: GameButtonCode)
    }

    enum class DPadDirection {
        UP, DOWN, LEFT, RIGHT, NONE
    }

    enum class GameButtonCode(val keyCode: Int) {
        A(KeyEvent.KEYCODE_BUTTON_A),
        B(KeyEvent.KEYCODE_BUTTON_B),
        X(KeyEvent.KEYCODE_BUTTON_X),
        Y(KeyEvent.KEYCODE_BUTTON_Y),
        START(KeyEvent.KEYCODE_BUTTON_START),
        SELECT(KeyEvent.KEYCODE_BUTTON_SELECT),
        L1(KeyEvent.KEYCODE_BUTTON_L1),
        R1(KeyEvent.KEYCODE_BUTTON_R1),
        L2(KeyEvent.KEYCODE_BUTTON_L2),
        R2(KeyEvent.KEYCODE_BUTTON_R2),
        THUMBL(KeyEvent.KEYCODE_BUTTON_THUMBL),
        THUMBR(KeyEvent.KEYCODE_BUTTON_THUMBR)
    }

    fun addListener(listener: GameControllerListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: GameControllerListener) {
        listeners.remove(listener)
    }

    /**
     * Process key events from gamepad input.
     * Returns true if the event was handled.
     */
    fun onKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        return when {
            keyCode == KeyEvent.KEYCODE_DPAD_UP -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    listeners.forEach { it.onDPadPressed(DPadDirection.UP) }
                }
                true
            }
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    listeners.forEach { it.onDPadPressed(DPadDirection.DOWN) }
                }
                true
            }
            keyCode == KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    listeners.forEach { it.onDPadPressed(DPadDirection.LEFT) }
                }
                true
            }
            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    listeners.forEach { it.onDPadPressed(DPadDirection.RIGHT) }
                }
                true
            }
            isGameControllerButton(keyCode) -> {
                val button = GameButtonCode.values().find { it.keyCode == keyCode }
                if (button != null) {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> listeners.forEach { it.onButtonPressed(button) }
                        KeyEvent.ACTION_UP -> listeners.forEach { it.onButtonReleased(button) }
                    }
                }
                true
            }
            else -> false
        }
    }

    /**
     * Process motion events from analog sticks and triggers.
     */
    fun onMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.TOOL_TYPE_UNKNOWN == 0) {
            return false
        }

        val inputDevice = InputDevice.getDevice(event.deviceId)
        if (inputDevice == null || inputDevice.sources and InputDevice.TOOL_TYPE_UNKNOWN == 0) {
            return false
        }

        // Left analog stick
        val lx = getCenteredAxis(event, MotionEvent.AXIS_X)
        val ly = getCenteredAxis(event, MotionEvent.AXIS_Y)
        if (Math.abs(lx) > 0.01f || Math.abs(ly) > 0.01f) {
            listeners.forEach { it.onAnalogStickMoved(lx, ly, true) }
        }

        // Right analog stick
        val rx = getCenteredAxis(event, MotionEvent.AXIS_Z)
        val ry = getCenteredAxis(event, MotionEvent.AXIS_RZ)
        if (Math.abs(rx) > 0.01f || Math.abs(ry) > 0.01f) {
            listeners.forEach { it.onAnalogStickMoved(rx, ry, false) }
        }

        return true
    }

    private fun isGameControllerButton(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_THUMBL,
            KeyEvent.KEYCODE_BUTTON_THUMBR -> true
            else -> false
        }
    }

    private fun getCenteredAxis(event: MotionEvent, axis: Int): Float {
        val range = InputDevice.getDevice(event.deviceId)?.getMotionRange(
            axis,
            event.source
        ) ?: return 0f

        val flat = range.flat
        val value = event.getAxisValue(axis)

        return when {
            Math.abs(value) > flat -> value
            else -> 0f
        }
    }

    /**
     * Check if any gamepad is connected.
     */
    fun isGameControllerConnected(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return false
        }
        return InputDevice.getDeviceIds().any { deviceId ->
            val device = InputDevice.getDevice(deviceId)
            val sources = device?.sources ?: 0
            (sources and InputDevice.TOOL_TYPE_UNKNOWN) != 0
        }
    }
}
