package dev.enro.core.compose.destination

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

internal class ComposableDestinationSavedStateRegistryOwner(
    private val owner: ComposableDestinationOwner
) : SavedStateRegistryOwner {

    private val savedStateController = SavedStateRegistryController.create(this)
    internal val savedState: Bundle =
        owner.parentSavedStateRegistry.consumeRestoredStateForKey(owner.instruction.instructionId) ?: Bundle()

    init {
        savedStateController.performRestore(savedState)
        if (owner.parentSavedStateRegistry.getSavedStateProvider(owner.instruction.instructionId) != null) {
            throw IllegalStateException(owner.parentContainer.backstack.backstack.toString())
        }
        owner.parentSavedStateRegistry.registerSavedStateProvider(owner.instruction.instructionId) {
            val outState = Bundle()
            owner.navigationController.onComposeContextSaved(
                owner.destination,
                outState
            )
            savedStateController.performSave(outState)
            outState
        }

        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if(event != Lifecycle.Event.ON_DESTROY) return
                owner.parentSavedStateRegistry.unregisterSavedStateProvider(owner.instruction.instructionId)
            }
        })
    }

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override fun getLifecycle(): Lifecycle {
        return owner.lifecycle
    }
}