package com.lemnik.anrtrace

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import androidx.annotation.IntDef
import kotlin.math.min

/**
 * The monitor thread is expected to send a PING message to the `Looper`, this is done from the
 * monitor in order to keep the cycle of PING->Reset Alarm->PING linear.
 */
private const val STATE_PING = 0

/**
 * The monitor thread is waiting for its "alarm", which indicates that the Looper is somehow
 * blocked. If the alarm trips, we move to STATE_SAMPLING.
 */
private const val STATE_WAITING_FOR_ALARM = 1
private const val STATE_SAMPLING = 3
private const val STATE_STOPPED = -1

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.LOCAL_VARIABLE
)
@Retention(AnnotationRetention.SOURCE)
@IntDef(STATE_PING, STATE_WAITING_FOR_ALARM, STATE_SAMPLING, STATE_STOPPED)
private annotation class MonitorThreadState

private const val MSG_PING = 1

/**
 * Watches a specific `Looper` for "slow" processing, used to trigger sampling on the `main` thread
 * when it looks like it might ANR.
 *
 * The basic formula of this thread is to watch the `Looper` logging and time every `Message`
 * being processed. When any message takes longer than the configured threshold, we start sampling
 * it's stack until either:
 *
 * - finished processing the message
 * - signals that it's idle
 * - the plugin is terminated
 * - the VM is terminated
 * - an Error is signaled
 *
 * Sampling of the `Looper` thread is done using [Thread.getStackTrace]
 * which suspends the other thread (typically `main`) in order to grab a
 * consistent view of it's stack. This suspend -> stack-unwind -> resume behaviour has a
 * performance hit on the thread being sampled, so we need to be careful to:
 *
 * - not sample too often and cause more problems
 * - sample often enough to produce useful flame-graphs
 */
