@file:Suppress("DEPRECATION")
package dev.enro.core.legacy

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import dev.enro.*
import dev.enro.annotations.NavigationDestination
import dev.enro.core.*
import junit.framework.TestCase.*
import kotlinx.parcelize.Parcelize
import org.junit.Test
import java.util.*

class ActivityToFragmentTests {

    @Test
    fun whenActivityIsNotAFragmentActivity_thenFragmentNavigationOpensSingleFragmentActivity() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity {
            it.getNavigationHandle().forward(GenericFragmentKey("fragment from component activity"))
        }
        expectActivityHostForAnyInstruction()
        assertEquals(
            "fragment from component activity",
            expectFragment<GenericFragment>()
                .getNavigationHandle()
                .asTyped<GenericFragmentKey>()
                .key
                .id
        )
    }

    @Test
    fun whenActivityOpensFragment_andActivityDoesNotHaveFragmentHost_thenFragmentIsLaunchedAsFullscreenDialogFragment() {
        val scenario = ActivityScenario.launch(DefaultActivity::class.java)
        val handle = scenario.getNavigationHandle<DefaultActivityKey>()

        val id = UUID.randomUUID().toString()
        handle.forward(GenericFragmentKey(id))

        val activity = expectFragmentHostForPresentableFragment()
        val activeFragment = activity.childFragmentManager.primaryNavigationFragment!!
        val fragmentHandle = activeFragment.getNavigationHandle().asTyped<GenericFragmentKey>()
        assertEquals(id, fragmentHandle.key.id)
    }

    @Test
    fun whenActivityOpensFragmentWithChildrenStack_andActivityDoesNotHaveFragmentHost_thenFragmentAndChildrenAreLaunchedAsSingleFragmentActivity() {
        val scenario = ActivityScenario.launch(DefaultActivity::class.java)
        val handle = scenario.getNavigationHandle<DefaultActivityKey>()

        val target = GenericFragmentKey(UUID.randomUUID().toString())
        handle.forward(
            GenericFragmentKey("1"),
            GenericFragmentKey("2"),
            target
        )

        val fragment = expectFragment<GenericFragment> { it.getNavigationHandle().key == target }
        val fragmentHandle = fragment.getNavigationHandle().asTyped<GenericFragmentKey>()
        assertEquals(target.id, fragmentHandle.key.id)
    }


    @Test
    fun whenActivityOpensFragment_andActivityHasFragmentHostForFragment_thenFragmentIsLaunchedIntoHost() {
        val scenario = ActivityScenario.launch(ActivityWithFragments::class.java)
        val handle = scenario.getNavigationHandle<ActivityWithFragmentsKey>()

        val id = UUID.randomUUID().toString()
        handle.forward(ActivityChildFragmentKey(id))

        expectActivity<ActivityWithFragments>()
        val activeFragment = expectFragment<ActivityChildFragment>()
        val fragmentHandle =
            activeFragment.getNavigationHandle().asTyped<ActivityChildFragmentKey>()
        assertEquals(id, fragmentHandle.key.id)
    }


    @Test
    fun whenActivityOpensFragment_andActivityHasFragmentHostForFragment_andFragmentContainerIsNotVisible_thenFragmentIsLaunchedIntoFullscreenDialogFragment() {
        val scenario = ActivityScenario.launch(ActivityWithFragments::class.java)
        val handle = scenario.getNavigationHandle<ActivityWithFragmentsKey>()
        scenario.onActivity {
            it.findViewById<View>(TestActivity.primaryFragmentContainer).isVisible = false
            it.findViewById<View>(TestActivity.secondaryFragmentContainer).isVisible = false
        }

        val id = UUID.randomUUID().toString()
        handle.forward(ActivityChildFragmentKey(id))

        expectFragmentHostForPresentableFragment()
        val activeFragment = expectFragment<ActivityChildFragment>()
        val fragmentHandle =
            activeFragment.getNavigationHandle().asTyped<ActivityChildFragmentKey>()
        assertEquals(id, fragmentHandle.key.id)
    }

    @Test
    fun whenActivityReplacedByFragment_andActivityHasFragmentHostForFragment_thenFragmentIsLaunchedAsSingleActivity_andCloseLeavesNoActivityActive() {
        val scenario = ActivityScenario.launch(ActivityWithFragments::class.java)
        val handle = scenario.getNavigationHandle<ActivityWithFragmentsKey>()

        val id = UUID.randomUUID().toString()
        handle.replace(ActivityChildFragmentKey(id))

        expectActivityHostForAnyInstruction()
        val activeFragment = expectFragment<ActivityChildFragment>()
        val fragmentHandle =
            activeFragment.getNavigationHandle().asTyped<ActivityChildFragmentKey>()
        assertEquals(id, fragmentHandle.key.id)

        fragmentHandle.close()
        expectNoActivity()
    }


    @Test
    fun whenActivityOpensFragment_andActivityHasFragmentHostThatDoesNotAcceptFragment_thenFragmentIsLaunchedAsFullscreenDialogFragment() {
        val scenario = ActivityScenario.launch(ActivityWithFragments::class.java)
        val handle = scenario.getNavigationHandle<ActivityWithFragmentsKey>()

        val id = UUID.randomUUID().toString()
        handle.forward(GenericFragmentKey(id))

        val activity = expectFragmentHostForPresentableFragment()
        val activeFragment = activity.childFragmentManager.primaryNavigationFragment!!
        val fragmentHandle = activeFragment.getNavigationHandle().asTyped<GenericFragmentKey>()
        assertEquals(id, fragmentHandle.key.id)
    }

    @Test
    fun whenActivityOpensFragmentAsReplacement_andActivityHasFragmentHostForFragment_thenFragmentIsLaunchedAsFullscreenDialogFragment() {
        val scenario = ActivityScenario.launch(ActivityWithFragments::class.java)
        val handle = scenario.getNavigationHandle<ActivityWithFragmentsKey>()

        val id = UUID.randomUUID().toString()
        handle.replace(ActivityChildFragmentKey(id))

        val activity = expectActivityHostForAnyInstruction()
        val activeFragment = activity.supportFragmentManager.primaryNavigationFragment!!
        val fragmentHandle =
            activeFragment.getNavigationHandle().asTyped<ActivityChildFragmentKey>()
        assertEquals(id, fragmentHandle.key.id)
    }

    @Test
    fun whenActivityOpensTwoFragmentsImmediatelyIntoDifferentContainers_thenBothFragmentsAreCorrectlyAddedToContainers() {
        val scenario = ActivityScenario.launch(ImmediateOpenChildActivity::class.java)

        scenario.onActivity {
            assertEquals(
                "one",
                it.supportFragmentManager.findFragmentById(TestActivity.primaryFragmentContainer)!!
                    .getNavigationHandle()
                    .asTyped<GenericFragmentKey>()
                    .key.id
            )

            assertEquals(
                "two",
                it.supportFragmentManager.findFragmentById(TestActivity.secondaryFragmentContainer)!!
                    .getNavigationHandle()
                    .asTyped<GenericFragmentKey>()
                    .key.id
            )
        }
    }

    @Test
    fun whenActivityOpensTwoFragmentsImmediatelyIntoDifferentContainers_andThoseFragmentsOpenTwoChildrenImmediately_thenAllFragmentsAreOpenedCorrectly() {
        val scenario = ActivityScenario.launch(ImmediateOpenFragmentChildActivity::class.java)

        scenario.onActivity {
            val primary =
                it.supportFragmentManager.findFragmentById(TestActivity.primaryFragmentContainer)!!
            val secondary =
                it.supportFragmentManager.findFragmentById(TestActivity.secondaryFragmentContainer)!!

            assertEquals(
                "one", primary.childFragmentManager
                    .findFragmentById(TestFragment.primaryFragmentContainer)!!
                    .getNavigationHandle()
                    .asTyped<GenericFragmentKey>()
                    .key.id
            )

            assertEquals(
                "two", primary.childFragmentManager
                    .findFragmentById(TestFragment.secondaryFragmentContainer)!!
                    .getNavigationHandle()
                    .asTyped<GenericFragmentKey>()
                    .key.id
            )

            assertEquals(
                "one", secondary.childFragmentManager
                    .findFragmentById(TestFragment.primaryFragmentContainer)!!
                    .getNavigationHandle()
                    .asTyped<GenericFragmentKey>()
                    .key.id
            )

            assertEquals(
                "two", secondary.childFragmentManager
                    .findFragmentById(TestFragment.secondaryFragmentContainer)!!
                    .getNavigationHandle()
                    .asTyped<GenericFragmentKey>()
                    .key.id
            )
        }
    }

    /**
     * Executing navigation instructions as a response to fragment creation (i.e. in "onCreate") may cause issues
     * with attempting to access viewmodels from a detached fragment. This test should verify
     * that the behaviour of the test above will continue to work after activity re-creation
     */
    @Test
    fun whenActivityOpensTwoFragmentsImmediatelyIntoDifferentContainers_andThoseFragmentsOpenTwoChildrenImmediately_thenAllFragmentsAreOpenedCorrectly_recreated() {
        val scenario = ActivityScenario.launch(ImmediateOpenFragmentChildActivity::class.java)
        scenario.recreate()

        scenario.onActivity {
            val primary =
                it.supportFragmentManager.findFragmentById(TestActivity.primaryFragmentContainer)!!
            val secondary =
                it.supportFragmentManager.findFragmentById(TestActivity.secondaryFragmentContainer)!!

            assertEquals(
                "one", primary.childFragmentManager
                    .findFragmentById(TestFragment.primaryFragmentContainer)!!
                    .getNavigationHandle()
                    .asTyped<GenericFragmentKey>()
                    .key.id
            )

            assertEquals(
                "two", primary.childFragmentManager
                    .findFragmentById(TestFragment.secondaryFragmentContainer)!!
                    .getNavigationHandle()
                    .asTyped<GenericFragmentKey>()
                    .key.id
            )

            assertEquals(
                "one", secondary.childFragmentManager
                    .findFragmentById(TestFragment.primaryFragmentContainer)!!
                    .getNavigationHandle()
                    .asTyped<GenericFragmentKey>()
                    .key.id
            )

            assertEquals(
                "two", secondary.childFragmentManager
                    .findFragmentById(TestFragment.secondaryFragmentContainer)!!
                    .getNavigationHandle()
                    .asTyped<GenericFragmentKey>()
                    .key.id
            )
        }
    }

    @Test
    fun givenActivityWithChildFragment_whenMultipleChildrenAreOpenedOnActivity_andStaleChildFragmentHandleIsUsedToOpenAnotherChild_thenStaleNavigationActionIsIgnored_andOtherNavigationActionsSucceed() {
        val scenario = ActivityScenario.launch(ActivityWithFragments::class.java)
        expectActivity<ActivityWithFragments>()
            .getNavigationHandle()
            .forward(ActivityChildFragmentKey(UUID.randomUUID().toString()))

        val activityHandle = expectActivity<ActivityWithFragments>().getNavigationHandle()
        val fragmentHandle = expectFragment<ActivityChildFragment>().getNavigationHandle()

        scenario.onActivity {
            activityHandle
                .forward(ActivityChildFragmentKey("one"))

            activityHandle
                .forward(ActivityChildFragmentKey("two"))

            fragmentHandle
                .forward(ActivityChildFragmentKey("three"))

            activityHandle
                .forward(ActivityChildFragmentKey("four"))
        }

        val id = expectFragment<ActivityChildFragment>().getNavigationHandle().asTyped<ActivityChildFragmentKey>().key.id
        assertEquals("four", id)
    }

    @Test
    fun givenActivityWithChildFragment_whenFragmentIsDetached_andStaleFragmentNavigationHandleIsUsedForNavigation_thenNothingHappens() {
        val scenario = ActivityScenario.launch(ActivityWithFragments::class.java)
        expectActivity<ActivityWithFragments>()
            .getNavigationHandle()
            .forward(ActivityChildFragmentKey(UUID.randomUUID().toString()))

        val fragment = expectFragment<ActivityChildFragment>()
        val fragmentHandle = fragment.getNavigationHandle()

        scenario.onActivity {
            it.supportFragmentManager.beginTransaction()
                .detach(fragment)
                .commitNow()

            fragmentHandle.forward(ActivityChildFragmentKey("should not appear"))
        }

        assertNull(expectActivity<ActivityWithFragments>().supportFragmentManager.primaryNavigationFragment)
    }

    @Test
    fun givenFragmentOpenInActivity_whenFragmentIsClosedAfterInstanceStateIsSaved_thenNavigationIsNotClosed_untilActivityIsActiveAgain() {
        val scenario = ActivityScenario.launch(ActivityWithFragments::class.java)
        expectActivity<ActivityWithFragments>()
            .getNavigationHandle()
            .forward(ActivityChildFragmentKey(UUID.randomUUID().toString()))

        val fragment = expectFragment<ActivityChildFragment>()
        val fragmentHandle = fragment.getNavigationHandle()

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity {
            assertTrue(it.supportFragmentManager.isStateSaved)
        }
        fragmentHandle.close()
        scenario.moveToState(Lifecycle.State.RESUMED)
        expectNoFragment<ActivityChildFragment>()
    }

    @Test
    fun givenFragmentOpenInActivity_whenFragmentIsClosedAfterInstanceStateIsSaved_thenNavigationIsNotClosed_untilActivityIsActiveAgain_recreation() {
        val scenario = ActivityScenario.launch(ActivityWithFragments::class.java)
        expectActivity<ActivityWithFragments>()
            .getNavigationHandle()
            .forward(ActivityChildFragmentKey(UUID.randomUUID().toString()))

        val fragment = expectFragment<ActivityChildFragment>()
        val fragmentHandle = fragment.getNavigationHandle()

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity {
            assertTrue(it.supportFragmentManager.isStateSaved)
        }
        fragmentHandle.close()
        scenario.recreate()
        scenario.moveToState(Lifecycle.State.RESUMED)
        expectActivity<ActivityWithFragments>()
        expectNoFragment<ActivityChildFragment>()
    }

    @Test
    fun givenTwoFragmentsOpenInActivity_whenTopFragmentIsClosedAfterInstanceStateIsSaved_thenNavigationIsNotClosed_untilActivityIsActiveAgain_andCorrectFragmentIsActive() {
        val scenario = ActivityScenario.launch(ActivityWithFragments::class.java)
        val firstFragmentKey = ActivityChildFragmentKey(UUID.randomUUID().toString())
        val secondFragmentKey = ActivityChildFragmentKey(UUID.randomUUID().toString())

        expectActivity<ActivityWithFragments>()
            .getNavigationHandle()
            .forward(firstFragmentKey)

        expectFragment<ActivityChildFragment> { it.getNavigationHandle().key == firstFragmentKey }
            .navigation
            .forward(secondFragmentKey)

        val fragment = expectFragment<ActivityChildFragment> { it.getNavigationHandle().key == secondFragmentKey }
        val fragmentHandle = fragment.getNavigationHandle()

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity {
            assertTrue(it.supportFragmentManager.isStateSaved)
        }
        fragmentHandle.close()
        scenario.moveToState(Lifecycle.State.RESUMED)
        expectFragment<ActivityChildFragment> { it.getNavigationHandle().key == firstFragmentKey }
        expectNoFragment<ActivityChildFragment> { it.getNavigationHandle().key == secondFragmentKey }
    }

    @Test
    fun givenTwoFragmentsOpenInActivity_whenTopFragmentIsClosedAfterInstanceStateIsSaved_thenNavigationIsNotClosed_untilActivityIsActiveAgain_andCorrectFragmentIsActive_recreation() {
        val scenario = ActivityScenario.launch(ActivityWithFragments::class.java)
        val firstFragmentKey = ActivityChildFragmentKey(UUID.randomUUID().toString())
        val secondFragmentKey = ActivityChildFragmentKey(UUID.randomUUID().toString())

        expectActivity<ActivityWithFragments>()
            .getNavigationHandle()
            .forward(firstFragmentKey)

        expectFragment<ActivityChildFragment> { it.getNavigationHandle().key == firstFragmentKey }
            .navigation
            .forward(secondFragmentKey)

        val fragment = expectFragment<ActivityChildFragment> { it.getNavigationHandle().key == secondFragmentKey }
        val fragmentHandle = fragment.getNavigationHandle()

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity {
            assertTrue(it.supportFragmentManager.isStateSaved)
        }
        fragmentHandle.close()
        scenario.recreate()
        scenario.moveToState(Lifecycle.State.RESUMED)
        expectFragment<ActivityChildFragment> { it.getNavigationHandle().key == firstFragmentKey }
        expectNoFragment<ActivityChildFragment> { it.getNavigationHandle().key == secondFragmentKey }
    }

    // https://github.com/isaac-udy/Enro/issues/34
    /**
     * givenActivityOpensFragmentA
     * andFragmentAPerformsForwardNavigationToFragmentB
     * andFragmentBPerformsForwardNavigationToFragmentC
     *
     * whenActivityLaterPerformsForwardNavigationToFragmentD
     * andFragmentDIsClosed
     *
     * thenFragmentCIsActiveInContainer
     */
    @Test
    fun givenActivityOpensFragment_andFragmentOpensForward_thenActivityOpensAnotherFragment_thenContainerBackstackIsRetained() {
        val scenario = ActivityScenario.launch(ActivityWithFragments::class.java)
        val fragmentAKey = ActivityChildFragmentKey("Fragment A")
        val fragmentBKey = ActivityChildFragmentKey("Fragment B")
        val fragmentCKey = ActivityChildFragmentKey("Fragment C")
        val fragmentDKey = ActivityChildFragmentKey("Fragment D")

        val activity = expectActivity<ActivityWithFragments>()
        activity.getNavigationHandle()
            .forward(fragmentAKey)

        expectContext<ActivityChildFragment, ActivityChildFragmentKey> { it.navigation.key == fragmentAKey }
            .navigation
            .forward(fragmentBKey)

        expectContext<ActivityChildFragment, ActivityChildFragmentKey> { it.navigation.key == fragmentBKey }
            .navigation
            .forward(fragmentCKey)

        expectContext<ActivityChildFragment, ActivityChildFragmentKey> { it.navigation.key == fragmentCKey }

        activity.getNavigationHandle()
            .forward(fragmentDKey)

        expectContext<ActivityChildFragment, ActivityChildFragmentKey> { it.navigation.key == fragmentDKey }
            .navigation
            .close()

        expectContext<ActivityChildFragment, ActivityChildFragmentKey> { it.navigation.key == fragmentCKey }
    }
}

