package com.github.methodtimer.plugin.services

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TimingRunTrackerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `consumeOutputPath returns null for unknown run`() {
        val tracker = TimingRunTracker()
        assertNull(tracker.consumeOutputPath("unknown"))
    }

    @Test
    fun `registerRun and consumeOutputPath return registered path`() {
        val tracker = TimingRunTracker()
        tracker.registerRun("MyApp", "/tmp/output.jsonl")
        assertEquals("/tmp/output.jsonl", tracker.consumeOutputPath("MyApp"))
    }

    @Test
    fun `consumeOutputPath returns null after last path consumed`() {
        val tracker = TimingRunTracker()
        tracker.registerRun("MyApp", "/tmp/output.jsonl")
        tracker.consumeOutputPath("MyApp")
        assertNull(tracker.consumeOutputPath("MyApp"))
    }

    @Test
    fun `multiple runs for same config consumed in FIFO order`() {
        val tracker = TimingRunTracker()
        tracker.registerRun("MyApp", "/tmp/output1.jsonl")
        tracker.registerRun("MyApp", "/tmp/output2.jsonl")
        assertEquals("/tmp/output1.jsonl", tracker.consumeOutputPath("MyApp"))
        assertEquals("/tmp/output2.jsonl", tracker.consumeOutputPath("MyApp"))
    }

    @Test
    fun `different configs are tracked independently`() {
        val tracker = TimingRunTracker()
        tracker.registerRun("App1", "/tmp/a.jsonl")
        tracker.registerRun("App2", "/tmp/b.jsonl")
        assertEquals("/tmp/a.jsonl", tracker.consumeOutputPath("App1"))
        assertEquals("/tmp/b.jsonl", tracker.consumeOutputPath("App2"))
    }

    @Test
    fun `dispose deletes registered temp files`() {
        val tracker = TimingRunTracker()
        val file = tempDir.resolve("timing.jsonl")
        Files.writeString(file, "test")
        tracker.registerRun("MyApp", file.toString())

        tracker.dispose()

        assertFalse(Files.exists(file))
    }

    @Test
    fun `dispose deletes tmp sidecar files too`() {
        val tracker = TimingRunTracker()
        val file = tempDir.resolve("timing.jsonl")
        val tmpFile = tempDir.resolve("timing.jsonl.tmp")
        Files.writeString(file, "test")
        Files.writeString(tmpFile, "test")
        tracker.registerRun("MyApp", file.toString())

        tracker.dispose()

        assertFalse(Files.exists(file))
        assertFalse(Files.exists(tmpFile))
    }

    @Test
    fun `releasePath prevents dispose from deleting the file`() {
        val tracker = TimingRunTracker()
        val file = tempDir.resolve("timing.jsonl")
        Files.writeString(file, "test")
        tracker.registerRun("MyApp", file.toString())
        tracker.releasePath(file.toString())

        tracker.dispose()

        assertTrue(Files.exists(file))
    }

    @Test
    fun `dispose does not throw when file does not exist`() {
        val tracker = TimingRunTracker()
        tracker.registerRun("MyApp", "/nonexistent/path/timing.jsonl")
        assertDoesNotThrow { tracker.dispose() }
    }

    @Test
    fun `dispose clears all internal state`() {
        val tracker = TimingRunTracker()
        tracker.registerRun("MyApp", "/tmp/output.jsonl")
        tracker.dispose()
        // После dispose consumeOutputPath не должен возвращать пути
        assertNull(tracker.consumeOutputPath("MyApp"))
    }
}
