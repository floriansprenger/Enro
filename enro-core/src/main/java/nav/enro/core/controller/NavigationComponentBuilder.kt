    package nav.enro.core.controller

import nav.enro.core.*
import nav.enro.core.controller.container.ExecutorContainer
import nav.enro.core.controller.container.NavigatorContainer
import nav.enro.core.controller.container.PluginContainer
import nav.enro.core.controller.interceptor.InstructionInterceptorController
import nav.enro.core.controller.interceptor.InstructionParentInterceptor
import nav.enro.core.controller.lifecycle.NavigationLifecycleController
import nav.enro.core.plugins.EnroHilt
import nav.enro.core.plugins.EnroPlugin

// TODO get rid of this, or give it a better name
interface NavigationComponentBuilderCommand {
    fun execute(builder: NavigationComponentBuilder)
}

class NavigationComponentBuilder {
    @PublishedApi
    internal val navigators: MutableList<Navigator<*, *>> = mutableListOf()
    @PublishedApi
    internal val overrides: MutableList<NavigationExecutor<*, *, *>> = mutableListOf()
    @PublishedApi
    internal val plugins: MutableList<EnroPlugin> = mutableListOf()

    fun navigator(navigator: Navigator<*, *>) {
        navigators.add(navigator)
    }

    fun override(override: NavigationExecutor<*, *, *>) {
        overrides.add(override)
    }

    inline fun <reified From : Any, reified Opens : Any> override(
        noinline block: NavigationExecutorBuilder<From, Opens, NavigationKey>.() -> Unit
    ) {
        overrides.add(createOverride(From::class, Opens::class, block))
    }

    fun component(builder: NavigationComponentBuilder) {
        navigators.addAll(builder.navigators)
        overrides.addAll(builder.overrides)
        plugins.addAll(builder.plugins)
    }

    fun plugin(enroPlugin: EnroPlugin) {
        plugins.add(enroPlugin)
    }

    internal fun build(): NavigationController {
        val useHilt = plugins.any { it is EnroHilt }

        val pluginContainer = PluginContainer(plugins)
        val navigatorContainer = NavigatorContainer(navigators, useHilt)
        val executorContainer = ExecutorContainer(overrides)

        val interceptorController = InstructionInterceptorController(
            listOf(
                InstructionParentInterceptor(navigatorContainer)
            )
        )
        val contextController = NavigationLifecycleController(executorContainer, pluginContainer)

        return NavigationController(
            pluginContainer = pluginContainer,
            navigatorContainer = navigatorContainer,
            executorContainer = executorContainer,
            interceptorController = interceptorController,
            contextController = contextController,
        )
    }
}

/**
 * Create a NavigationController from the NavigationControllerDefinition/DSL, and immediately attach it
 * to the NavigationApplication from which this function was called.
 */
fun NavigationApplication.navigationController(block: NavigationComponentBuilder.() -> Unit = {}): NavigationController {
    return NavigationComponentBuilder()
        .apply { generatedComponent?.execute(this) }
        .apply(block)
        .build()
        .apply { install(this@navigationController) }
}

private val NavigationApplication.generatedComponent get(): NavigationComponentBuilderCommand? =
    runCatching {
        Class.forName(this::class.java.name + "Navigation")
            .newInstance() as NavigationComponentBuilderCommand
    }.getOrNull()

/**
 * Create a NavigationControllerBuilder, without attaching it to a NavigationApplication.
 *
 * This method is primarily used for composing several builder definitions together in a final NavigationControllerBuilder.
 */
fun createNavigationComponent(block: NavigationComponentBuilder.() -> Unit): NavigationComponentBuilder {
    return NavigationComponentBuilder()
        .apply(block)
}