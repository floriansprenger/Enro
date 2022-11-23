package dev.enro.core

import android.app.Activity
import androidx.fragment.app.Fragment
import dev.enro.core.activity.ActivityNavigationBinding
import dev.enro.core.activity.DefaultActivityExecutor
import dev.enro.core.compose.ComposableNavigationBinding
import dev.enro.core.compose.DefaultComposableExecutor
import dev.enro.core.fragment.DefaultFragmentExecutor
import dev.enro.core.fragment.FragmentNavigationBinding
import dev.enro.core.synthetic.DefaultSyntheticExecutor
import dev.enro.core.synthetic.SyntheticNavigationBinding
import kotlin.reflect.KClass

// This class is used primarily to simplify the lambda signature of NavigationExecutor.open
public class ExecutorArgs<FromContext : Any, OpensContext : Any, KeyType : NavigationKey>(
    public val fromContext: NavigationContext<out FromContext>,
    public val binding: NavigationBinding<out KeyType, out OpensContext>,
    public val key: KeyType,
    instruction: AnyOpenInstruction
) {
    public val instruction: AnyOpenInstruction = instruction.internal.copy(
        previouslyActiveContainer = fromContext.containerManager.activeContainer?.key
    )
}

public abstract class NavigationExecutor<FromContext : Any, OpensContext : Any, KeyType : NavigationKey>(
    public val fromType: KClass<FromContext>,
    public val opensType: KClass<OpensContext>,
    public val keyType: KClass<KeyType>
) {
    public open fun animation(instruction: AnyOpenInstruction): NavigationAnimation {
        return when (instruction.navigationDirection) {
            NavigationDirection.Push -> DefaultAnimations.push
            NavigationDirection.Present -> DefaultAnimations.present
            NavigationDirection.Forward -> DefaultAnimations.forward
            NavigationDirection.Replace -> DefaultAnimations.replace
            NavigationDirection.ReplaceRoot -> DefaultAnimations.replaceRoot
        }
    }

    public open fun closeAnimation(context: NavigationContext<out OpensContext>): NavigationAnimation {
        return DefaultAnimations.close
    }

    public open fun preOpened(
        context: NavigationContext<out FromContext>
    ) {
    }

    public abstract fun open(
        args: ExecutorArgs<out FromContext, out OpensContext, out KeyType>
    )

    public open fun postOpened(
        context: NavigationContext<out OpensContext>
    ) {
    }

    public open fun preClosed(
        context: NavigationContext<out OpensContext>
    ) {
    }

    public abstract fun close(
        context: NavigationContext<out OpensContext>
    )
}

