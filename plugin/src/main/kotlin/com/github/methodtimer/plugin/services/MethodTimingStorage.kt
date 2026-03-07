package com.github.methodtimer.plugin.services

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "MethodTimingStorage",
    storages = [Storage("methodTimingData.xml")]
)
@Service(Service.Level.PROJECT)
class MethodTimingStorage : PersistentStateComponent<MethodTimingStorage.State> {

    class State {
        var timings: MutableMap<String, Long> = mutableMapOf()
        var lastRunTimestamp: Long = 0L
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    fun updateTimings(newTimings: Map<String, Long>) {
        myState.timings.putAll(newTimings)
        myState.lastRunTimestamp = System.currentTimeMillis()
    }

    fun getTimingForMethod(fqn: String): Long? = myState.timings[fqn]

    fun getAllTimings(): Map<String, Long> = myState.timings.toMap()

    fun clearTimings() {
        myState.timings.clear()
        myState.lastRunTimestamp = 0L
    }

    companion object {
        fun getInstance(project: Project): MethodTimingStorage =
            project.getService(MethodTimingStorage::class.java)
    }
}
