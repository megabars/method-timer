package com.github.methodtimer.plugin.execution

import com.github.methodtimer.plugin.codevision.MethodTimingCodeVisionProvider
import com.github.methodtimer.plugin.services.MethodTimingStorage
import com.github.methodtimer.plugin.services.TimingResultsReader
import com.github.methodtimer.plugin.services.TimingRunTracker
import com.intellij.codeInsight.codeVision.CodeVisionHost
import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class TimingProcessListener : ExecutionListener, Disposable {

    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "method-timer-poller").apply { isDaemon = true }
    }

    init {
        Disposer.register(ApplicationManager.getApplication(), this)
    }

    override fun dispose() {
        scheduler.shutdownNow()
    }

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        val project = env.project
        val runProfileName = env.runProfile.name
        val outputPath = TimingRunTracker.getInstance(project).getOutputPath(runProfileName) ?: return

        LOG.info("[MethodTimer] Starting periodic polling for: $runProfileName")

        // Каждые 3 секунды читаем файл и обновляем Code Vision
        val task: ScheduledFuture<*> = scheduler.scheduleAtFixedRate({
            try {
                readAndUpdateTimings(project, outputPath, deleteFile = false)
            } catch (e: Exception) {
                LOG.warn("[MethodTimer] Polling error", e)
            }
        }, 5, 3, TimeUnit.SECONDS)

        // Останавливаем polling когда процесс завершится
        handler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                task.cancel(false)
                LOG.info("[MethodTimer] Process terminated: $runProfileName")
                // Финальное чтение с удалением файла через scheduler (без блокировки пула IDE)
                scheduler.schedule({
                    readAndUpdateTimings(project, outputPath, deleteFile = true)
                    TimingRunTracker.getInstance(project).consumeOutputPath(runProfileName)
                }, 1, TimeUnit.SECONDS)
            }
        })
    }

    private fun readAndUpdateTimings(project: Project, outputPath: String, deleteFile: Boolean) {
        if (project.isDisposed) return
        val path = Paths.get(outputPath)
        if (!Files.exists(path)) return

        val results = TimingResultsReader.readResults(path)
        if (results.isEmpty()) return

        LOG.debug("[MethodTimer] Read ${results.size} timing entries")

        val storage = MethodTimingStorage.getInstance(project)
        storage.updateTimings(results)

        if (deleteFile) {
            try { Files.deleteIfExists(path) } catch (_: Exception) {}
        }

        // Принудительно инвалидируем Code Vision
        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                val codeVisionHost = project.service<CodeVisionHost>()
                codeVisionHost.invalidateProvider(
                    CodeVisionHost.LensInvalidateSignal(null, listOf(MethodTimingCodeVisionProvider.ID))
                )
            }
        }
    }

    companion object {
        private val LOG = Logger.getInstance(TimingProcessListener::class.java)
    }
}
