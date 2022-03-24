package dev.enro.core.result.internal

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import dev.enro.core.NavigationHandle
import dev.enro.core.getNavigationHandle
import dev.enro.core.result.EnroResultChannel
import dev.enro.core.result.managedByLifecycle
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@PublishedApi
internal class LazyResultChannelProperty<T>(
    owner: Any,
    resultType: Class<T>,
    onResult: (T) -> Unit
) : ReadOnlyProperty<Any, EnroResultChannel<T>> {

    private var resultChannel: EnroResultChannel<T>? = null

    init {
        val handle = when (owner) {
            is ComponentActivity -> lazy { owner.getNavigationHandle() }
            is Fragment -> lazy { owner.getNavigationHandle() }
            is NavigationHandle -> lazy { owner as NavigationHandle }
            else -> throw IllegalArgumentException("Owner must be a Fragment, ComponentActivity, or NavigationHandle")
        }
        val lifecycleOwner = owner as LifecycleOwner
        val lifecycle = lifecycleOwner.lifecycle

        lifecycle.coroutineScope.launchWhenCreated {
            resultChannel = ResultChannelImpl(
                navigationHandle = handle.value,
                resultType = resultType,
                onResult = onResult
            ).managedByLifecycle(lifecycle)
        }
    }

    override fun getValue(
        thisRef: Any,
        property: KProperty<*>
    ): EnroResultChannel<T> = resultChannel!!
}
