package com.lemnik.anrtrace

import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag

data class ANRBreadcrumbOptions @JvmOverloads constructor(
    val samplingStarted: Boolean = false,
    val samplingStopped: Boolean = false,
) {
    @JvmName("-onSamplerStarted")
    internal fun onSamplerStarted() {
        if (samplingStarted) {
            Bugsnag.leaveBreadcrumb("ANR sampling started", emptyMap(), BreadcrumbType.STATE)
        }
    }

    @JvmName("-onSamplerStopped")
    internal fun onSamplerStopped() {
        if (samplingStopped) {
            Bugsnag.leaveBreadcrumb(
                "ANR sampling stopped / Looper recovered",
                emptyMap(),
                BreadcrumbType.STATE
            )
        }
    }
}