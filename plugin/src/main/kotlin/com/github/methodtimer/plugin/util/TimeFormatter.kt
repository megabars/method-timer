package com.github.methodtimer.plugin.util

import java.util.Locale

object TimeFormatter {

    fun format(nanos: Long): String = when {
        nanos < 1_000L -> "$nanos ns"
        nanos < 1_000_000L -> "${nanos / 1_000} \u03BCs"
        nanos < 999_950_000L -> "%.1f ms".format(Locale.ROOT, nanos / 1_000_000.0)
        else -> "%.2f s".format(Locale.ROOT, nanos / 1_000_000_000.0)
    }
}
