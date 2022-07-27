package com.lemnik.performanceexample

import android.os.SystemClock

class BusyClass {

    operator fun invoke(timeout: Long = Long.MAX_VALUE) = dispatchNext(1, timeout)

    fun plywood(depth: Int, timeout: Long) = dispatchNext(depth, timeout)
    fun activism(depth: Int, timeout: Long) = dispatchNext(depth, timeout)
    fun herbal(depth: Int, timeout: Long) = dispatchNext(depth, timeout)
    fun outcome(depth: Int, timeout: Long) = dispatchNext(depth, timeout)
    fun animosity(depth: Int, timeout: Long) = dispatchNext(depth, timeout)
    fun evade(depth: Int, timeout: Long) = dispatchNext(depth, timeout)
    fun spelling(depth: Int, timeout: Long) = dispatchNext(depth, timeout)
    fun juicy(depth: Int, timeout: Long) = dispatchNext(depth, timeout)
    fun wheat(depth: Int, timeout: Long) = dispatchNext(depth, timeout)
    fun itinerary(depth: Int, timeout: Long) = dispatchNext(depth, timeout)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun dispatchNext(depth: Int, timeout: Long): Int {
        Thread.sleep((1L..5L).random())
        if (depth >= 10) return 0

        var invokeCount = 0

        do {
            when ((0 until depth).random()) {
                0 -> plywood(depth + 1, timeout)
                1 -> activism(depth + 1, timeout)
                2 -> herbal(depth + 1, timeout)
                3 -> outcome(depth + 1, timeout)
                4 -> animosity(depth + 1, timeout)
                5 -> evade(depth + 1, timeout)
                6 -> spelling(depth + 1, timeout)
                7 -> juicy(depth + 1, timeout)
                8 -> wheat(depth + 1, timeout)
                9 -> itinerary(depth + 1, timeout)
            }

            invokeCount++
        } while (depth == 1 && SystemClock.elapsedRealtime() < timeout)

        return invokeCount
    }

}