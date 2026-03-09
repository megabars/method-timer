package com.github.methodtimer.plugin.services

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

@State(
    name = "MethodTimingStorage",
    storages = [Storage("methodTimingData.xml")]
)
@Service(Service.Level.PROJECT)
class MethodTimingStorage : PersistentStateComponent<MethodTimingStorage.State> {

    class State {
        var timings: MutableMap<String, Long> = ConcurrentHashMap()
        var lastRunTimestamp: Long = 0L
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        // XmlSerializer десериализует Map как LinkedHashMap — восстанавливаем потокобезопасность
        state.timings = ConcurrentHashMap(state.timings)
        myState = state
    }

    fun updateTimings(newTimings: Map<String, Long>) {
        myState.timings.putAll(newTimings)
        myState.lastRunTimestamp = System.currentTimeMillis()
    }

    fun getTimingForMethod(fqn: String): Long? = myState.timings[fqn]

    companion object {
        fun getInstance(project: Project): MethodTimingStorage =
            project.getService(MethodTimingStorage::class.java)
    }
}
