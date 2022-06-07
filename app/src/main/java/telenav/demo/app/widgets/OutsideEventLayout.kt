package telenav.demo.app.widgets

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children

/**
 * A layout will monitor it's touch event, callback if touch happens outside a certain child view
 *
 * @since 2022-06-07
 */
class OutsideEventLayout: ConstraintLayout {
    companion object {
        private const val TAG = "OutsideEventLayout"
    }

    // to storage listeners
    private val mListeners: MutableMap<View, OnTouchOutsideListener> = mutableMapOf()

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int,
                defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // process only down action
        if (ev.action == MotionEvent.ACTION_DOWN) {
            // notify every listener while touch not in child's rect
            for (entry in mListeners) {
                if (!isTouchInChild(entry.key, ev)) entry.value.onTouchOutside(entry.key)
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun isTouchInChild(child: View, ev: MotionEvent): Boolean {
        // when the view's width or height not ready, return true to prevent mis-callback
        if (width == 0 || height == 0) {
            Log.i(TAG, "isTouchInChild: width = $width, height = $height, might not drew yet")
            return true
        }
        val outLocation = intArrayOf(0, 0) // will hold left-top corner point coordinate
        child.getLocationOnScreen(outLocation)
        val rect = Rect(outLocation[0], outLocation[1], outLocation[0] + child.width, outLocation[1] + child.height)
        return rect.contains(ev.rawX.toInt(), ev.rawY.toInt())
    }

    /**
     * Set listener for a child view
     *
     * @param view child view of this layout
     * @param listener OnTouchOutsideListener
     */
    fun setListener(view: View, listener: OnTouchOutsideListener) {
        children.find { it === view }?.let {
            mListeners[it] = listener
            return
        }
        Log.e(TAG, "setListener: view not a child")
    }

    /**
     * Remove listener for a child view
     *
     * @param view child view of this layout
     */
    fun removeListener(view: View) {
        mListeners.remove(view)
    }

    interface OnTouchOutsideListener {
        fun onTouchOutside(view: View)
    }
}