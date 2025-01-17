@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package dev.enro.test

import android.annotation.SuppressLint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import dev.enro.core.*
import dev.enro.core.container.NavigationBackstack
import dev.enro.core.container.NavigationContainerContext
import dev.enro.core.container.backstackOf
import dev.enro.core.container.toBackstack
import dev.enro.core.controller.EnroDependencyScope
import dev.enro.core.internal.handle.NavigationHandleScope
import junit.framework.TestCase
import org.junit.Assert.*
import java.lang.ref.WeakReference

class TestNavigationHandle<T : NavigationKey>(
    internal val navigationHandle: NavigationHandle
) : TypedNavigationHandle<T> {
    override val id: String
        get() = navigationHandle.id

    override val key: T
        get() = navigationHandle.key as T

    override val instruction: NavigationInstruction.Open<*>
        get() = navigationHandle.instruction

    override val dependencyScope: EnroDependencyScope
        get() = navigationHandle.dependencyScope

    internal var internalOnCloseRequested: () -> Unit = { close() }

    override val lifecycle: Lifecycle
        get() {
            return navigationHandle.lifecycle
        }

    val instructions: List<NavigationInstruction>
        get() = navigationHandle::class.java.getDeclaredField("instructions").let {
            it.isAccessible = true
            val instructions = it.get(navigationHandle)
            it.isAccessible = false
            return instructions as List<NavigationInstruction>
        }

    override fun executeInstruction(navigationInstruction: NavigationInstruction) {
        navigationHandle.executeInstruction(navigationInstruction)
    }
}

class FakeNavigationHandle internal constructor(
    key: NavigationKey,
    private val onCloseRequested: () -> Unit,
): NavigationHandle {
    override val instruction: NavigationInstruction.Open<*> = NavigationInstruction.Open.OpenInternal(
        navigationDirection = when(key) {
            is NavigationKey.SupportsPush -> NavigationDirection.Push
            is NavigationKey.SupportsPresent -> NavigationDirection.Present
            else -> NavigationDirection.Forward
        },
        navigationKey = key
    )
    private val instructions = mutableListOf<NavigationInstruction>()

    internal val navigationContainers = mutableMapOf<NavigationContainerKey, TestNavigationContainer>(
        TestNavigationContainer.parentContainer to createTestNavigationContainer(
            key = TestNavigationContainer.parentContainer,
            backstack = backstackOf(instruction)
        ),
        TestNavigationContainer.activeContainer to createTestNavigationContainer(TestNavigationContainer.activeContainer)
    )


    @SuppressLint("VisibleForTests")
    override val lifecycle: LifecycleRegistry = LifecycleRegistry.createUnsafe(this).apply {
        currentState = Lifecycle.State.RESUMED
    }

    override val id: String = instruction.instructionId
    override val key: NavigationKey = key
    override val dependencyScope: EnroDependencyScope = NavigationHandleScope(
        EnroTest.getCurrentNavigationController()
    ).bind(this)

    override fun executeInstruction(navigationInstruction: NavigationInstruction) {
        instructions.add(navigationInstruction)
        when (navigationInstruction) {
            is NavigationInstruction.RequestClose -> {
                onCloseRequested()
            }
            is NavigationInstruction.ContainerOperation -> {
                val containerKey = when (navigationInstruction.target) {
                    NavigationInstruction.ContainerOperation.Target.ParentContainer -> TestNavigationContainer.parentContainer
                    NavigationInstruction.ContainerOperation.Target.ActiveContainer -> TestNavigationContainer.activeContainer
                    is NavigationInstruction.ContainerOperation.Target.TargetContainer -> navigationInstruction.target.key
                }
                val container = navigationContainers[containerKey]
                    ?: throw IllegalStateException("TestNavigationHandle was not configured to have container with key $containerKey")
                container.apply(navigationInstruction.operation)
            }
            else -> {}
        }
    }
}

fun <T : NavigationKey> createTestNavigationHandle(
    key: T,
): TestNavigationHandle<T> {
    lateinit var navigationHandle: WeakReference<TestNavigationHandle<T>>
    val fakeNavigationHandle = FakeNavigationHandle(key) {
        navigationHandle.get()?.internalOnCloseRequested?.invoke()
    }
    navigationHandle = WeakReference(TestNavigationHandle(fakeNavigationHandle))
    return navigationHandle.get()!!
}

fun TestNavigationHandle<*>.putNavigationContainer(
    key: NavigationContainerKey,
    backstack: NavigationBackstack,
) : TestNavigationContainer {
    if(navigationHandle !is FakeNavigationHandle) {
        throw IllegalStateException("Cannot putNavigationContainer: TestNavigationHandle operating in a real environment")
    }
    val container = createTestNavigationContainer(key, backstack)
    navigationHandle.navigationContainers[key] = container
    return container
}

fun TestNavigationHandle<*>.putNavigationContainer(
    key: NavigationContainerKey,
    vararg instructions: NavigationInstruction.Open<*>,
) : TestNavigationContainer = putNavigationContainer(key, instructions.toList().toBackstack())

