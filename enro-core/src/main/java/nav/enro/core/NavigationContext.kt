package nav.enro.core

import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import nav.enro.core.activity.ActivityNavigator
import nav.enro.core.controller.NavigationController
import nav.enro.core.fragment.FragmentNavigator
import nav.enro.core.internal.handle.NavigationHandleViewModel

sealed class NavigationContext<ContextType : Any>(
    val contextReference: ContextType
) {
    abstract val controller: NavigationController
    abstract val lifecycle: Lifecycle
    abstract val childFragmentManager: FragmentManager

    internal open val navigator: Navigator<*, ContextType>? by lazy {
        controller.navigatorForContextType(contextReference::class) as? Navigator<*, ContextType>
    }
}

internal class ActivityContext<ContextType : FragmentActivity>(
    contextReference: ContextType,
) : NavigationContext<ContextType>(contextReference) {
    override val controller get() = contextReference.application.navigationController
    override val lifecycle get() = contextReference.lifecycle
    override val navigator get() = super.navigator as? ActivityNavigator<*, ContextType>
    override val childFragmentManager get() = contextReference.supportFragmentManager
}

internal class FragmentContext<ContextType : Fragment>(
    contextReference: ContextType,
) : NavigationContext<ContextType>(contextReference) {
    override val controller get() = contextReference.requireActivity().application.navigationController
    override val lifecycle get() = contextReference.lifecycle
    override val navigator get() = super.navigator as? FragmentNavigator<*, ContextType>
    override val childFragmentManager get() = contextReference.childFragmentManager
}

val NavigationContext<out Fragment>.fragment get() = contextReference

val NavigationContext<*>.activity: FragmentActivity
    get() = when (contextReference) {
        is FragmentActivity -> contextReference
        is Fragment -> contextReference.requireActivity()
        else -> throw IllegalStateException()
    }

@Suppress("UNCHECKED_CAST") // Higher level logic dictates this cast will pass
internal val <T : FragmentActivity> T.navigationContext: ActivityContext<T>
    get() = viewModels<NavigationHandleViewModel> { ViewModelProvider.NewInstanceFactory() } .value.navigationContext as ActivityContext<T>

@Suppress("UNCHECKED_CAST") // Higher level logic dictates this cast will pass
internal val <T : Fragment> T.navigationContext: FragmentContext<T>
    get() = viewModels<NavigationHandleViewModel> { ViewModelProvider.NewInstanceFactory() } .value.navigationContext as FragmentContext<T>

fun NavigationContext<*>.rootContext(): NavigationContext<*> {
    var parent = this
    while (true) {
        val currentContext = parent
        parent = parent.parentContext() ?: return currentContext
    }
}

fun NavigationContext<*>.parentContext(): NavigationContext<*>? {
    return when (this) {
        is ActivityContext -> null
        is FragmentContext<out Fragment> ->
            when (val parentFragment = fragment.parentFragment) {
                null -> fragment.requireActivity().navigationContext
                else -> parentFragment.navigationContext
            }
    }
}

fun NavigationContext<*>.leafContext(): NavigationContext<*> {
    val primaryNavigationFragment = childFragmentManager.primaryNavigationFragment ?: return this
    primaryNavigationFragment.view ?: return this
    val childContext = primaryNavigationFragment.navigationContext
    return childContext.leafContext()
}

internal fun NavigationContext<*>.getNavigationHandleViewModel(): NavigationHandleViewModel {
    return when (this) {
        is FragmentContext<out Fragment> -> fragment.getNavigationHandle()
        is ActivityContext<out FragmentActivity> -> activity.getNavigationHandle()
    } as NavigationHandleViewModel
}
