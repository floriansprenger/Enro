package dev.enro.core.compose.destination

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import dev.enro.core.AnyOpenInstruction
import dev.enro.core.DefaultAnimations
import dev.enro.core.NavigationAnimation
import dev.enro.core.activity
import dev.enro.core.compose.ComposableDestination
import dev.enro.core.compose.LocalNavigationHandle
import dev.enro.core.container.NavigationBackstack
import dev.enro.core.container.NavigationContainer
import dev.enro.core.internal.handle.getNavigationHandleViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ComposableDestinationOwner(
    parentContainer: NavigationContainer,
    val instruction: AnyOpenInstruction,
    val destination: ComposableDestination
): ViewModel(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    HasDefaultViewModelProviderFactory {

    private val parentContainerState = mutableStateOf(parentContainer)
    internal var parentContainer: NavigationContainer
        get() {
            return parentContainerState.value
        }
        set(value) {
            parentContainerState.value = value
        }

    private val animationState = mutableStateOf(DefaultAnimations.none.asComposable())
    internal var animation: NavigationAnimation.Composable
        get() {
            return animationState.value
        }
        set(value) {
            animationState.value = value
        }

    private val transitionState = MutableTransitionState<Boolean>(false)

    @SuppressLint("StaticFieldLeak")
    @Suppress("LeakingThis")
    private val lifecycleRegistry = LifecycleRegistry(this)

    @Suppress("LeakingThis")
    private val savedStateRegistryOwner = ComposableDestinationSavedStateRegistryOwner(this)

    @Suppress("LeakingThis")
    private val viewModelStoreOwner = ComposableDestinationViewModelStoreOwner(this, savedStateRegistryOwner.savedState)

    private val lifecycleFlow = createLifecycleFlow()

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryOwner.savedStateRegistry

    init {
        destination.owner = this
        navigationController.onComposeDestinationAttached(
            destination,
            savedStateRegistryOwner.savedState
        )
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    override fun getViewModelStore(): ViewModelStore {
        return viewModelStoreOwner.viewModelStore
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return viewModelStoreOwner.defaultViewModelProviderFactory
    }

    override fun getDefaultViewModelCreationExtras(): CreationExtras {
        return viewModelStoreOwner.defaultViewModelCreationExtras
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    internal fun Render(backstackState: NavigationBackstack) {
        val lifecycleState by lifecycleFlow.collectAsState()
        if (!lifecycleState.isAtLeast(Lifecycle.State.CREATED)) return

        val saveableStateHolder = rememberSaveableStateHolder()


        val renderDestination = remember {
            movableContentOf {
                ProvideRenderingEnvironment(saveableStateHolder) {
                    destination.Render()
                    RegisterComposableLifecycleState(backstackState)
                }
            }
        }

        val isVisible = instruction == backstackState.active
        LaunchedEffect(isVisible) {
            while(!transitionState.isIdle) delay(8)
            transitionState.targetState = isVisible
        }

        animation.content(transitionState) {
            renderDestination()
            RegisterComposableLifecycleState(backstackState)
        }
    }

    @Composable
    private fun RegisterComposableLifecycleState(
        backstackState: NavigationBackstack
    ) {
        DisposableEffect(instruction == backstackState.active) {
            val isActive = backstackState.active == instruction
            val isStarted = lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)
            when {
                isActive -> lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                isStarted -> lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            }

            onDispose {
                if (!backstackState.backstack.contains(instruction)) {
                    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                }
            }
        }
    }

    @Composable
    private fun ProvideRenderingEnvironment(
        saveableStateHolder: SaveableStateHolder,
        content: @Composable () -> Unit,
    ) {
        CompositionLocalProvider(
            LocalLifecycleOwner provides this@ComposableDestinationOwner,
            LocalViewModelStoreOwner provides this@ComposableDestinationOwner,
            LocalSavedStateRegistryOwner provides this@ComposableDestinationOwner,
            LocalNavigationHandle provides remember { getNavigationHandleViewModel() }
        ) {
            saveableStateHolder.SaveableStateProvider(key = instruction.instructionId) {
                navigationController.composeEnvironmentContainer.Render {
                    content()
                }
            }
        }
    }
}

internal val ComposableDestinationOwner.navigationController get() = parentContainer.parentContext.controller
internal val ComposableDestinationOwner.parentSavedStateRegistry get() = parentContainer.parentContext.savedStateRegistryOwner.savedStateRegistry
internal val ComposableDestinationOwner.activity: ComponentActivity get() = parentContainer.parentContext.activity

private fun LifecycleOwner.createLifecycleFlow(): StateFlow<Lifecycle.State> {
    val lifecycleFlow = MutableStateFlow(Lifecycle.State.INITIALIZED)
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            lifecycleFlow.value = source.lifecycle.currentState
        }
    })
    return lifecycleFlow
}

@Composable
fun rememberVisibleState(visible: Boolean): Boolean {
    val visibleState = rememberSaveable {
        mutableStateOf(!visible)
    }

    SideEffect {
        visibleState.value = visible
    }

    return visibleState.value
}