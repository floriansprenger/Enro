package dev.enro.core.container

import dev.enro.core.NavigationDirection
import dev.enro.core.NavigationInstruction
import dev.enro.core.OpenForwardInstruction

fun createEmptyBackStack() = NavigationContainerBackstack(
    lastInstruction = NavigationInstruction.Close,
    backstack = listOf(),
    exiting = null,
    exitingIndex = -1,
    isDirectUpdate = true
)

fun createRestoredBackStack(backstack: List<OpenForwardInstruction>) = NavigationContainerBackstack(
    backstack = backstack,
    exiting = null,
    exitingIndex = -1,
    lastInstruction = backstack.lastOrNull() ?: NavigationInstruction.Close,
    isDirectUpdate = true
)

data class NavigationContainerBackstack(
    val lastInstruction: NavigationInstruction,
    val backstack: List<OpenForwardInstruction>,
    val exiting: OpenForwardInstruction?,
    val exitingIndex: Int,
    val isDirectUpdate: Boolean
) {
    val visible: OpenForwardInstruction? = backstack.lastOrNull()
    val renderable: List<OpenForwardInstruction> = run {
        if (exiting == null) return@run backstack
        if (backstack.contains(exiting)) return@run backstack
        if (exitingIndex > backstack.lastIndex) return@run backstack + exiting
        return@run backstack.flatMapIndexed { index, open ->
            if (exitingIndex == index) return@flatMapIndexed listOf(exiting, open)
            return@flatMapIndexed listOf(open)
        }
    }

    internal fun push(
        instruction: OpenForwardInstruction
    ): NavigationContainerBackstack {
        return copy(
            backstack = backstack + instruction,
            exiting = visible,
            exitingIndex = backstack.lastIndex,
            lastInstruction = instruction,
            isDirectUpdate = false
        )
    }

    internal fun close(): NavigationContainerBackstack {
        return copy(
            backstack = backstack.dropLast(1),
            exiting = visible,
            exitingIndex = backstack.lastIndex,
            lastInstruction = NavigationInstruction.Close,
            isDirectUpdate = false
        )
    }

    internal fun close(id: String): NavigationContainerBackstack {
        val index = backstack.indexOfLast {
            it.instructionId == id
        }
        if (index < 0) return this
        val exiting = backstack[index]
        return copy(
            backstack = backstack.minus(exiting),
            exiting = exiting,
            exitingIndex = index,
            lastInstruction = NavigationInstruction.Close,
            isDirectUpdate = false
        )
    }
}