internal class SlowLooperMonitorThread(
    private val looper: Looper,
    private val startSamplingDelay: Long,
    private val sampleInterval: Long,
    private val breadcrumbs: ANRBreadcrumbOptions,
    priority: Int,
) : Thread("ANRMonitorThread"), Handler.Callback {
    private val lock = Object()

    /*
     * Implementation notes:
     *
     * The "alarm" here is actually just a timed `wait()` on the `lock` object.
     */

    @Volatile
    private var root = SampleTreeNode("", "")

    private var previousSampleGraph: SampleTreeNode? = null

    private var lastMessageTime = 0L
    private var lastStackSampleTimeNs = 0L

    @MonitorThreadState
    private var state = 0

    private val handler = Handler(looper, this)

    private val pingDelayTime: Long = min(startSamplingDelay / 4L, 1L)

    internal val sampleGraphRoot: SampleTreeNode?
        get() = root.takeIf { it.isNotEmpty } ?: previousSampleGraph

    private val stateName: String
        get() = when (state) {
            STATE_PING -> "Ping"
            STATE_WAITING_FOR_ALARM -> "AlarmWait"
            STATE_SAMPLING -> "Sampling"
            STATE_STOPPED -> "Stopped"
            else -> "Unknown[$state]"
        }

    init {
        require(startSamplingDelay > 0) { "startSamplingDelay must be >0 but was: $startSamplingDelay" }
        require(sampleInterval > 0) { "sampleInterval must be >0 but was: $sampleInterval" }

        this.priority = priority
        this.isDaemon = true

        start()
    }

    override fun run() {
        pingLooper()

        while (state != STATE_STOPPED) {
            when (state) {
                STATE_PING -> {
                    state = STATE_WAITING_FOR_ALARM
                    clearSamplingGraph()
                    pingLooper()
                }
                STATE_WAITING_FOR_ALARM -> if (waitForSamplingAlarm()) {
                    startSampling()
                }
                STATE_SAMPLING -> recordStackSample()
            }

            if (state == STATE_SAMPLING) {
                try {
                    sleep(sampleInterval)
                } catch (interrupted: InterruptedException) {
                    // ignore these completely, the next loop will handle it
                }
            }
        }
    }

    private fun startSampling() {
        breadcrumbs.onSamplerStarted()

        // mark the last sample
        lastStackSampleTimeNs = SystemClock.elapsedRealtimeNanos()

        // the alarm has passed, so we want to start sampling
        state = STATE_SAMPLING

        // record our first sample here
        recordStackSample()
    }

    /**
     * Record a single sample of the Looper threads stack. This uses `Thread.stackTrace` to grab
     * a snapshot and then record it against other accumulated sample nodes.
     */
    private fun recordStackSample() {
        val timeSinceLastSample = SystemClock.elapsedRealtimeNanos() - lastStackSampleTimeNs
        lastStackSampleTimeNs = SystemClock.elapsedRealtimeNanos()

        var node = root // capture the root node before we capture the stack trace

        /*
         * Thread.getStackTrace is *extremely* expensive in the scheme of things since the thread
         * being checked must first be suspended, then the stack can be unwound, and only then
         * can the thread be resumed.
         *
         * The stack unwind itself does (at the time of writing) appear to include some level
         * of caching, but at the time of writing the String values returned don't appear to have
         * any level of reuse and each getStackTrace call appears to allocate a new set of strings
         * on top of it's stack-unwind.
         *
         * Ultimately it would be nice to have multiple pluggable sampling approaches here.
         */
        val stackSample = looper.thread.stackTrace

        // we read the stack *backwards* - bottom (root) to top (leaf)
        for (index in stackSample.indices.reversed()) {
            val element = stackSample[index]

            node = node.child(element.className, element.methodName)
            node.counter++
            node.totalTimeNs += timeSinceLastSample
        }
    }

    /**
     * Wait for the "start sampling" alarm to go off, and return `true` if sampling should begin.
     * This method is subject to spurious wake-ups and state changes, so it will return `false`
     * if sampling should not start.
     */
    private fun waitForSamplingAlarm(): Boolean {
        val alarmTimeout = timeToNextAlarm()
        if (alarmTimeout > 0) {
            synchronized(lock) {
                // re-check the state under lock
                if (state == STATE_WAITING_FOR_ALARM) {
                    lock.wait(alarmTimeout)
                    return state == STATE_WAITING_FOR_ALARM && timeToNextAlarm() > 0
                }
            }
        }

        // we shouldn't wait anymore: start sampling
        return state == STATE_WAITING_FOR_ALARM
    }

    private fun timeToNextAlarm() =
        startSamplingDelay - (SystemClock.elapsedRealtime() - lastMessageTime)

    override fun handleMessage(msg: Message): Boolean {
        if (state == STATE_STOPPED) {
            return true
        }

        when (msg.what) {
            MSG_PING -> handlePing()
            else -> return false
        }

        return true
    }

    private fun handlePing() {
        lastMessageTime = SystemClock.elapsedRealtime()
        sendState(STATE_PING)
    }

    private fun pingLooper() {
        handler.sendEmptyMessageDelayed(STATE_WAITING_FOR_ALARM, pingDelayTime)
    }

    private fun clearSamplingGraph() {
        // only replace the root sample node if it's got data, otherwise leave it alone
        if (root.isNotEmpty) {
            breadcrumbs.onSamplerStopped(root)
            // replace the "previous" with whatever we just finished recording
            previousSampleGraph = root

            root = SampleTreeNode("", "")
        }
    }

    // called from "other" threads to safely set the state of the SlowLooperMonitorThread thread
    private fun sendState(@MonitorThreadState newState: Int) {
        synchronized(lock) {
            // we only make a change if the state is going to change
            // otherwise we can avoid the locks and notify()
            if (this.state != newState) {
                this.state = newState
                lock.notify()
            }
        }
    }

    fun terminate() {
        sendState(STATE_STOPPED)
        handler.removeMessages(MSG_PING)
    }
}