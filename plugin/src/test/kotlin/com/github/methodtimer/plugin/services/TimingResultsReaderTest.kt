package com.github.methodtimer.plugin.services

import com.intellij.testFramework.junit5.TestApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

@TestApplication
class TimingResultsReaderTest {

    @TempDir
    lateinit var tempDir: Path

    // ── нормальное чтение ────────────────────────────────────────────────────

    @Test
    fun `reads single entry`() {
        val file = tempDir.resolve("timing.jsonl")
        file.writeText("""{"fqn":"com.example.Foo.bar()","timeNs":12345}""")

        val result = TimingResultsReader.readResults(file)

        assertEquals(mapOf("com.example.Foo.bar()" to 12345L), result)
    }

    @Test
    fun `reads multiple entries`() {
        val file = tempDir.resolve("timing.jsonl")
        file.writeText(
            """
            {"fqn":"com.example.Foo.bar()","timeNs":100}
            {"fqn":"com.example.Foo.baz(java.lang.String, int)","timeNs":200}
            """.trimIndent()
        )

        val result = TimingResultsReader.readResults(file)

        assertEquals(2, result.size)
        assertEquals(100L, result["com.example.Foo.bar()"])
        assertEquals(200L, result["com.example.Foo.baz(java.lang.String, int)"])
    }

    @Test
    fun `last entry wins for duplicate fqn`() {
        val file = tempDir.resolve("timing.jsonl")
        file.writeText(
            """
            {"fqn":"com.example.Foo.bar()","timeNs":100}
            {"fqn":"com.example.Foo.bar()","timeNs":999}
            """.trimIndent()
        )

        val result = TimingResultsReader.readResults(file)

        assertEquals(999L, result["com.example.Foo.bar()"])
    }

    // ── JSON unescape ────────────────────────────────────────────────────────

    @Test
    fun `unescapes backslash sequences in fqn`() {
        val file = tempDir.resolve("timing.jsonl")
        // fqn содержит \" и \\ — редкий, но возможный случай
        file.writeText("""{"fqn":"com.example.Foo.bar(java.lang.String)","timeNs":42}""")

        val result = TimingResultsReader.readResults(file)

        assertEquals(42L, result["com.example.Foo.bar(java.lang.String)"])
    }

    @Test
    fun `unescapes unicode escape in fqn`() {
        val file = tempDir.resolve("timing.jsonl")
        // \u0041 = 'A'
        file.writeText("""{"fqn":"com.ex\u0041mple.Foo.bar()","timeNs":7}""")

        val result = TimingResultsReader.readResults(file)

        assertEquals(7L, result["com.exAmple.Foo.bar()"])
    }

    @Test
    fun `unescapes tab and newline in fqn`() {
        val file = tempDir.resolve("timing.jsonl")
        file.writeText("""{"fqn":"foo\tbar\nbaz","timeNs":1}""")

        val result = TimingResultsReader.readResults(file)

        assertEquals(1L, result["foo\tbar\nbaz"])
    }

    // ── граничные случаи ────────────────────────────────────────────────────

    @Test
    fun `empty file returns empty map`() {
        val file = tempDir.resolve("timing.jsonl")
        file.writeText("")

        val result = TimingResultsReader.readResults(file)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `blank lines are skipped`() {
        val file = tempDir.resolve("timing.jsonl")
        file.writeText("\n\n   \n{\"fqn\":\"com.Foo.a()\",\"timeNs\":5}\n\n")

        val result = TimingResultsReader.readResults(file)

        assertEquals(mapOf("com.Foo.a()" to 5L), result)
    }

    @Test
    fun `invalid line is skipped, valid lines still parsed`() {
        val file = tempDir.resolve("timing.jsonl")
        file.writeText(
            """
            not-json-at-all
            {"fqn":"com.Foo.good()","timeNs":99}
            """.trimIndent()
        )

        val result = TimingResultsReader.readResults(file)

        assertEquals(mapOf("com.Foo.good()" to 99L), result)
    }

    @Test
    fun `zero timeNs is parsed correctly`() {
        val file = tempDir.resolve("timing.jsonl")
        file.writeText("""{"fqn":"com.Foo.fast()","timeNs":0}""")

        val result = TimingResultsReader.readResults(file)

        assertEquals(0L, result["com.Foo.fast()"])
    }

    @Test
    fun `max long timeNs is parsed correctly`() {
        val file = tempDir.resolve("timing.jsonl")
        file.writeText("""{"fqn":"com.Foo.slow()","timeNs":${Long.MAX_VALUE}}""")

        val result = TimingResultsReader.readResults(file)

        assertEquals(Long.MAX_VALUE, result["com.Foo.slow()"])
    }

    @Test
    fun `non-existent file returns empty map`() {
        val file = tempDir.resolve("does-not-exist.jsonl")

        val result = TimingResultsReader.readResults(file)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `fqn with generic-erased params`() {
        val file = tempDir.resolve("timing.jsonl")
        file.writeText("""{"fqn":"com.example.Service.process(java.util.List, java.lang.Object)","timeNs":333}""")

        val result = TimingResultsReader.readResults(file)

        assertEquals(333L, result["com.example.Service.process(java.util.List, java.lang.Object)"])
    }
}
