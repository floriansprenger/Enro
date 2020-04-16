package nav.enro.core.internal.context

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import nav.enro.core.NavigationKey

class FragmentHostDefinition(
    private val containerView: Int,
    private val acceptFunction: (navigationKey: NavigationKey) -> Boolean
) {
    fun accepts(navigationKey: NavigationKey) = acceptFunction(navigationKey)

    internal fun createFragmentHost(fragment: Fragment) =
        FragmentHost(containerView, fragment.childFragmentManager)

    internal fun createFragmentHost(activity: FragmentActivity) =
        FragmentHost(containerView, activity.supportFragmentManager)

}

internal class FragmentHost(
    internal val containerView: Int,
    internal val fragmentManager: FragmentManager
)