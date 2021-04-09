package telenav.demo.app.widgets

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import telenav.demo.app.R

@SuppressLint("ClickableViewAccessibility")
class SearchButton : LinearLayout, View.OnTouchListener, View.OnClickListener,
    Animator.AnimatorListener {

    companion object {
        private const val ANIM_DURATION: Long = 2000
        private const val ANIM_SCALE_VALUE = 0.2f
        const val BUTTON_HOLDING_THRESHOLD = 750
    }

    private var onClickListener: OnClickListener? = null
    private var onTouchListener: OnTouchListener? = null
    private var keepScaleUp = false
    private var scaleUp = true
    private var blockedAnimation = false
    private var actionDownStart: Long = 0
    private var onHoldNotified = false
    private val syncObj = Object()
    private var onHoldListener: OnHoldListener? = null


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        inflate(context, R.layout.search_button, this)
        animateButton()

        super.setOnTouchListener(this)
        super.setOnClickListener(this)
    }

    private fun animateButton() {
        scaleUp()
    }

    override fun onClick(view: View?) {
        if (onHoldNotified) {
            onHoldListener?.onHoldEnded()
        } else {
            onClickListener?.onClick(view)
        }
    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> actionDown()
            MotionEvent.ACTION_UP -> actionUp()
        }

        notifyOnHoldIfNeeded()
        return onTouchListener?.onTouch(view, event) ?: false
    }

    private fun actionDown() {
        keepScaleUp(true)
        onHoldNotified = false
        actionDownStart = System.currentTimeMillis()
    }

    private fun actionUp() {
        keepScaleUp(false)
    }

    private fun notifyOnHoldIfNeeded() {
        if (System.currentTimeMillis() - actionDownStart > BUTTON_HOLDING_THRESHOLD && !onHoldNotified) {
            onHoldNotified = true
            onHoldListener?.onHoldStarted()
        }
    }

    override fun onAnimationRepeat(p0: Animator?) = Unit
    override fun onAnimationCancel(p0: Animator?) = Unit
    override fun onAnimationStart(p0: Animator?) = Unit

    override fun onAnimationEnd(p0: Animator?) {
        if (scaleUp) {
            scaleUp()
        } else {
            scaleDownIfAllowed()
        }
    }

    private fun scaleUp() {
        val scaleUpAnimator =
            animate().scaleXBy(ANIM_SCALE_VALUE).scaleYBy(ANIM_SCALE_VALUE).apply {
                duration = ANIM_DURATION
            }
        scaleUpAnimator.setListener(this)

        scaleUp = false
        scaleUpAnimator.start()
    }

    private fun scaleDown() {
        val scaleDownAnimator =
            animate().scaleXBy(-ANIM_SCALE_VALUE).scaleYBy(-ANIM_SCALE_VALUE).apply {
                duration = ANIM_DURATION
            }
        scaleDownAnimator.setListener(this)

        scaleUp = true
        scaleDownAnimator.start()
    }

    private fun scaleDownIfAllowed() {
        synchronized(syncObj) {
            if (keepScaleUp)
                blockedAnimation = true
            else
                scaleDown()
        }
    }

    private fun keepScaleUp(keep: Boolean) {
        synchronized(syncObj) {
            keepScaleUp = keep
            if (!keepScaleUp && blockedAnimation) {
                blockedAnimation = false
                scaleDown()
            }
        }
    }

    override fun setOnTouchListener(onTouchListener: OnTouchListener?) {
        this.onTouchListener = onTouchListener
    }

    override fun setOnClickListener(onClickListener: OnClickListener?) {
        this.onClickListener = onClickListener
    }

    fun setOnHoldListener(onHoldListener: OnHoldListener?) {
        this.onHoldListener = onHoldListener
    }

    interface OnHoldListener {
        fun onHoldStarted()
        fun onHoldEnded()
    }
}