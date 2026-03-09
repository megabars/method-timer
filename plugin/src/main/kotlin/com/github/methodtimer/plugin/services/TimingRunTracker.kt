package com.github.methodtimer.plugin.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

@Service(Service.Level.PROJECT)
class TimingRunTracker : Disposable {

    private val outputPaths = ConcurrentHashMap<String, ConcurrentLinkedDeque<String>>()
    // Все зарегистрированные пути — для cleanup при закрытии проекта или крашe IDE
    private val allPaths = ConcurrentHashMap.newKeySet<String>()

    fun registerRun(runProfileName: String, outputPath: String) {
        allPaths.add(outputPath)
        outputPaths.computeIfAbsent(runProfileName) { ConcurrentLinkedDeque() }.addLast(outputPath)
    }

    fun consumeOutputPath(runProfileName: String): String? {
        val deque = outputPaths[runProfileName] ?: return null
        val path = deque.pollFirst()
        if (deque.isEmpty()) outputPaths.remove(runProfileName)
        return path
    }

    /** Вызывается когда файл уже удалён слушателем — убираем из tracking-set */
    fun releasePath(path: String) {
        allPaths.remove(path)
    }

    override fun dispose() {
        // Удаляем все temp-файлы, которые не были убраны слушателем (крашe IDE, закрытие проекта при живом процессе)
        for (path in allPaths) {
            try { Files.deleteIfExists(Paths.get(path)) } catch (_: Exception) {}
            try { Files.deleteIfExists(Paths.get("$path.tmp")) } catch (_: Exception) {}
        }
        allPaths.clear()
        outputPaths.clear()
    }

    companion object {
        fun getInstance(project: Project): TimingRunTracker =
            project.getService(TimingRunTracker::class.java)
    }
}
