package dev.enro.core.fragment.container

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import dev.enro.core.*
import dev.enro.core.container.*
import dev.enro.core.fragment.DefaultFragmentExecutor
import dev.enro.core.fragment.FragmentNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FragmentNavigationContainer internal constructor(
    @IdRes val containerId: Int,
    parentContext: NavigationContext<*>,
    accept: (NavigationKey) -> Boolean,
    emptyBehavior: EmptyBehavior,
    val fragmentManager: FragmentManager
) : NavigationContainer(
    id = containerId.toString(),
    parentContext = parentContext,
    accept = accept,
    emptyBehavior = emptyBehavior,
) {
    override val activeContext: NavigationContext<*>?
        get() = fragmentManager.findFragmentById(containerId)?.navigationContext

    override fun reconcileBackstack(
        removed: List<NavigationContainerBackstackEntry>,
        backstack: NavigationContainerBackstack
    ): Boolean {
        if(!tryExecutePendingTransitions()){
            return false
        }

        val toRemove = removed
            .mapNotNull {
                fragmentManager.findFragmentByTag(it.instruction.instructionId)
            }
        val toDetach = backstack.backstack.dropLast(1)
            .mapNotNull {
                fragmentManager.findFragmentByTag(it.instructionId)
            }
        val activeInstruction = backstack.backstack.lastOrNull()
        val activeFragment = activeInstruction?.let {
            fragmentManager.findFragmentByTag(it.instructionId)
        }
        val newFragment = if(activeFragment == null && activeInstruction != null) {
            DefaultFragmentExecutor.createFragment(
                fragmentManager,
                parentContext.controller.navigatorForKeyType(activeInstruction.navigationKey::class)!!,
                activeInstruction
            )
        } else null

        fragmentManager.commitNow {
            toRemove.forEach {
                remove(it)
            }
            toDetach.forEach {
                detach(it)
            }

            if(activeInstruction == null) return@commitNow

            if(activeFragment != null) {
                attach(activeFragment)
                setPrimaryNavigationFragment(activeFragment)
            }
            if(newFragment != null) {
                add(containerId, newFragment, activeInstruction.instructionId)
                setPrimaryNavigationFragment(newFragment)
            }
        }

        return true
    }

    private fun tryExecutePendingTransitions(): Boolean {
        return kotlin
            .runCatching {
                fragmentManager.executePendingTransactions()
                true
            }
            .getOrDefault(false)
    }

}
