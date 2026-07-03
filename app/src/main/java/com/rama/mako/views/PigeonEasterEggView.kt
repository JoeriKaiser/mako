package com.rama.mako.views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.rama.mako.R
import kotlin.random.Random

/**
 * A subtle pigeon easter egg.
 *
 * A branch appears at a screen edge, a pigeon perches on it for a moment,
 * then lifts off and flies across the screen. The branch lingers briefly
 * before fading out.
 */
class PigeonEasterEggView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private enum class Direction {
        LEFT_TO_RIGHT, RIGHT_TO_LEFT
    }

    private val branchView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_XY
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.px_pigeon_branch))
    }

    private val birdView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_XY
    }

    private val perchedFrame = ContextCompat.getDrawable(context, R.drawable.px_pigeon_perched)
    private val flyingFrame1 = ContextCompat.getDrawable(context, R.drawable.px_pigeon_1)
    private val flyingFrame2 = ContextCompat.getDrawable(context, R.drawable.px_pigeon_2)

    private var isFlying = false
        private set
    private var pendingRunnable: Runnable? = null
    private var currentAnimator: AnimatorSet? = null

    /**
     * Public read-only flag indicating whether the pigeon sequence is currently active.
     */
    val isCurrentlyFlying: Boolean get() = isFlying

    init {
        isClickable = false
        isFocusable = false
        clipChildren = false
        clipToPadding = false
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        addView(branchView)
        addView(birdView)
        visibility = GONE
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pendingRunnable?.let { removeCallbacks(it) }
        pendingRunnable = null
        currentAnimator?.cancel()
        currentAnimator = null
        birdView.animate().cancel()
        branchView.animate().cancel()
        isFlying = false
        visibility = GONE
    }

    /**
     * Triggers the full pigeon easter egg sequence.
     */
    fun fly() {
        if (isFlying) return
        isFlying = true

        post {
            if (!isAttachedToWindow) {
                isFlying = false
                return@post
            }
            val parent = parent as? FrameLayout ?: run {
                isFlying = false
                return@post
            }
            val pigeonSize = resources.getDimensionPixelSize(R.dimen.pigeon_size)
            val branchWidth = pigeonSize * 2
            val direction = if (Random.nextBoolean()) Direction.LEFT_TO_RIGHT else Direction.RIGHT_TO_LEFT
            val perchY = Random.nextInt(pigeonSize, parent.height - pigeonSize)
            val perchDelayMs = Random.nextLong(1200, 2500)

            val branchLp = FrameLayout.LayoutParams(branchWidth, pigeonSize).apply {
                topMargin = perchY
                leftMargin = if (direction == Direction.LEFT_TO_RIGHT) {
                    -pigeonSize
                } else {
                    parent.width - pigeonSize
                }
            }
            branchView.layoutParams = branchLp
            branchView.alpha = 1f

            val birdLp = FrameLayout.LayoutParams(pigeonSize, pigeonSize).apply {
                topMargin = perchY + 1 // feet sit on the branch
                leftMargin = if (direction == Direction.LEFT_TO_RIGHT) {
                    0
                } else {
                    parent.width - pigeonSize
                }
            }
            birdView.layoutParams = birdLp
            birdView.scaleX = if (direction == Direction.LEFT_TO_RIGHT) 1f else -1f
            birdView.setImageDrawable(perchedFrame)
            birdView.translationX = 0f
            birdView.translationY = 0f

            visibility = VISIBLE

            Log.d(TAG, "perched y=$perchY direction=$direction delay=$perchDelayMs")

            val runnable = Runnable { takeOff(direction, parent, pigeonSize) }
            pendingRunnable = runnable
            postDelayed(runnable, perchDelayMs)
        }
    }

    private fun takeOff(direction: Direction, parent: FrameLayout, pigeonSize: Int) {
        // Small lift-off: bird dips, flaps once, and rises slightly.
        val liftDuration = 400L
        birdView.setImageDrawable(flyingFrame1)

        val dip = ObjectAnimator.ofFloat(birdView, "translationY", 0f, pigeonSize * 0.15f).apply {
            duration = liftDuration / 3
        }
        val rise = ObjectAnimator.ofFloat(birdView, "translationY", pigeonSize * 0.15f, -pigeonSize * 0.3f).apply {
            duration = liftDuration * 2 / 3
        }
        val liftSet = AnimatorSet().apply { playSequentially(dip, rise) }

        // One wing flap during lift-off.
        val flap = ValueAnimator.ofInt(0, 2).apply {
            duration = liftDuration
            addUpdateListener { animator ->
                val frame = (animator.animatedValue as Int) % 2
                birdView.setImageDrawable(if (frame == 0) flyingFrame1 else flyingFrame2)
            }
        }

        currentAnimator = AnimatorSet().apply {
            playTogether(liftSet, flap)
            doOnEnd { startFlight(direction, parent, pigeonSize) }
            start()
        }
    }

    private fun startFlight(direction: Direction, parent: FrameLayout, pigeonSize: Int) {
        val durationMs = Random.nextLong(5000, 7000)
        val baseAmplitude = resources.getDimensionPixelSize(R.dimen.pigeon_arc_amplitude).toFloat()
        val controlY1 = baseAmplitude * (Random.nextFloat() * 1.6f - 0.8f) // -80% to +80% of base
        val controlY2 = baseAmplitude * (Random.nextFloat() * 1.6f - 0.8f)
        val endY = baseAmplitude * (Random.nextFloat() * 2.4f - 1.2f) // -120% to +120% of base

        val distance = (parent.width + pigeonSize * 2).toFloat()
        val fromX = 0f
        val toX = if (direction == Direction.LEFT_TO_RIGHT) distance else -distance

        Log.d(TAG, "flight: direction=$direction duration=$durationMs endY=$endY")

        val flyX = ObjectAnimator.ofFloat(birdView, "translationX", fromX, toX).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
        }

        val arcAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                val oneMinusT = 1f - t
                // Cubic bezier for a smooth, non-repeating vertical arc.
                val y = (oneMinusT * oneMinusT * oneMinusT * 0f) +
                        (3f * oneMinusT * oneMinusT * t * controlY1) +
                        (3f * oneMinusT * t * t * controlY2) +
                        (t * t * t * endY)
                birdView.translationY = -pigeonSize * 0.3f + y
            }
        }

        val flapDuration = Random.nextLong(280, 360)
        val flapAnimator = ValueAnimator.ofInt(0, (durationMs / flapDuration).toInt()).apply {
            duration = durationMs
            addUpdateListener { animator ->
                val frame = (animator.animatedValue as Int) % 2
                birdView.setImageDrawable(if (frame == 0) flyingFrame1 else flyingFrame2)
            }
        }

        currentAnimator = AnimatorSet().apply {
            playTogether(flyX, arcAnimator, flapAnimator)
            doOnEnd { fadeBranch() }
            start()
        }
    }

    private fun fadeBranch() {
        currentAnimator = AnimatorSet().apply {
            play(ObjectAnimator.ofFloat(branchView, "alpha", 1f, 0f).apply { duration = 800 })
            doOnEnd { resetAfterFlight() }
            start()
        }
    }

    private fun resetAfterFlight() {
        visibility = GONE
        branchView.alpha = 1f
        birdView.translationX = 0f
        birdView.translationY = 0f
        isFlying = false
        currentAnimator = null
    }

    companion object {
        private const val TAG = "PigeonEasterEggView"
    }
}
