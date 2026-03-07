package com.github.methodtimer.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class TimingRunTracker {

    private val outputPaths = ConcurrentHashMap<String, String>()

    fun registerRun(runProfileName: String, outputPath: String) {
        outputPaths[runProfileName] = outputPath
    }

    fun getOutputPath(runProfileName: String): String? {
        return outputPaths[runProfileName]
    }

    fun consumeOutputPath(runProfileName: String): String? {
        return outputPaths.remove(runProfileName)
    }

    companion object {
        fun getInstance(project: Project): TimingRunTracker =
            project.getService(TimingRunTracker::class.java)
    }
}
