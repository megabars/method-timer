package com.github.methodtimer.plugin.services

import com.intellij.openapi.diagnostic.Logger
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

object TimingResultsReader {

    private val LOG = Logger.getInstance(TimingResultsReader::class.java)

    // Соответствует формату {"fqn":"<escaped>","timeNs":<digits>} из TimingResultWriter.toJson()
    private val FQN_REGEX = Regex(""""fqn":"((?:[^"\\]|\\.)*)"""")
    private val TIME_NS_REGEX = Regex(""""timeNs":(\d+)""")

    fun readResults(filePath: Path): Map<String, Long> {
        val results = mutableMapOf<String, Long>()

        try {
            Files.newBufferedReader(filePath).use { reader ->
                reader.forEachLine { line ->
                    if (line.isBlank()) return@forEachLine
                    try {
                        val fqnRaw = FQN_REGEX.find(line)?.groupValues?.get(1)
                            ?: run { LOG.warn("No fqn in timing line: $line"); return@forEachLine }
                        val timeNs = TIME_NS_REGEX.find(line)?.groupValues?.get(1)?.toLongOrNull()
                            ?: run { LOG.warn("No timeNs in timing line: $line"); return@forEachLine }
                        results[unescapeJson(fqnRaw)] = timeNs
                    } catch (e: Exception) {
                        LOG.warn("Failed to parse timing line: $line", e)
                    }
                }
            }
        } catch (_: NoSuchFileException) {
            // Файл мог быть удалён между вызовом exists() и чтением (атомарный move агента)
        } catch (e: Exception) {
            LOG.error("Failed to read timing results from $filePath", e)
        }

        return results
    }

    /** Разворачивает JSON-escape-последовательности согласно RFC 8259. */
    private fun unescapeJson(s: String): String {
        if ('\\' !in s) return s
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c != '\\' || i + 1 >= s.length) {
                sb.append(c); i++; continue
            }
            when (s[i + 1]) {
                '"'  -> { sb.append('"');       i += 2 }
                '\\' -> { sb.append('\\');      i += 2 }
                '/'  -> { sb.append('/');       i += 2 }
                'n'  -> { sb.append('\n');      i += 2 }
                'r'  -> { sb.append('\r');      i += 2 }
                't'  -> { sb.append('\t');      i += 2 }
                'b'  -> { sb.append('\b');      i += 2 }
                'f'  -> { sb.append('\u000C');  i += 2 }
                'u'  -> {
                    if (i + 6 <= s.length) {
                        val hex = s.substring(i + 2, i + 6).toIntOrNull(16)
                        if (hex != null) { sb.append(hex.toChar()); i += 6; continue }
                    }
                    sb.append(c); i++
                }
                else -> { sb.append(c); i++ }
            }
        }
        return sb.toString()
    }
}
