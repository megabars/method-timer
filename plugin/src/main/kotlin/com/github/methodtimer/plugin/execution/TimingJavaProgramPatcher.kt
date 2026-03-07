package com.github.methodtimer.plugin.execution

import com.github.methodtimer.plugin.services.TimingRunTracker
import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.io.FileUtil
import java.io.File

class TimingJavaProgramPatcher : JavaProgramPatcher() {

    private val LOG = Logger.getInstance(TimingJavaProgramPatcher::class.java)

    override fun patchJavaParameters(executor: Executor, configuration: RunProfile, javaParameters: JavaParameters) {
        if (configuration !is ModuleBasedConfiguration<*, *>) return
        val agentJar = resolveAgentJar()
        if (agentJar == null) {
            LOG.warn("[MethodTimer] Agent JAR not found")
            return
        }
        val outputFile = FileUtil.createTempFile("method-timing-", ".jsonl", true)

        javaParameters.vmParametersList.add("-javaagent:${agentJar.absolutePath}=${outputFile.absolutePath}")

        TimingRunTracker.getInstance(configuration.project).registerRun(configuration.name, outputFile.absolutePath)
        LOG.info("[MethodTimer] Injected agent for '${configuration.name}', output: ${outputFile.absolutePath}")
    }

    private fun resolveAgentJar(): File? {
        val plugin = PluginManagerCore.getPlugin(PluginId.getId("com.github.methodtimer")) ?: return null
        val agentDir = plugin.pluginPath.resolve("agent")
        return agentDir.toFile().listFiles()?.firstOrNull { it.name.endsWith("-all.jar") }
    }
}
