package nav.enro.core.context

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import nav.enro.core.NavigationKey
import nav.enro.core.controller.NavigationController
import nav.enro.core.controller.navigationController
import nav.enro.core.navigator.ActivityNavigator
import nav.enro.core.navigator.FragmentNavigator
import nav.enro.core.navigator.Navigator

data class ChildContainer(
    @IdRes val containerId: Int,
    val accept: (NavigationKey) -> Boolean
)

sealed class NavigationContext<ContextType : Any>(
    val contextReference: ContextType
) {
    abstract val controller: NavigationController
    abstract val lifecycle: Lifecycle
    abstract val childFragmentManager: FragmentManager

    internal open val navigator: Navigator<ContextType, *>? by lazy {
        controller.navigatorForContextType(contextReference::class) as? Navigator<ContextType, *>
    }
}

internal class ActivityContext<ContextType : FragmentActivity>(
    contextReference: ContextType,
) : NavigationContext<ContextType>(contextReference) {
    override val controller get() = contextReference.application.navigationController
    override val lifecycle get() = contextReference.lifecycle
    override val navigator get() = super.navigator as? ActivityNavigator<ContextType, *>
    override val childFragmentManager get() = contextReference.supportFragmentManager
}

internal class FragmentContext<ContextType : Fragment>(
    contextReference: ContextType,
) : NavigationContext<ContextType>(contextReference) {
    override val controller get() = contextReference.requireActivity().application.navigationController
    override val lifecycle get() = contextReference.lifecycle
    override val navigator get() = super.navigator as? FragmentNavigator<ContextType, *>
    override val childFragmentManager get() = contextReference.childFragmentManager
}


