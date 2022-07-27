package com.lemnik.anrtrace

import android.os.Looper
import androidx.annotation.IntRange
import com.bugsnag.android.Client
import com.bugsnag.android.Event
import com.bugsnag.android.OnErrorCallback
import com.bugsnag.android.Plugin

/**
 * Use this in your Bugsnag [Configuration](com.bugsnag.android.Configuration) to enable performance
 * reporting on Application Not Responding (ANR) reports.
 */
class ANRTracePlugin @JvmOverloads constructor(
    /**
     * Number of milliseconds the main thread can be blocked before we start performance monitoring.
     * Higher values will reduce the additional load imposed by sampling when the system is
     * operating well, but will also reduce the usefulness of the reports when an ANR is reported.
     */
    @IntRange(from = 1L, to = 5000L)
    private val startSamplingDelay: Long = 1000L,

    /**
     * The interval between each stack sample taken when profiling is active. Lower values
     * will improve the accuracy of the reports, but increase the load on the system when sampling
     * is active.
     *
     * Higher numbers reduce the load when sampling is active but reduce the accuracy of the sampling.
     * Due to how sampling works (by taking repeated snapshots of a threads stack-trace) larger
     * delays means more data that won't be "seen" by the sampler.
     */
    @IntRange(from = 1L, to = 5000L)
    private val sampleInterval: Long = 100L,

    /**
     * The [thread priority](Thread#priority) for the performance monitoring and sampling thread.
     * By default this is slightly lower than the [normal](Thread.NORM_PRIORITY) thread priority,
     * which reduces the impact that the sampler will have on the application when it's sampling.
     */
    private val samplerThreadPriority: Int = Thread.NORM_PRIORITY - 1,

    /**
     * The [ANRTracePlugin] can produce breadcrumbs when the monitored `Looper` appears blocked
     * and becomes unblocked again. Which breadcrumbs are logged can be changed using this option,
     * the default is to log no breadcrumbs.
     */
    private val breadcrumbs: ANRBreadcrumbOptions = ANRBreadcrumbOptions(),

    /**
     * The [StackTreeVisitor] that will handle the performance tree when an ANR is reported. This
     * can be altered to change the representation of the stack sampler data to suit your app.
     */
    private val stackTreeVisitor: StackTreeVisitor<*> = MetadataStackTreeVisitor(),
) : Plugin, OnErrorCallback {
    private var client: Client? = null
    private var slowLooperMonitor: SlowLooperMonitorThread? = null

    override fun load(client: Client) {
        this.client = client.also {
            it.addOnError(this)
        }

        this.slowLooperMonitor = SlowLooperMonitorThread(
            Looper.getMainLooper(),
            startSamplingDelay,
            sampleInterval,
            breadcrumbs,
            samplerThreadPriority,
        )
    }

    override fun unload() {
        client?.removeOnError(this)
        client = null

        slowLooperMonitor?.terminate()
        slowLooperMonitor = null
    }

    override fun onError(event: Event): Boolean {
        if (event.errors.any { it.errorClass.contains("ANR") }) {
            val samples = slowLooperMonitor?.sampleGraphRoot
            if (samples != null) {
                encodeStackTree(event, samples, stackTreeVisitor)
            } else {
                event.addMetadata(
                    "ANR",
                    "sampler",
                    "no sample data taken"
                )
            }
        }

        return true
    }

    private fun <E> encodeStackTree(
        event: Event,
        samples: SampleTreeNode,
        stackTreeVisitor: StackTreeVisitor<E>,
    ) {
        val root = stackTreeVisitor.begin()
        samples.accept(stackTreeVisitor, root)

        // force the ANR section to be created on the Event
        event.addMetadata("ANR", "#sentinelValue", "")

        // populate the ANR section
        val anrMetadata = event.getMetadata("ANR") as MutableMap<String, Any>
        stackTreeVisitor.end(anrMetadata, root)

        // remove our sentinel value
        anrMetadata.remove("#sentinelValue")
    }
}