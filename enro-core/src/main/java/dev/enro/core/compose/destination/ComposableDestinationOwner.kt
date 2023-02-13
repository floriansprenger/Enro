package dev.enro.core.compose.destination

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.updateTransition
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import dev.enro.core.*
import dev.enro.core.compose.ComposableDestination
import dev.enro.core.compose.LocalNavigationHandle
import dev.enro.core.compose.dialog.BottomSheetDestination
import dev.enro.core.compose.dialog.DialogDestination
import dev.enro.core.compose.dialog.EnroBottomSheetContainer
import dev.enro.core.compose.dialog.EnroDialogContainer
import dev.enro.core.container.NavigationContainer
import dev.enro.core.controller.usecase.ComposeEnvironment
import dev.enro.core.controller.usecase.OnNavigationContextCreated
import dev.enro.core.controller.usecase.OnNavigationContextSaved

@Stable
internal class ComposableDestinationOwner(
    val parentContainer: NavigationContainer,
    val instruction: AnyOpenInstruction,
    val destination: ComposableDestination,
    onNavigationContextCreated: OnNavigationContextCreated,
    onNavigationContextSaved: OnNavigationContextSaved,
    private val composeEnvironment: ComposeEnvironment,
    viewModelStore: ViewModelStore,
) : ViewModel(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    HasDefaultViewModelProviderFactory {

    internal val transitionState = MutableTransitionState(false)

    @SuppressLint("StaticFieldLeak")
    @Suppress("LeakingThis")
    private val lifecycleRegistry = LifecycleRegistry(this)

    @Suppress("LeakingThis")
    private val savedStateRegistryOwner = ComposableDestinationSavedStateRegistryOwner(this, onNavigationContextSaved)

    @Suppress("LeakingThis")
    private val viewModelStoreOwner = ComposableDestinationViewModelStoreOwner(
        owner = this,
        savedState = savedStateRegistryOwner.savedState,
        viewModelStore = viewModelStore
    )

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryOwner.savedStateRegistry

    init {
        destination.owner = this
        onNavigationContextCreated(
            destination.context,
            savedStateRegistryOwner.savedState
        )
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override val lifecycle: Lifecycle get() {
        return lifecycleRegistry
    }

    override val viewModelStore: ViewModelStore get() {
        return viewModelStoreOwner.viewModelStore
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory get() {
        return viewModelStoreOwner.defaultViewModelProviderFactory
    }

    override val defaultViewModelCreationExtras: CreationExtras get() {
        return viewModelStoreOwner.defaultViewModelCreationExtras
    }

    internal fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    internal fun Render(backstackState: List<AnyOpenInstruction>) {
        val lifecycleState = rememberLifecycleState()
        if (!lifecycleState.isAtLeast(Lifecycle.State.CREATED)) return

        val saveableStateHolder = rememberSaveableStateHolder()

        val renderDestination = remember(instruction.instructionId) {
            movableContentOf {
                ProvideRenderingEnvironment(saveableStateHolder) {
                    when (destination) {
                        is DialogDestination -> EnroDialogContainer(destination, destination)
                        is BottomSheetDestination -> EnroBottomSheetContainer(destination, destination)
                        else -> destination.Render()
                    }
                }
            }
        }

        val animation = remember(transitionState.targetState) {
            when (destination) {
                is DialogDestination,
                is BottomSheetDestination -> {
                    NavigationAnimation.Composable(
                        forView = DefaultAnimations.ForView.none,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    )
                }
                else -> parentContainer.currentAnimations.asComposable()
            }
        }
        val transition = updateTransition(transitionState, "ComposableDestination Visibility")
        animation.content(transition) {
            renderDestination()
            RegisterComposableLifecycleState(backstackState)
        }
    }

    @Composable
    private fun RegisterComposableLifecycleState(
        backstackState: List<AnyOpenInstruction>,
    ) {
        DisposableEffect(backstackState) {
            val isActive = backstackState.lastOrNull() == instruction
            val isInBackstack = backstackState.contains(instruction)
            when {
                isActive -> lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                isInBackstack -> lifecycleRegistry.currentState = Lifecycle.State.STARTED
                else -> {}
            }

            onDispose {
                when {
                    isActive -> {}
                    isInBackstack -> lifecycleRegistry.currentState = Lifecycle.State.CREATED
                    else -> lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
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
            LocalNavigationHandle provides remember { getNavigationHandle() }
        ) {
            saveableStateHolder.SaveableStateProvider(key = instruction.instructionId) {
                composeEnvironment {
                    content()
                }
            }
        }
    }
}

internal val ComposableDestinationOwner.navigationController get() = parentContainer.parentContext.controller
internal val ComposableDestinationOwner.parentSavedStateRegistry get() = parentContainer.parentContext.savedStateRegistryOwner.savedStateRegistry
internal val ComposableDestinationOwner.activity: ComponentActivity get() = parentContainer.parentContext.activity

@Composable
internal fun LifecycleOwner.rememberLifecycleState() : Lifecycle.State {
    val activeState = remember(this, lifecycle.currentState) { mutableStateOf(lifecycle.currentState) }

    DisposableEffect(this, activeState) {
        val observer = LifecycleEventObserver { _, event ->
            activeState.value = event.targetState
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return activeState.value
}