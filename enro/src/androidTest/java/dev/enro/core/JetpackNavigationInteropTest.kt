package dev.enro.core

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import dev.enro.TestFragment
import dev.enro.application
import dev.enro.core.controller.navigationController
import dev.enro.expectFragment
import org.junit.After
import org.junit.Before
import org.junit.Test

class JetpackNavigationInteropTest {

    val override = createOverride<Any, Fragment> {
        closed {
            when (val parent = it.contextReference.parentFragment) {
                is NavHostFragment ->  parent.navController.popBackStack()
                else -> defaultClosed(it)
            }
        }
    }

    @Before
    fun before() {
        application.navigationController.addOverride(override)
    }

    @After
    fun after() {
        application.navigationController.removeOverride(override)
    }

    @Test
    fun whenBackButtonIsPressed_thenJetpackNavigationReceivesBackButtonPress() {
        val scenario = ActivityScenario.launch(JetpackNavigationActivity::class.java)

        expectFragment<JetpackNavigationFragment> {
            it.navigationArgument == 0
        }.openNext(scenario)

        expectFragment<JetpackNavigationFragment> {
            it.navigationArgument == 1
        }.openNext(scenario)

        expectFragment<JetpackNavigationFragment> {
            it.navigationArgument == 2
        }

        Espresso.pressBack()
        expectFragment<JetpackNavigationFragment> {
            it.navigationArgument == 1
        }

        Espresso.pressBack()
        expectFragment<JetpackNavigationFragment> {
            it.navigationArgument == 0
        }
    }

}

internal class JetpackNavigationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(dev.enro.test.R.layout.jetpack_navigation_activity_layout)
    }
}

internal class JetpackNavigationFragment : TestFragment() {
    val navigationArgument by lazy {
        requireArguments().getInt("argument", 0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val layout = requireView() as LinearLayout
        val title = layout.children.first() as TextView
        title.text = "Jetpack Navigation $navigationArgument"
    }

    fun openNext(activityScenario: ActivityScenario<*>) {
        activityScenario.onActivity {
            findNavController().navigate(
                dev.enro.test.R.id.JetpackNavigationFragment,
                bundleOf("argument" to navigationArgument + 1)
            )
        }
    }
}