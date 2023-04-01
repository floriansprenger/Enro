package dev.enro.core.result

import android.os.Bundle
import android.os.Parcelable
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.enro.core.*
import dev.enro.core.container.toBackstack
import dev.enro.core.result.internal.ResultChannelImpl
import dev.enro.extensions.getParcelableListCompat
import dev.enro.extensions.isSaveableInBundle
import dev.enro.viewmodel.getNavigationHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

@PublishedApi
internal class FlowResultManager(
    savedStateHandle: SavedStateHandle?
) {
    val results = mutableStateMapOf<String, CompletedFlowStep>().apply {
        savedStateHandle ?: return@apply
        val bundle = savedStateHandle.get<Bundle>(RESULTS_KEY) ?: return@apply
        val savedList = bundle.getParcelableListCompat<CompletedFlowStep>(RESULTS_KEY).orEmpty()
        val savedMap = savedList.associateBy { it.stepId }
        putAll(savedMap)
    }

    init {
        savedStateHandle?.setSavedStateProvider(RESULTS_KEY) {
            val saveable = results.values.filter { it.result.isSaveableInBundle() }
            bundleOf(RESULTS_KEY to ArrayList(saveable))
        }
    }

    companion object {
        private const val RESULTS_KEY = "FlowResultManager.RESULTS_KEY"
    }
}

@PublishedApi
@Parcelize
internal data class FlowStep(
    val stepId: String,
    val key: NavigationKey,
    val dependsOn: Long,
    val direction: NavigationDirection,
) : Parcelable {
    fun complete(result: Any): CompletedFlowStep {
        return CompletedFlowStep(
            stepId = stepId,
            result = result,
            dependsOn = dependsOn
        )
    }
}

@Parcelize
internal data class FlowStepKey(
    val stepId: String,
) :
    NavigationKey.SupportsPush.WithResult<Any>,
    NavigationKey.SupportsPresent.WithResult<Any>


@PublishedApi
@Parcelize
internal data class CompletedFlowStep(
    val stepId: String,
    val result: @RawValue Any,
    val dependsOn: Long,
) : Parcelable

@PublishedApi
internal fun List<Any>.contentHash(): Long = fold(0L) { result, it -> 31L * result + it.hashCode() }

public class NavigationFlowScope internal constructor(
    @PublishedApi
    internal val resultManager: FlowResultManager
) {
    @PublishedApi
    internal val steps: MutableList<FlowStep> = mutableListOf()

    public inline fun <reified T : Any> push(
        dependsOn: List<Any> = emptyList(),
        noinline key: () -> NavigationKey.SupportsPush.WithResult<T>,
    ): T {
        val baseId = key::class.java.name
        val count = steps.count { it.stepId.startsWith(baseId) }
        val step = FlowStep(
            stepId = "$baseId@$count",
            key = key(),
            dependsOn = dependsOn.contentHash(),
            direction = NavigationDirection.Push,
        )
        steps.add(step)

        val completedStep = resultManager.results[step.stepId]?.let {
            if (it.result !is T) {
                resultManager.results.remove(it.stepId)
                return@let null
            }
            if (it.dependsOn != step.dependsOn) {
                resultManager.results.remove(it.stepId)
                return@let null
            }
            it
        }
        return completedStep?.result as? T ?: throw NoResultForPush(step)
    }

    public inline fun <reified T : Any> present(
        dependsOn: List<Any> = emptyList(),
        noinline key: () -> NavigationKey.SupportsPresent.WithResult<T>,
    ): T {
        val baseId = key::class.java.name
        val count = steps.count { it.stepId.startsWith(baseId) }
        val step = FlowStep(
            stepId = "$baseId@$count",
            key = key(),
            dependsOn = dependsOn.contentHash(),
            direction = NavigationDirection.Present,
        )
        steps.add(step)

        val completedStep = resultManager.results[step.stepId]?.let {
            if (it.result !is T) {
                resultManager.results.remove(it.stepId)
                return@let null
            }
            if (it.dependsOn != step.dependsOn) {
                resultManager.results.remove(it.stepId)
                return@let null
            }
            it
        }
        return completedStep?.result as? T ?: throw NoResultForPresent(step)
    }

    public fun escape(): Nothing {
        throw Escape()
    }

    @PublishedApi
    internal class NoResultForPush(val step: FlowStep) : RuntimeException()

    @PublishedApi
    internal class NoResultForPresent(val step: FlowStep) : RuntimeException()

    @PublishedApi
    internal class Escape : RuntimeException()
}