@Parcelize
class ImmediateOpenChildActivityKey : NavigationKey

@NavigationDestination(ImmediateOpenChildActivityKey::class)
class ImmediateOpenChildActivity : TestActivity() {
    private val navigation by navigationHandle<ImmediateOpenChildActivityKey> {
        defaultKey(ImmediateOpenChildActivityKey())
        container(primaryFragmentContainer) {
            it is GenericFragmentKey && it.id == "one"
        }
        container(secondaryFragmentContainer) {
            it is GenericFragmentKey && it.id == "two"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigation.forward(GenericFragmentKey("one"))
        navigation.forward(GenericFragmentKey("two"))
    }
}

@Parcelize
class ImmediateOpenFragmentChildActivityKey : NavigationKey

@NavigationDestination(ImmediateOpenFragmentChildActivityKey::class)
class ImmediateOpenFragmentChildActivity : TestActivity() {
    private val navigation by navigationHandle<ImmediateOpenFragmentChildActivityKey> {
        defaultKey(ImmediateOpenFragmentChildActivityKey())
        container(primaryFragmentContainer) {
            it is ImmediateOpenChildFragmentKey && it.name == "one"
        }
        container(secondaryFragmentContainer) {
            it is ImmediateOpenChildFragmentKey && it.name == "two"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigation.forward(ImmediateOpenChildFragmentKey("one"))
        navigation.forward(ImmediateOpenChildFragmentKey("two"))
    }
}


@Parcelize
data class ImmediateOpenChildFragmentKey(val name: String) : NavigationKey

@NavigationDestination(ImmediateOpenChildFragmentKey::class)
class ImmediateOpenChildFragment : TestFragment() {
    private val navigation by navigationHandle<ImmediateOpenChildFragmentKey> {
        container(primaryFragmentContainer) {
            it is GenericFragmentKey && it.id == "one"
        }

        container(secondaryFragmentContainer) {
            it is GenericFragmentKey && it.id == "two"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigation.forward(GenericFragmentKey("one"))
        navigation.forward(GenericFragmentKey("two"))
    }
}