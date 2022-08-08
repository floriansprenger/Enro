package dev.enro.core.fragment.container

import android.os.Bundle
import androidx.compose.material.ExperimentalMaterialApi
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import dagger.hilt.internal.GeneratedComponentManagerHolder
import dev.enro.core.*
import dev.enro.core.compose.ComposableNavigator
import dev.enro.core.compose.dialog.*
import dev.enro.core.container.asPresentInstruction
import dev.enro.core.fragment.FragmentNavigator
import dev.enro.core.hosts.*
import dev.enro.core.hosts.OpenComposableDialogInFragment
import dev.enro.core.hosts.OpenComposableDialogInHiltFragment
import dev.enro.core.hosts.OpenComposableInFragment
import dev.enro.core.hosts.OpenComposableInHiltFragment

internal object FragmentFactory {

    private val generatedComponentManagerHolderClass = kotlin.runCatching {
        GeneratedComponentManagerHolder::class.java
    }.getOrNull()

    @OptIn(ExperimentalMaterialApi::class)
    fun createFragment(
        parentContext: NavigationContext<*>,
        navigator: Navigator<*, *>,
        instruction: AnyOpenInstruction
    ): Fragment {
        val isHiltContext = if(generatedComponentManagerHolderClass != null) {
            parentContext.contextReference is GeneratedComponentManagerHolder
        } else false

        val fragmentManager = when(parentContext.contextReference) {
            is FragmentActivity -> parentContext.contextReference.supportFragmentManager
            is Fragment -> parentContext.contextReference.childFragmentManager
            else -> throw IllegalStateException()
        }

        when (navigator) {
            is FragmentNavigator<*, *> -> {
                val isPresentation = instruction.navigationDirection is NavigationDirection.Present
                val isDialog = DialogFragment::class.java.isAssignableFrom(navigator.contextType.java)

                val fragment =  if(isPresentation && !isDialog) {
                    val wrappedKey = when {
                        isHiltContext -> OpenPresentableFragmentInHiltFragment(instruction.asPresentInstruction())
                        else ->  OpenPresentableFragmentInFragment(instruction.asPresentInstruction())
                    }
                    createFragment(
                        parentContext = parentContext,
                        navigator = parentContext.controller.navigatorForKeyType(wrappedKey::class) as Navigator<*, *>,
                        instruction = NavigationInstruction.Open.OpenInternal(
                            instructionId = instruction.instructionId,
                            navigationDirection = instruction.navigationDirection,
                            navigationKey = wrappedKey
                        )
                    )
                }
                else {
                    fragmentManager.fragmentFactory.instantiate(
                        navigator.contextType.java.classLoader!!,
                        navigator.contextType.java.name
                    ).apply {
                        arguments = Bundle().addOpenInstruction(instruction)
                    }
                }

                return fragment
            }
            is ComposableNavigator<*, *> -> {

                val isDialog = DialogDestination::class.java.isAssignableFrom(navigator.contextType.java)
                        || BottomSheetDestination::class.java.isAssignableFrom(navigator.contextType.java)

                val wrappedKey = when {
                    isDialog -> when {
                        isHiltContext -> OpenComposableDialogInHiltFragment(instruction.asPresentInstruction())
                        else -> OpenComposableDialogInFragment(instruction.asPresentInstruction())
                    }
                    else -> when {
                        isHiltContext -> OpenComposableInHiltFragment(instruction, isRoot = false)
                        else -> OpenComposableInFragment(instruction, isRoot = false)
                    }
                }

                return createFragment(
                    parentContext = parentContext,
                    navigator = parentContext.controller.navigatorForKeyType(wrappedKey::class) as Navigator<*, *>,
                    instruction = NavigationInstruction.Open.OpenInternal(
                        instructionId = instruction.instructionId,
                        navigationDirection = instruction.navigationDirection,
                        navigationKey = wrappedKey
                    )
                )
            }
            else -> throw IllegalStateException()
        }
    }
}