fun TestNavigationHandle<*>.expectCloseInstruction() {
    TestCase.assertTrue(instructions.last() is NavigationInstruction.Close)
}

fun <T : Any> TestNavigationHandle<*>.expectOpenInstruction(type: Class<T>, filter: (T) -> Boolean = { true }): NavigationInstruction.Open<*> {
    val instruction = instructions.filterIsInstance<NavigationInstruction.Open<*>>().last {
        runCatching { filter(it.navigationKey as T) }.getOrDefault(false)
    }
    assertTrue(type.isAssignableFrom(instruction.navigationKey::class.java))
    return instruction
}

inline fun <reified T : Any> TestNavigationHandle<*>.expectOpenInstruction(noinline filter: (T) -> Boolean = { true }): NavigationInstruction.Open<*> {
    return expectOpenInstruction(T::class.java, filter)
}

inline fun <reified T : Any> TestNavigationHandle<*>.expectOpenInstruction(key: T): NavigationInstruction.Open<*> {
    return expectOpenInstruction(T::class.java) { it == key }
}

fun TestNavigationHandle<*>.expectParentContainer(): NavigationContainerContext {
    lateinit var container: NavigationContainerContext
    onParentContainer { container = this@onParentContainer }
    return container
}

fun TestNavigationHandle<*>.expectActiveContainer(): NavigationContainerContext {
    lateinit var container: NavigationContainerContext
    onActiveContainer { container = this@onActiveContainer }
    return container
}

fun TestNavigationHandle<*>.expectContainer(key: NavigationContainerKey): NavigationContainerContext {
    lateinit var container: NavigationContainerContext
    onContainer(key) { container = this@onContainer }
    return container
}

fun TestNavigationHandle<*>.assertRequestedClose() {
    val instruction = instructions.filterIsInstance<NavigationInstruction.RequestClose>()
        .lastOrNull()
    assertNotNull(instruction)
}

fun TestNavigationHandle<*>.assertClosed() {
    val instruction = instructions.filterIsInstance<NavigationInstruction.Close>()
        .lastOrNull()
    assertNotNull(instruction)
}

fun TestNavigationHandle<*>.assertNotClosed() {
    val instruction = instructions.filterIsInstance<NavigationInstruction.Close>()
        .lastOrNull()
    assertNull(instruction)
}

fun <T : Any> TestNavigationHandle<*>.assertOpened(type: Class<T>, direction: NavigationDirection? = null): T {
    val instruction = instructions.filterIsInstance<NavigationInstruction.Open<*>>()
        .lastOrNull()

    assertNotNull(instruction)
    requireNotNull(instruction)

    assertTrue(type.isAssignableFrom(instruction.navigationKey::class.java))
    if (direction != null) {
        assertEquals(direction, instruction.navigationDirection)
    }
    return instruction.navigationKey as T
}

inline fun <reified T : Any> TestNavigationHandle<*>.assertOpened(direction: NavigationDirection? = null): T {
    return assertOpened(T::class.java, direction)
}

fun <T : Any> TestNavigationHandle<*>.assertAnyOpened(type: Class<T>, direction: NavigationDirection? = null): T {
    val instruction = instructions.filterIsInstance<NavigationInstruction.Open<*>>()
        .lastOrNull { type.isAssignableFrom(it.navigationKey::class.java) }

    assertNotNull(instruction)
    requireNotNull(instruction)

    assertTrue(type.isAssignableFrom(instruction.navigationKey::class.java))
    if (direction != null) {
        assertEquals(direction, instruction.navigationDirection)
    }
    return instruction.navigationKey as T
}

inline fun <reified T : Any> TestNavigationHandle<*>.assertAnyOpened(direction: NavigationDirection? = null): T {
    return assertAnyOpened(T::class.java, direction)
}

fun TestNavigationHandle<*>.assertNoneOpened() {
    val instruction = instructions.filterIsInstance<NavigationInstruction.Open<*>>()
        .lastOrNull()
    assertNull(instruction)
}

internal fun TestNavigationHandle<*>.getResult(): Any? {
    return instructions.filterIsInstance<NavigationInstruction.Close.WithResult>()
        .lastOrNull()
        ?.result
}

fun <T : Any> TestNavigationHandle<*>.assertResultDelivered(predicate: (T) -> Boolean): T {
    val result = getResult()
    assertNotNull(result)
    requireNotNull(result)
    result as T
    assertTrue(predicate(result))
    return result
}

fun <T : Any> TestNavigationHandle<*>.assertResultDelivered(expected: T): T {
    val result = getResult()
    assertEquals(expected, result)
    return result as T
}

inline fun <reified T : Any> TestNavigationHandle<*>.assertResultDelivered(): T {
    return assertResultDelivered { true }
}

fun TestNavigationHandle<*>.assertNoResultDelivered() {
    val result = getResult()
    assertNull(result)
}