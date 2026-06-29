package acr.browser.lightning.input

import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Unified handler for handheld input including gamepad, D-pad, and analog sticks.
 * Translates physical input to browser navigation and scrolling commands.
 */
class HandheldInputHandler : GameControllerManager.GameControllerListener {

    interface NavigationListener {
        fun onNavigateUp()
        fun onNavigateDown()
        fun onNavigateLeft()
        fun onNavigateRight()
        fun onConfirm()
        fun onBack()
        fun onScrollUp()
        fun onScrollDown()
        fun onScrollLeft()
        fun onScrollRight()
    }

    private var navigationListener: NavigationListener? = null

    fun setNavigationListener(listener: NavigationListener?) {
        navigationListener = listener
    }

    override fun onDPadPressed(direction: GameControllerManager.DPadDirection) {
        when (direction) {
            GameControllerManager.DPadDirection.UP -> navigationListener?.onNavigateUp()
            GameControllerManager.DPadDirection.DOWN -> navigationListener?.onNavigateDown()
            GameControllerManager.DPadDirection.LEFT -> navigationListener?.onNavigateLeft()
            GameControllerManager.DPadDirection.RIGHT -> navigationListener?.onNavigateRight()
            GameControllerManager.DPadDirection.NONE -> {}
        }
    }

    override fun onAnalogStickMoved(x: Float, y: Float, isLeftStick: Boolean) {
        if (isLeftStick) {
            // Left stick: navigation
            when {
                y < -0.5f -> navigationListener?.onNavigateUp()
                y > 0.5f -> navigationListener?.onNavigateDown()
                x < -0.5f -> navigationListener?.onNavigateLeft()
                x > 0.5f -> navigationListener?.onNavigateRight()
            }
        } else {
            // Right stick: scrolling
            when {
                y < -0.5f -> navigationListener?.onScrollUp()
                y > 0.5f -> navigationListener?.onScrollDown()
                x < -0.5f -> navigationListener?.onScrollLeft()
                x > 0.5f -> navigationListener?.onScrollRight()
            }
        }
    }

    override fun onButtonPressed(button: GameControllerManager.GameButtonCode) {
        when (button) {
            GameControllerManager.GameButtonCode.A -> navigationListener?.onConfirm()
            GameControllerManager.GameButtonCode.B -> navigationListener?.onBack()
            GameControllerManager.GameButtonCode.X -> navigationListener?.onScrollUp()
            GameControllerManager.GameButtonCode.Y -> navigationListener?.onScrollDown()
            else -> {}
        }
    }

    override fun onButtonReleased(button: GameControllerManager.GameButtonCode) {
        // No action needed on release for most buttons
    }
}
