package com.github.methodtimer.plugin.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TimeFormatterTest {

    // ── наносекунды ───────────────────────────────────────────────────────────

    @Test
    fun `zero nanoseconds`() = assertEquals("0 ns", TimeFormatter.format(0L))

    @Test
    fun `small nanoseconds`() = assertEquals("500 ns", TimeFormatter.format(500L))

    @Test
    fun `boundary 999 ns`() = assertEquals("999 ns", TimeFormatter.format(999L))

    // ── микросекунды ──────────────────────────────────────────────────────────

    @Test
    fun `boundary 1000 ns = 1 us`() = assertEquals("1 \u03BCs", TimeFormatter.format(1_000L))

    @Test
    fun `15000 ns = 15 us`() = assertEquals("15 \u03BCs", TimeFormatter.format(15_000L))

    @Test
    fun `boundary 999999 ns`() = assertEquals("999 \u03BCs", TimeFormatter.format(999_999L))

    // ── миллисекунды ──────────────────────────────────────────────────────────

    @Test
    fun `boundary 1000000 ns = 1 ms`() = assertEquals("1.0 ms", TimeFormatter.format(1_000_000L))

    @Test
    fun `1500000 ns = 1_5 ms`() = assertEquals("1.5 ms", TimeFormatter.format(1_500_000L))

    @Test
    fun `boundary 999999999 ns`() = assertEquals("1000.0 ms", TimeFormatter.format(999_999_999L))

    // ── секунды ───────────────────────────────────────────────────────────────

    @Test
    fun `boundary 1000000000 ns = 1 s`() = assertEquals("1.00 s", TimeFormatter.format(1_000_000_000L))

    @Test
    fun `2500000000 ns = 2_5 s`() = assertEquals("2.50 s", TimeFormatter.format(2_500_000_000L))

    @Test
    fun `large value 60 s`() = assertEquals("60.00 s", TimeFormatter.format(60_000_000_000L))
}
