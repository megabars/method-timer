package com.github.methodtimer.plugin.services

import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

object TimingResultsReader {

    private val LOG = Logger.getInstance(TimingResultsReader::class.java)

    fun readResults(filePath: Path): Map<String, Long> {
        if (!Files.exists(filePath)) {
            return emptyMap()
        }

        val results = mutableMapOf<String, Long>()

        try {
            Files.newBufferedReader(filePath).use { reader ->
                reader.forEachLine { line ->
                    if (line.isBlank()) return@forEachLine
                    try {
                        val json = JsonParser.parseString(line).asJsonObject
                        val fqn = json.get("fqn").asString
                        val timeNs = json.get("timeNs").asLong
                        results[fqn] = timeNs
                    } catch (e: Exception) {
                        LOG.warn("Failed to parse timing line: $line", e)
                    }
                }
            }
        } catch (_: NoSuchFileException) {
            // Файл мог быть удалён между проверкой exists() и чтением (атомарный move агента)
        } catch (e: Exception) {
            LOG.error("Failed to read timing results from $filePath", e)
        }

        return results
    }
}