internal fun interface CreateResultChannel {
    operator fun invoke(
        onClosed: (Any) -> Unit,
        onResult: (NavigationKey.WithResult<*>, Any) -> Unit
    ): EnroResultChannel<Any, NavigationKey.WithResult<Any>>
}

@AdvancedEnroApi
public class NavigationFlow<T> internal constructor(
    private val scope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle?,
    private val navigation: NavigationHandle,
    private val registerForNavigationResult: CreateResultChannel,
    private val flow: NavigationFlowScope.() -> T,
    private val onCompleted: (T) -> Unit,
) {
    private var steps: List<FlowStep> = savedStateHandle?.get<Bundle>(STEPS_KEY)
        ?.getParcelableListCompat<FlowStep>(STEPS_KEY)
        .orEmpty()

    private val resultManager = FlowResultManager(savedStateHandle)
    private val resultChannel = registerForNavigationResult(
        onClosed = { key ->
            if (key !is FlowStepKey) return@registerForNavigationResult
            if (key.stepId == steps.lastOrNull()?.stepId) {
                resultManager.results.remove(key.stepId)
                steps = steps.dropLast(1)
            }
        },
        onResult = { key, result ->
            if (key !is FlowStepKey) return@registerForNavigationResult
            val step = steps.first { it.stepId == key.stepId }
            resultManager.results[key.stepId] = step.complete(result)
            next()
        },
    )

    init {
        savedStateHandle?.setSavedStateProvider(STEPS_KEY) {
            bundleOf(STEPS_KEY to ArrayList(steps))
        }
    }

    public fun next() {
        val flowScope = NavigationFlowScope(resultManager)
        runCatching { onCompleted(flowScope.flow()) }
            .recover {
                when(it) {
                    is NavigationFlowScope.NoResultForPush -> {}
                    is NavigationFlowScope.NoResultForPresent -> {}
                    is NavigationFlowScope.Escape -> return
                    else -> throw it
                }
            }
            .getOrThrow()

        val resultChannelId = (resultChannel as ResultChannelImpl<*,*>).id
        val oldSteps = steps
        steps = flowScope.steps
        navigation.onActiveContainer {
            val existingInstructions = backstack
                .mapNotNull { instruction ->
                    val flowKey = instruction.internal.resultKey as? FlowStepKey ?: return@mapNotNull null
                    val step = steps.firstOrNull { it.stepId == flowKey.stepId } ?: return@mapNotNull null
                    step to instruction
                }
                .groupBy { it.first.stepId }
                .mapValues { it.value.lastOrNull() }

            val instructions = steps.map { step ->
                val existingStep = existingInstructions[step.stepId]?.second?.takeIf {
                    oldSteps
                        .firstOrNull { it.stepId == step.stepId }
                        ?.dependsOn == step.dependsOn
                }
                existingStep ?: NavigationInstruction.Open.OpenInternal(
                    navigationDirection = step.direction,
                    navigationKey = step.key,
                    resultKey = FlowStepKey(step.stepId),
                    resultId = resultChannelId,
                    additionalData = mutableMapOf(
                        IS_PUSHED_IN_FLOW to (step.direction is NavigationDirection.Push)
                    )
                )
            }
            val finalInstructions = instructions
                .filter { it.navigationDirection == NavigationDirection.Push }
                .plus(
                    instructions.lastOrNull().takeIf { it?.navigationDirection == NavigationDirection.Present }
                )
                .filterNotNull()

            setBackstack(finalInstructions.toBackstack())
        }
    }

    internal companion object {
        const val IS_PUSHED_IN_FLOW = "NavigationFlow.IS_PUSHED_IN_FLOW"
        const val STEPS_KEY = "NavigationFlow.STEPS_KEY"
    }
}

public fun <T> ViewModel.registerForFlowResult(
    savedStateHandle: SavedStateHandle?,
    flow: NavigationFlowScope.() -> T,
    onCompleted: (T) -> Unit,
): PropertyDelegateProvider<ViewModel, ReadOnlyProperty<ViewModel, NavigationFlow<T>>> {
    return PropertyDelegateProvider { thisRef, property ->
        val flow = NavigationFlow(
            scope = viewModelScope,
            savedStateHandle = savedStateHandle,
            navigation = getNavigationHandle(),
            registerForNavigationResult = { onClosed, onResult ->
                registerForNavigationResultWithKey(
                    onClosed = onClosed,
                    onResult = onResult,
                ).getValue(thisRef, property)
            },
            flow = flow,
            onCompleted = onCompleted,
        )

        ReadOnlyProperty { _, _ -> flow }
    }
}