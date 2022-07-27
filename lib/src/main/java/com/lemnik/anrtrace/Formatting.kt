package com.lemnik.anrtrace

internal fun StringBuilder.humanFormatTimeNs(timeNs: Long) {
    if (timeNs > 1000000) {
        // 12ms
        append(timeNs / 1000000).append('m')
    } else if (timeNs > 100000) {
        // .9ms instead of full ns detail
        append('.').append(timeNs / 100000).append('m')
    } else if (timeNs > 10000) {
        // .09ms instead of full ns detail
        append('.').append('0').append(timeNs / 10000).append('m')
    } else {
        append(timeNs).append('n')
    }

    append('s')
}