package com.github.methodtimer.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

@Service(Service.Level.PROJECT)
class TimingRunTracker {

    private val outputPaths = ConcurrentHashMap<String, ConcurrentLinkedDeque<String>>()

    fun registerRun(runProfileName: String, outputPath: String) {
        outputPaths.computeIfAbsent(runProfileName) { ConcurrentLinkedDeque() }.addLast(outputPath)
    }

    fun consumeOutputPath(runProfileName: String): String? {
        val deque = outputPaths[runProfileName] ?: return null
        val path = deque.pollFirst()
        if (deque.isEmpty()) outputPaths.remove(runProfileName)
        return path
    }

    companion object {
        fun getInstance(project: Project): TimingRunTracker =
            project.getService(TimingRunTracker::class.java)
    }
}
