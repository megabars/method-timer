package com.github.methodtimer.plugin.util

object TimeFormatter {

    fun format(nanos: Long): String = when {
        nanos < 1_000L -> "$nanos ns"
        nanos < 1_000_000L -> "${nanos / 1_000} \u03BCs"
        nanos < 1_000_000_000L -> "%.1f ms".format(nanos / 1_000_000.0)
        else -> "%.2f s".format(nanos / 1_000_000_000.0)
    }
}
