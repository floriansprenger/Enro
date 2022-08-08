@file:Suppress("DEPRECATION")
package dev.enro.core.legacy

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import dev.enro.*
import dev.enro.core.close
import dev.enro.core.compose.ComposableDestination
import dev.enro.core.forward
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

private fun expectActivityHostForAnyInstruction(): FragmentActivity {
    return expectActivity { it::class.java.simpleName == "ActivityHostForAnyInstruction" }
}

class ActivityToComposableTests {

    @Test
    fun whenActivityOpensComposable_andActivityDoesNotHaveComposeContainer_thenComposableIsLaunchedAsComposableFragmentHost() {
        val scenario = ActivityScenario.launch(DefaultActivity::class.java)
        val handle = scenario.getNavigationHandle<DefaultActivityKey>()

        val id = UUID.randomUUID().toString()
        handle.forward(GenericComposableKey(id))

        expectActivityHostForAnyInstruction()
        expectContext<ComposableDestination, GenericComposableKey> {
            it.navigation.key.id == id
        }
    }

    @Test
    fun givenStandaloneComposable_whenHostActivityCloses_thenComposableViewModelStoreIsCleared() {
        val scenario = ActivityScenario.launch(DefaultActivity::class.java)
        val handle = scenario.getNavigationHandle<DefaultActivityKey>()

        handle.forward(GenericComposableKey(id = "StandaloneComposable"))

        expectActivityHostForAnyInstruction()

        val context = expectContext<ComposableDestination, GenericComposableKey>()

        val viewModel by ViewModelLazy(
            viewModelClass = OnClearedTrackingViewModel::class,
            storeProducer = { context.context.viewModelStore },
            factoryProducer = { ViewModelProvider.NewInstanceFactory() }
        )

        assertFalse(viewModel.onClearedCalled)

        context.navigation.close()

        expectActivity<DefaultActivity>()
        waitFor { viewModel.onClearedCalled }
        assertTrue(viewModel.onClearedCalled)
    }

    @Test
    fun givenActivityHostedComposable_whenHostActivityCloses_thenComposableViewModelStoreIsCleared() {
        val scenario = ActivityScenario.launch(ActivityWithComposables::class.java)
        val handle = scenario.getNavigationHandle<ActivityWithComposablesKey>()

        handle.forward(GenericComposableKey(id = "ComposableViewModelExample"))

        val context = expectContext<ComposableDestination, GenericComposableKey>()

        val viewModel by ViewModelLazy(
            viewModelClass = OnClearedTrackingViewModel::class,
            storeProducer = { context.context.viewModelStore },
            factoryProducer = { ViewModelProvider.NewInstanceFactory() }
        )

        handle.close()

        waitFor { viewModel.onClearedCalled }
        assertTrue(viewModel.onClearedCalled)
    }
}

class OnClearedTrackingViewModel : ViewModel() {
    var onClearedCalled = false

    override fun onCleared() {
        onClearedCalled = true
    }
}