package nav.enro.core.overrides

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import junit.framework.Assert.assertTrue
import nav.enro.*
import nav.enro.core.*
import nav.enro.core.navigationController
import nav.enro.core.createOverride
import nav.enro.core.defaultOpen
import org.junit.Before
import org.junit.Test

class FragmentToActivityOverrideTests() {

    lateinit var initialScenario: ActivityScenario<ActivityWithFragments>

    @Before
    fun before() {
        val intent = Intent(application, ActivityWithFragments::class.java)
            .addOpenInstruction(
                NavigationInstruction.Open(
                    NavigationDirection.FORWARD,
                    ActivityWithFragmentsKey(id = "initial activity")
                )
            )

        initialScenario = ActivityScenario.launch<ActivityWithFragments>(intent)
    }

    @Test
    fun givenFragmentToActivityOverride_whenFragmentIsStandalone_whenActivityIsLaunchedFrom_thenOverrideIsCalled() {
        var preOpenCalled = false
        var openCalled = false
        var postOpenCalled = false

        application.navigationController.addOverride(
            createOverride<GenericFragment, GenericActivity>{
                preOpened { preOpenCalled = true }
                postOpened { postOpenCalled = true }
                opened {
                    openCalled = true
                    defaultOpen<GenericActivity>().invoke(it)
                }
            }
        )

        initialScenario.getNavigationHandle<ActivityWithFragmentsKey>()
            .forward(GenericFragmentKey("override test"))

        expectFragment<GenericFragment>()
            .getNavigationHandle()
            .forward(GenericActivityKey("override test 2"))

        expectActivity<GenericActivity>()

        assertTrue(preOpenCalled)
        assertTrue(openCalled)
        assertTrue(postOpenCalled)
    }

    @Test
    fun givenFragmentToActivityOverride_whenFragmentIsStandalone_whenActivityIsClosed_thenOverrideIsCalled() {
        var closeOverrideCalled = false
        var preCloseCalled = false

        application.navigationController.addOverride(
            createOverride<GenericFragment, GenericActivity> {
                preClosed { preCloseCalled = true}
                closed {
                    closeOverrideCalled = true
                    defaultClose<GenericActivity>().invoke(it)
                }
            }
        )

        initialScenario.getNavigationHandle<ActivityWithFragmentsKey>()
            .forward(GenericFragmentKey("override test"))

        expectFragment<GenericFragment>()
            .getNavigationHandle()
            .forward(GenericActivityKey("override test 2"))

        expectActivity<GenericActivity>()
            .getNavigationHandle()
            .close()

        expectFragment<GenericFragment>()

        assertTrue(closeOverrideCalled)
        assertTrue(preCloseCalled)
    }


    @Test
    fun givenFragmentToActivityOverride_whenFragmentIsNested_whenActivityIsLaunchedFrom_thenOverrideIsCalled() {
        var preOpenCalled = false
        var openCalled = false
        var postOpenCalled = false

        application.navigationController.addOverride(
            createOverride<ActivityChildFragment, GenericActivity> {
                preOpened { preOpenCalled = true }
                postOpened { postOpenCalled = true }
                opened {
                    openCalled = true
                    defaultOpen<GenericActivity>().invoke(it)
                }
            }
        )

        initialScenario.getNavigationHandle<ActivityWithFragmentsKey>()
            .forward(ActivityChildFragmentKey("override test"))

        expectFragment<ActivityChildFragment>()
            .getNavigationHandle()
            .forward(GenericActivityKey("override test 2"))

        expectActivity<GenericActivity>()

        assertTrue(preOpenCalled)
        assertTrue(openCalled)
        assertTrue(postOpenCalled)
    }

    @Test
    fun givenFragmentToActivityOverride_whenFragmentIsNested_whenActivityIsClosed_thenOverrideIsCalled() {
        var closeOverrideCalled = false
        var preCloseCalled = false

        application.navigationController.addOverride(
            createOverride<ActivityChildFragment, GenericActivity> {
                preClosed { preCloseCalled = true }
                closed {
                    closeOverrideCalled = true
                    defaultClose<GenericActivity>().invoke(it)
                }
            }
        )

        initialScenario.getNavigationHandle<ActivityWithFragmentsKey>()
            .forward(ActivityChildFragmentKey("override test"))

        expectFragment<ActivityChildFragment>()
            .getNavigationHandle()
            .forward(GenericActivityKey("override test 2"))

        expectActivity<GenericActivity>()
            .getNavigationHandle()
            .close()

        expectFragment<ActivityChildFragment>()

        assertTrue(closeOverrideCalled)
        assertTrue(preCloseCalled)
    }

}