public class NavigationExecutorBuilder<FromContext : Any, OpensContext : Any, KeyType : NavigationKey> @PublishedApi internal constructor(
    private val fromType: KClass<FromContext>,
    private val opensType: KClass<OpensContext>,
    private val keyType: KClass<KeyType>
) {

    private var animationFunc: ((instruction: AnyOpenInstruction) -> NavigationAnimation)? = null
    private var closeAnimationFunc: ((context: NavigationContext<out OpensContext>) -> NavigationAnimation)? =
        null
    private var preOpenedFunc: ((context: NavigationContext<out FromContext>) -> Unit)? = null
    private var openedFunc: ((args: ExecutorArgs<out FromContext, out OpensContext, out KeyType>) -> Unit)? =
        null
    private var postOpenedFunc: ((context: NavigationContext<out OpensContext>) -> Unit)? = null
    private var preClosedFunc: ((context: NavigationContext<out OpensContext>) -> Unit)? = null
    private var closedFunc: ((context: NavigationContext<out OpensContext>) -> Unit)? = null

    @Suppress("UNCHECKED_CAST")
    public fun defaultOpened(args: ExecutorArgs<out FromContext, out OpensContext, out KeyType>) {
        when (args.binding) {
            is ActivityNavigationBinding ->
                DefaultActivityExecutor::open as ((ExecutorArgs<out Any, out OpensContext, out NavigationKey>) -> Unit)

            is FragmentNavigationBinding ->
                DefaultFragmentExecutor::open as ((ExecutorArgs<out Any, out OpensContext, out NavigationKey>) -> Unit)

            is SyntheticNavigationBinding ->
                DefaultSyntheticExecutor::open as ((ExecutorArgs<out Any, out OpensContext, out NavigationKey>) -> Unit)

            is ComposableNavigationBinding ->
                DefaultComposableExecutor::open as ((ExecutorArgs<out Any, out OpensContext, out NavigationKey>) -> Unit)

            else -> throw IllegalArgumentException("No default launch executor found for ${opensType.java}")
        }.invoke(args)
    }

    @Suppress("UNCHECKED_CAST")
    public fun defaultClosed(context: NavigationContext<out OpensContext>) {
        when (context.binding) {
            is ActivityNavigationBinding ->
                DefaultActivityExecutor::close as (NavigationContext<out OpensContext>) -> Unit

            is FragmentNavigationBinding ->
                DefaultFragmentExecutor::close as (NavigationContext<out OpensContext>) -> Unit

            is ComposableNavigationBinding ->
                DefaultComposableExecutor::close as (NavigationContext<out OpensContext>) -> Unit

            // Null means that we must be looking at a NoKeyNavigator, so we still want to pass back to
            // the default Activity/Fragment executor
            null -> when(context.contextReference) {
                is Activity -> DefaultActivityExecutor::close as (NavigationContext<out OpensContext>) -> Unit
                is Fragment -> DefaultFragmentExecutor::close as (NavigationContext<out OpensContext>) -> Unit
                else -> throw IllegalArgumentException("No default close executor found for NoKeyNavigator with context ${context.contextReference::class.java.simpleName}")
            }

            else -> throw IllegalArgumentException("No default close executor found for ${opensType.java}")
        }.invoke(context)
    }

    public fun animation(block: (instruction: AnyOpenInstruction) -> NavigationAnimation) {
        if (animationFunc != null) throw IllegalStateException("Value is already set!")
        animationFunc = block
    }

    public fun closeAnimation(block: (context: NavigationContext<out OpensContext>) -> NavigationAnimation) {
        if (closeAnimationFunc != null) throw IllegalStateException("Value is already set!")
        closeAnimationFunc = block
    }

    public fun preOpened(block: (context: NavigationContext<out FromContext>) -> Unit) {
        if (preOpenedFunc != null) throw IllegalStateException("Value is already set!")
        preOpenedFunc = block
    }

    public fun opened(block: (args: ExecutorArgs<out FromContext, out OpensContext, out KeyType>) -> Unit) {
        if (openedFunc != null) throw IllegalStateException("Value is already set!")
        openedFunc = block
    }

    public fun postOpened(block: (context: NavigationContext<out OpensContext>) -> Unit) {
        if (postOpenedFunc != null) throw IllegalStateException("Value is already set!")
        postOpenedFunc = block
    }

    public fun preClosed(block: (context: NavigationContext<out OpensContext>) -> Unit) {
        if (preClosedFunc != null) throw IllegalStateException("Value is already set!")
        preClosedFunc = block
    }

    public fun closed(block: (context: NavigationContext<out OpensContext>) -> Unit) {
        if (closedFunc != null) throw IllegalStateException("Value is already set!")
        closedFunc = block
    }

    internal fun build() = object : NavigationExecutor<FromContext, OpensContext, KeyType>(
        fromType,
        opensType,
        keyType
    ) {
        override fun animation(instruction: AnyOpenInstruction): NavigationAnimation {
            return animationFunc?.invoke(instruction) ?: super.animation(instruction)
        }

        override fun closeAnimation(context: NavigationContext<out OpensContext>): NavigationAnimation {
            return closeAnimationFunc?.invoke(context) ?: super.closeAnimation(context)
        }

        override fun preOpened(context: NavigationContext<out FromContext>) {
            preOpenedFunc?.invoke(context)
        }

        override fun open(args: ExecutorArgs<out FromContext, out OpensContext, out KeyType>) {
            openedFunc?.invoke(args) ?: defaultOpened(args)
        }

        override fun postOpened(context: NavigationContext<out OpensContext>) {
            postOpenedFunc?.invoke(context)
        }

        override fun preClosed(context: NavigationContext<out OpensContext>) {
            preClosedFunc?.invoke(context)
        }

        override fun close(context: NavigationContext<out OpensContext>) {
            closedFunc?.invoke(context) ?: defaultClosed(context)
        }
    }
}

public fun <From : Any, Opens : Any> createOverride(
    fromClass: KClass<From>,
    opensClass: KClass<Opens>,
    block: NavigationExecutorBuilder<From, Opens, NavigationKey>.() -> Unit
): NavigationExecutor<From, Opens, NavigationKey> =
    NavigationExecutorBuilder(fromClass, opensClass, NavigationKey::class)
        .apply(block)
        .build()

public inline fun <reified From : Any, reified Opens : Any> createOverride(
    noinline block: NavigationExecutorBuilder<From, Opens, NavigationKey>.() -> Unit
): NavigationExecutor<From, Opens, NavigationKey> =
    createOverride(From::class, Opens::class, block)

public inline fun <reified From : Fragment, reified Opens : Fragment> createSharedElementOverride(
    elements: List<Pair<Int, Int>>
): NavigationExecutor<From, Opens, NavigationKey> {
    return createOverride {
        opened { args ->
            args.instruction.setSharedElements(
                elements.map { EnroSharedElement(it.first, it.second) }
            )
            defaultOpened(args)
        }
    }
}