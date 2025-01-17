package dev.enro.core.result.internal

import androidx.compose.runtime.DisallowComposableCalls
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.enro.core.*
import dev.enro.core.result.EnroResult
import dev.enro.core.result.UnmanagedNavigationResultChannel

private class ResultChannelProperties<Result : Any, Key : NavigationKey.WithResult<Result>>(
    val navigationHandle: NavigationHandle,
    val resultType: Class<Result>,
    val onClosed: (Key) -> Unit,
    val onResult: (Key, Result) -> Unit,
)

internal class ResultChannelImpl<Result: Any, Key : NavigationKey.WithResult<Result>> @PublishedApi internal constructor(
    private val enroResult: EnroResult,
    navigationHandle: NavigationHandle,
    resultType: Class<Result>,
    onClosed: @DisallowComposableCalls (Key) -> Unit,
    onResult: @DisallowComposableCalls (Key, Result) -> Unit,
    additionalResultId: String = "",
) : UnmanagedNavigationResultChannel<Result, Key> {

    /**
     * The arguments passed to the ResultChannelImpl hold references to the external world, and
     * can hold references to objects that could leak in memory. We store these properties inside
     * a variable which is cleared to null when the ResultChannelImpl is destroyed, to ensure
     * that these references are not held by the ResultChannelImpl after it has been destroyed.
     */
    private var arguments: ResultChannelProperties<Result, Key>? = ResultChannelProperties(
        navigationHandle = navigationHandle,
        resultType = resultType,
        onClosed = onClosed,
        onResult = onResult,
    )

    /**
     * The resultId being set here to the JVM class name of the onResult lambda is a key part of
     * being able to make result channels work without providing an explicit id. The JVM will treat
     * the lambda as an anonymous class, which is uniquely identifiable by it's class name.
     *
     * If the behaviour of the Kotlin/JVM interaction changes in a future release, it may be required
     * to pass an explicit resultId as a part of the ResultChannelImpl constructor, which would need
     * to be unique per result channel created.
     *
     * It is possible to have two result channels registered for the same result type:
     * <code>
     *     val resultOne = registerForResult<Boolean> { ... }
     *     val resultTwo = registerForResult<Boolean> { ... }
     *
     *     // ...
     *     resultTwo.open(SomeNavigationKey( ... ))
     * </code>
     *
     * It's important in this case that resultTwo can be identified as the channel to deliver the
     * result into, and this identification needs to be stable across application process death.
     * The simple solution would be to require users to provide a name for the channel:
     * <code>
     *     val resultTwo = registerForResult<Boolean>("resultTwo") { ... }
     * </code>
     *
     * but using the anonymous class name is a nicer way to do things for now, with the ability to
     * fall back to explicit identification of the channels in the case that the Kotlin/JVM behaviour
     * changes in the future.
     */
    internal val id = ResultChannelId(
        ownerId = navigationHandle.id,
        resultId = onResult::class.java.name +"@"+additionalResultId
    )

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        if(event == Lifecycle.Event.ON_DESTROY) {
            destroy()
        }
    }.apply { navigationHandle.lifecycle.addObserver(this) }

    override fun open(key: Key) {
        val properties = arguments ?: return
        properties.navigationHandle.executeInstruction(
            NavigationInstruction.Forward(key).internal.copy(
                resultId = id
            )
        )
    }

    override fun push(key: NavigationKey.SupportsPush.WithResult<out Result>) {
        val properties = arguments ?: return
        properties.navigationHandle.executeInstruction(
            NavigationInstruction.Push(key).internal.copy(
                resultId = id
            )
        )
    }

    override fun present(key: NavigationKey.SupportsPresent.WithResult<out Result>) {
        val properties = arguments ?: return
        properties.navigationHandle.executeInstruction(
            NavigationInstruction.Present(key).internal.copy(
                resultId = id
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    internal fun consumeResult(pendingResult: PendingResult) {
        val properties = arguments ?: return
        when(pendingResult) {
            is PendingResult.Closed -> {
                val key = pendingResult.navigationKey
                key as Key
                properties.navigationHandle.runWhenHandleActive {
                    properties.onClosed(key)
                }
            }
            is PendingResult.Result -> {
                val result = pendingResult.result
                val key = pendingResult.navigationKey
                if (!properties.resultType.isAssignableFrom(result::class.java))
                    throw EnroException.ReceivedIncorrectlyTypedResult("Attempted to consume result with wrong type!")
                result as Result
                key as Key
                properties.navigationHandle.runWhenHandleActive {
                    properties.onResult(key, result)
                }
            }
        }
    }

    override fun attach() {
        val properties = arguments ?: return
        if(properties.navigationHandle.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        enroResult.registerChannel(this)
    }

    override fun detach() {
        val properties = arguments ?: return
        enroResult.deregisterChannel(this)
    }

    override fun destroy() {
        val properties = arguments ?: return
        detach()
        properties.navigationHandle.lifecycle.removeObserver(lifecycleObserver)
        arguments = null
    }
}
