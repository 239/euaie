package euaie

import java.util.concurrent.atomic.*
import kotlin.time.*

val time = TimeSource.Monotonic

class Task {
    val done = AtomicLong(0L)
    val goal = AtomicLong(0L)
    private val enabled: AtomicBoolean = AtomicBoolean(false)
    private val active: AtomicBoolean = AtomicBoolean(false)
    private var start = time.markNow()
    private var stop = time.markNow()

    fun enabled() = enabled.get()
    fun active() = active.get()

    fun progress(x: Double = 100.0) = if (goal.get() == 0L) 0.0 else done.get() * x / goal.get()
    fun textual() = if (goal.get() == 0L) "$done" else "$done / $goal (${progress().toInt()}%)"
    fun duration() = if (enabled.get()) time.markNow() - start else stop - start

    fun started() = enabled.get() && active.get()
    fun start(reset: Boolean = false) {
        enabled.set(true)
        active.set(true)
        if (reset) done.set(0L)
        if (reset) goal.set(0L)
        if (reset) start = time.markNow()
    }

    fun paused() = enabled.get() && !active.get()
    fun pause() {
        enabled.set(true)
        active.set(false)
    }

    fun canceled() = !enabled.get() && active.get()
    fun cancel() {
        enabled.set(false)
        active.set(true)
        stop = time.markNow()
    }

    fun finished() = !enabled.get() && !active.get()
    fun finish() {
        enabled.set(false)
        active.set(false)
        stop = time.markNow()
    }
}
