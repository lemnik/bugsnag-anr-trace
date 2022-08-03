package com.lemnik.anrtrace

import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag

data class ANRBreadcrumbOptions @JvmOverloads constructor(
    val samplingStarted: Boolean = true,
    val samplingStopped: Boolean = true,

    /**
     * Optionally a [StackTreeVisitor] to add metadata to the [samplingStopped] breadcrumb. If this
     * is not `null` then it will be called with the full stack tree when the monitored `Looper`
     * is considered "recovered". This can be useful for tracking down parts of your application
     * that come close to ANR errors without actually hitting them (making them ideal targets
     * for optimisation).
     */
    val stackTreeVisitor: StackTreeVisitor<*>? = HotStackTreeVisitor(ClassNameFormatter.ShortPackageName),
) {
    @JvmName("-onSamplerStarted")
    internal fun onSamplerStarted() {
        if (samplingStarted && Bugsnag.isStarted()) {
            Bugsnag.leaveBreadcrumb("ANR sampling started", emptyMap(), BreadcrumbType.STATE)
        }
    }

    @JvmName("-onSamplerStopped")
    internal fun onSamplerStopped(sampleTreeRoot: SampleTreeNode) {
        if (samplingStopped && Bugsnag.isStarted()) {
            Bugsnag.leaveBreadcrumb(
                "ANR sampling stopped / Looper recovered",
                stackTreeVisitor?.let { createStackTreeMetadata(sampleTreeRoot, it) } ?: emptyMap(),
                BreadcrumbType.STATE
            )
        }
    }

    private fun <E> createStackTreeMetadata(
        sampleTreeRoot: SampleTreeNode,
        visitor: StackTreeVisitor<E>
    ): Map<String, Any> {
        val metadata = HashMap<String, Any>()
        val root = visitor.begin()
        sampleTreeRoot.single().accept(visitor, root)

        visitor.end(root, metadata::put)

        return metadata
    }

    companion object {
        /**
         * Convenience `ANRBreadcrumbOptions` which disables all of the ANR related breadcrumbs.
         */
        @JvmStatic
        val DISABLED = ANRBreadcrumbOptions(samplingStarted = false, samplingStopped = false)
    }
}