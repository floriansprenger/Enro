package dev.enro.core.result.internal

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import dev.enro.core.EnroException
import dev.enro.core.NavigationHandle
import dev.enro.core.NavigationKey
import dev.enro.core.controller.usecase.createResultChannel
import dev.enro.core.getNavigationHandle
import dev.enro.core.result.NavigationResultChannel
import dev.enro.core.result.managedByLifecycle
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@PublishedApi
internal class LazyResultChannelProperty<Result: Any, Key: NavigationKey.WithResult<Result>>(
    owner: Any,
    resultType: KClass<Result>,
    onClosed: (Key) -> Unit = {},
    onResult: (Key, Result) -> Unit
) : ReadOnlyProperty<Any, NavigationResultChannel<Result, Key>> {

    private var resultChannel: NavigationResultChannel<Result, Key>? = null

    init {
        val handle = when (owner) {
            is ComponentActivity -> lazy { owner.getNavigationHandle() }
            is Fragment -> lazy { owner.getNavigationHandle() }
            is NavigationHandle -> lazy { owner as NavigationHandle }
            else -> throw EnroException.UnreachableState()
        }
        val lifecycleOwner = owner as LifecycleOwner
        val lifecycle = lifecycleOwner.lifecycle

        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event != Lifecycle.Event.ON_CREATE) return;
                resultChannel = handle.value.createResultChannel<Result, Key>(
                    resultType = resultType,
                    onClosed = onClosed,
                    onResult = onResult,
                ).managedByLifecycle(lifecycle)
            }
        })
    }

    override fun getValue(
        thisRef: Any,
        property: KProperty<*>
    ): NavigationResultChannel<Result, Key> = resultChannel ?: throw EnroException.ResultChannelIsNotInitialised(
        "LazyResultChannelProperty's EnroResultChannel is not initialised. Are you attempting to use the result channel before the result channel's lifecycle owner has entered the CREATED state?"
    )
}
