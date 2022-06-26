package dev.enro.core.compose

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import dagger.hilt.android.internal.lifecycle.HiltViewModelFactory
import dagger.hilt.internal.GeneratedComponentManagerHolder
import dev.enro.core.*
import dev.enro.core.compose.animation.EnroAnimatedVisibility
import dev.enro.core.compose.container.ComposableNavigationContainer
import dev.enro.core.compose.dialog.BottomSheetDestination
import dev.enro.core.compose.dialog.DialogDestination
import dev.enro.core.compose.dialog.EnroBottomSheetContainer
import dev.enro.core.compose.dialog.EnroDialogContainer
import dev.enro.core.container.NavigationContainerBackstack
import dev.enro.core.internal.handle.getNavigationHandleViewModel
import dev.enro.viewmodel.EnroViewModelFactory


internal class ComposableDestinationContextReference(
    val instruction: AnyOpenInstruction,
    val destination: ComposableDestination,
    internal var parentContainer: ComposableNavigationContainer
) : ViewModel(),
    LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner {

    private val navigationController get() = parentContainer.parentContext.controller
    private val parentViewModelStoreOwner get() = parentContainer.parentContext.viewModelStoreOwner
    private val parentSavedStateRegistry get() = parentContainer.parentContext.savedStateRegistryOwner.savedStateRegistry
    internal val activity: ComponentActivity get() = parentContainer.parentContext.activity

    private val arguments by lazy { Bundle().addOpenInstruction(instruction) }
    private val savedState: Bundle? =
        parentSavedStateRegistry.consumeRestoredStateForKey(instruction.instructionId)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val viewModelStore: ViewModelStore = ViewModelStore()

    @SuppressLint("StaticFieldLeak")
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    private var defaultViewModelFactory: ViewModelProvider.Factory

    init {
        destination.contextReference = this

        savedStateController.performRestore(savedState)
        parentSavedStateRegistry.registerSavedStateProvider(instruction.instructionId) {
            val outState = Bundle()
            navigationController.onComposeContextSaved(
                destination,
                outState
            )
            savedStateController.performSave(outState)
            outState
        }
        navigationController.onComposeDestinationAttached(
            destination,
            savedState
        )

        defaultViewModelFactory = run {
            val generatedComponentManagerHolderClass = kotlin.runCatching {
                GeneratedComponentManagerHolder::class.java
            }.getOrNull()

            val factory = if (generatedComponentManagerHolderClass != null && activity is GeneratedComponentManagerHolder) {
                HiltViewModelFactory.createInternal(
                    activity,
                    this,
                    arguments,
                    SavedStateViewModelFactory(activity.application, this, savedState)
                )
            } else {
                SavedStateViewModelFactory(activity.application, this, savedState)
            }

            return@run EnroViewModelFactory(
                getNavigationHandleViewModel(),
                factory
            )
        }

        lifecycleRegistry.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_DESTROY -> {
                        parentSavedStateRegistry.unregisterSavedStateProvider(instruction.instructionId)
                        viewModelStore.clear()
                        lifecycleRegistry.removeObserver(this)
                    }
                    else -> {
                    }
                }
            }
        })
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun getLifecycle(): LifecycleRegistry {
        return lifecycleRegistry
    }

    override fun getViewModelStore(): ViewModelStore {
        return viewModelStore
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return defaultViewModelFactory
    }

    override val savedStateRegistry: SavedStateRegistry get() =
        savedStateController.savedStateRegistry

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun Render() {
        val backstackState by parentContainer.backstackFlow.collectAsState()
        if (!lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) return
        val saveableStateHolder = rememberSaveableStateHolder()

        val navigationHandle = remember { getNavigationHandleViewModel() }

        val isVisible = instruction == backstackState.visible
        val animations = remember(isVisible) {
            if (backstackState.isDirectUpdate) return@remember DefaultAnimations.none
            animationsFor(
                navigationHandle.navigationContext ?: return@remember DefaultAnimations.none,
                backstackState.lastInstruction
            )
        }

        EnroAnimatedVisibility(
            visible = isVisible,
            animations = animations
        ) {
            CompositionLocalProvider(
                LocalLifecycleOwner provides this,
                LocalViewModelStoreOwner provides this,
                LocalSavedStateRegistryOwner provides this,
                LocalNavigationHandle provides navigationHandle
            ) {
                saveableStateHolder.SaveableStateProvider(key = instruction.instructionId) {
                    navigationController.composeEnvironmentContainer.Render {
                        destination.Render()
                    }
                }
            }

            DisposableEffect(true) {
                onDispose {
                    parentContainer.onInstructionDisposed(instruction)
                }
            }
        }
    }
}

internal fun getComposableDestinationContext(
    instruction: AnyOpenInstruction,
    destination: ComposableDestination,
    parentContainer: ComposableNavigationContainer
): ComposableDestinationContextReference {
    return ComposableDestinationContextReference(
        instruction = instruction,
        destination = destination,
        parentContainer = parentContainer
    )
}

abstract class ComposableDestination: LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    HasDefaultViewModelProviderFactory {
    internal lateinit var contextReference: ComposableDestinationContextReference

    override val savedStateRegistry: SavedStateRegistry
        get() = contextReference.savedStateRegistry

    override fun getLifecycle(): Lifecycle {
        return contextReference.lifecycle
    }

    override fun getViewModelStore(): ViewModelStore {
        return contextReference.viewModelStore
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return contextReference.defaultViewModelProviderFactory
    }

    @Composable
    abstract fun Render()
}