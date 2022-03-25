package dev.enro.core.container

import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.IllegalStateException

class NavigationContainerManager {
    private val restoredContainerStates = mutableMapOf<String, NavigationContainerBackstack>()
    private var restoredActiveContainer: String? = null

    private val _containers: MutableSet<NavigationContainer> = mutableSetOf()
    val containers: Set<NavigationContainer> = _containers

    internal val activeContainerState: MutableStateFlow<NavigationContainer?> =
        MutableStateFlow(null)
    val activeContainer: NavigationContainer? get() = activeContainerState.value

    internal fun setActiveContainerById(id: String?) {
        activeContainerState.value = containers.firstOrNull { it.id == id }
    }

    internal fun addContainer(container: NavigationContainer) {
        _containers.add(container)
        restore(container)
    }

    internal fun removeContainer(container: NavigationContainer) {
        _containers.remove(container)
    }

    internal fun save(outState: Bundle) {
        containers.forEach {
            outState.putParcelableArrayList(
                "$BACKSTACK_KEY@${it.id}", ArrayList(it.backstackFlow.value.backstackEntries)
            )
        }
        outState.putStringArrayList(CONTAINER_IDS_KEY, ArrayList(containers.map { it.id }))
        outState.putString(ACTIVE_CONTAINER_KEY, activeContainer?.id)
    }

    internal fun restore(savedInstanceState: Bundle?) {
        if(savedInstanceState == null) return

        savedInstanceState.getStringArrayList(CONTAINER_IDS_KEY)
            .orEmpty()
            .forEach {
                restoredContainerStates[it] = createRestoredBackStack(
                    savedInstanceState
                        .getParcelableArrayList<NavigationContainerBackstackEntry>("$BACKSTACK_KEY@$it")
                        .orEmpty()
                )
            }

        restoredActiveContainer = savedInstanceState.getString(ACTIVE_CONTAINER_KEY)
        containers.forEach { restore(it) }
    }

    internal fun restore(container: NavigationContainer) {
        val activeContainer = activeContainer
        val backstack = restoredContainerStates[container.id] ?: return
        container.setBackstack(backstack)
        if(restoredActiveContainer == container.id) {
            setActiveContainer(container)
            restoredActiveContainer = null
        }
        else {
            // TODO this is required because setBackstack sets the active container. Need to fix that...
            setActiveContainer(activeContainer)
        }
    }

    fun setActiveContainer(containerController: NavigationContainer?) {
        if(containerController == null) {
            activeContainerState.value = null
            return
        }
        val selectedContainer = containers.firstOrNull { it.id == containerController.id }
            ?: throw IllegalStateException("NavigationContainer with id ${containerController.id} is not registered with this NavigationContainerManager")
        activeContainerState.value = selectedContainer
    }

    companion object {
        const val ACTIVE_CONTAINER_KEY = "dev.enro.core.container.NavigationContainerManager.ACTIVE_CONTAINER_KEY"
        const val CONTAINER_IDS_KEY = "dev.enro.core.container.NavigationContainerManager.CONTAINER_IDS_KEY"
        const val BACKSTACK_KEY = "dev.enro.core.container.NavigationContainerManager.BACKSTACK_KEY"
    }
}