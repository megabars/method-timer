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
        @Volatile var lastRunTimestamp: Long = 0L
    }

    @Volatile private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        // XmlSerializer десериализует Map как LinkedHashMap — восстанавливаем потокобезопасность
        state.timings = ConcurrentHashMap(state.timings)
        myState = state
    }

    /** Возвращает true если хотя бы одно значение изменилось. */
    fun updateTimings(newTimings: Map<String, Long>): Boolean {
        var changed = false
        for ((fqn, ns) in newTimings) {
            if (myState.timings.put(fqn, ns) != ns) changed = true
        }
        if (changed) myState.lastRunTimestamp = System.currentTimeMillis()
        return changed
    }

    fun getTimingForMethod(fqn: String): Long? = myState.timings[fqn]

    companion object {
        fun getInstance(project: Project): MethodTimingStorage =
            project.getService(MethodTimingStorage::class.java)
    }
}
