package nav.enro.result

import androidx.test.core.app.ActivityScenario
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import nav.enro.DefaultActivity
import nav.enro.DefaultActivityKey
import nav.enro.core.*
import nav.enro.expectActivity
import nav.enro.expectContext
import org.junit.Test
import java.util.*

class ResultTests {
    @Test
    fun whenActivityRequestsResult_andResultProviderIsStandaloneFragment_thenResultIsReceived() {
        val scenario = ActivityScenario.launch(ResultReceiverActivity::class.java)
        val result = UUID.randomUUID().toString()
        scenario.onActivity {
            it.resultChannel.open(FragmentResultKey())
        }

        expectContext<ResultFragment, FragmentResultKey>()
            .navigation
            .closeWithResult(result)

        val activity = expectActivity<ResultReceiverActivity>()

        assertEquals(result, activity.result)
    }

    @Test
    fun whenActivityRequestsResult_andResultProviderIsActivity_thenResultIsReceived() {
        val scenario = ActivityScenario.launch(ResultReceiverActivity::class.java)
        val result = UUID.randomUUID().toString()
        scenario.onActivity {
            it.resultChannel.open(ActivityResultKey())
        }

        val resultActivity = expectActivity<ResultActivity>()
        resultActivity.getNavigationHandle()
            .asTyped<ActivityResultKey>()
            .closeWithResult(result)

        val activity = expectActivity<ResultReceiverActivity>()

        assertEquals(result, activity.result)
    }

    @Test
    fun whenActivityRequestsResult_andResultProviderIsNestedFragment_thenResultIsReceived() {
        val scenario = ActivityScenario.launch(ResultReceiverActivity::class.java)
        val result = UUID.randomUUID().toString()
        scenario.onActivity {
            it.resultChannel.open(NestedFragmentResultKey())
        }

        expectContext<NestedResultFragment, NestedFragmentResultKey>()
            .navigation
            .closeWithResult(result)

        val activity = expectActivity<ResultReceiverActivity>()

        assertEquals(result, activity.result)
    }


    @Test
    fun whenActivityRequestsResultThroughMultipleChannels_andResultProviderIsFragment_thenChannelUniquenessIsPreserved() {
        ActivityScenario.launch(ResultReceiverActivity::class.java)
        val result = UUID.randomUUID().toString()
        val secondaryResult = UUID.randomUUID().toString()

        expectActivity<ResultReceiverActivity>()
            .resultChannel
            .open(FragmentResultKey())

        expectContext<ResultFragment, FragmentResultKey>()
            .navigation
            .closeWithResult(result)

        expectActivity<ResultReceiverActivity>()
            .secondaryResultChannel
            .open(FragmentResultKey())

        expectContext<ResultFragment, FragmentResultKey>()
            .navigation
            .closeWithResult(secondaryResult)

        val activity = expectActivity<ResultReceiverActivity>()

        assertEquals(result, activity.result)
        assertEquals(secondaryResult, activity.secondaryResult)
    }

    @Test
    fun whenActivityRequestsResultThroughMultipleChannels_andResultProviderIsActivity_thenChannelUniquenessIsPreserved() {
        ActivityScenario.launch(ResultReceiverActivity::class.java)
        val result = UUID.randomUUID().toString()
        val secondaryResult = UUID.randomUUID().toString()

        expectActivity<ResultReceiverActivity>()
            .resultChannel
            .open(ActivityResultKey())

        expectActivity<ResultActivity>()
            .getNavigationHandle()
            .asTyped<ActivityResultKey>()
            .closeWithResult(result)

        expectActivity<ResultReceiverActivity>()
            .secondaryResultChannel
            .open(ActivityResultKey())

        expectActivity<ResultActivity>()
            .getNavigationHandle()
            .asTyped<ActivityResultKey>()
            .closeWithResult(secondaryResult)

        val activity = expectActivity<ResultReceiverActivity>()

        assertEquals(result, activity.result)
        assertEquals(secondaryResult, activity.secondaryResult)
    }

    @Test
    fun whenActivityRequestsResult_andActivityIsReCreated_thenResultIsStillSent() {
        val scenario = ActivityScenario.launch(ResultReceiverActivity::class.java)
        val result = UUID.randomUUID().toString()

        val initialActivity = expectActivity<ResultReceiverActivity>()
        val initalActivityHash = initialActivity.hashCode()

        scenario.recreate()
        initialActivity.resultChannel
            .open(ActivityResultKey())

        expectContext<ResultActivity, ActivityResultKey>()
            .navigation
            .closeWithResult(result)

        val activity = expectActivity<ResultReceiverActivity>()

        assertEquals(result, activity.result)
        assertFalse(initalActivityHash == activity.hashCode())
    }

    @Test
    fun whenFragmentRequestsResult_andResultProviderIsStandaloneFragment_thenResultIsReceived() {
        ActivityScenario.launch(DefaultActivity::class.java)
        val result = UUID.randomUUID().toString()

        expectContext<DefaultActivity, DefaultActivityKey>()
            .navigation
            .forward(ResultReceiverFragmentKey())

        expectContext<ResultReceiverFragment, ResultReceiverFragmentKey>()
            .context
            .resultChannel
            .open(FragmentResultKey())

        expectContext<ResultFragment, FragmentResultKey>()
            .navigation
            .closeWithResult(result)

        assertEquals(
            result,
            expectContext<ResultReceiverFragment, ResultReceiverFragmentKey>()
                .context
                .result
        )
    }

    @Test
    fun whenFragmentRequestsResult_andResultProviderIsActivity_thenResultIsReceived() {
        ActivityScenario.launch(DefaultActivity::class.java)
        val result = UUID.randomUUID().toString()

        expectContext<DefaultActivity, DefaultActivityKey>()
            .navigation
            .forward(ResultReceiverFragmentKey())

        expectContext<ResultReceiverFragment, ResultReceiverFragmentKey>()
            .context
            .resultChannel
            .open(ActivityResultKey())

        expectContext<ResultActivity, ActivityResultKey>()
            .navigation
            .closeWithResult(result)

        assertEquals(
            result,
            expectContext<ResultReceiverFragment, ResultReceiverFragmentKey>()
                .context
                .result
        )
    }

    @Test
    fun whenNestedFragmentRequestsResult_andResultProviderIsStandaloneFragment_thenResultIsReceived() {
        ActivityScenario.launch(NestedResultReceiverActivity::class.java)
        val result = UUID.randomUUID().toString()

        expectContext<NestedResultReceiverActivity, NestedResultReceiverActivityKey>()
            .navigation
            .forward(ResultReceiverFragmentKey())

        expectContext<ResultReceiverFragment, ResultReceiverFragmentKey>()
            .context
            .resultChannel
            .open(FragmentResultKey())

        expectContext<ResultFragment, FragmentResultKey>()
            .navigation
            .closeWithResult(result)

        assertEquals(
            result,
            expectContext<ResultReceiverFragment, ResultReceiverFragmentKey>()
                .context
                .result
        )
    }

    @Test
    fun whenNestedFragmentRequestsResult_andResultProviderIsActivity_thenResultIsReceived() {
        ActivityScenario.launch(NestedResultReceiverActivity::class.java)
        val result = UUID.randomUUID().toString()

        expectContext<NestedResultReceiverActivity, NestedResultReceiverActivityKey>()
            .navigation
            .forward(ResultReceiverFragmentKey())

        expectContext<ResultReceiverFragment, ResultReceiverFragmentKey>()
            .context
            .resultChannel
            .open(ActivityResultKey())

        expectContext<ResultActivity, ActivityResultKey>()
            .navigation
            .closeWithResult(result)

        assertEquals(
            result,
            expectContext<ResultReceiverFragment, ResultReceiverFragmentKey>()
                .context
                .result
        )
    }

    @Test
    fun whenNestedFragmentRequestsResult_andResultProviderIsNestedFragment_thenResultIsReceived() {
        ActivityScenario.launch(NestedResultReceiverActivity::class.java)
        val result = UUID.randomUUID().toString()

        expectContext<NestedResultReceiverActivity, NestedResultReceiverActivityKey>()
            .navigation
            .forward(ResultReceiverFragmentKey())

        expectContext<ResultReceiverFragment, ResultReceiverFragmentKey>()
            .context
            .resultChannel
            .open(NestedFragmentResultKey())

        expectContext<NestedResultFragment, NestedFragmentResultKey>()
            .navigation
            .closeWithResult(result)

        assertEquals(
            result,
            expectContext<ResultReceiverFragment, ResultReceiverFragmentKey>()
                .context
                .result
        )
    }

    @Test
    fun whenNestedFragmentRequestsResult_andResultProviderIsNestedFragmentSideBySideWithFragment_thenResultIsReceived() {
        ActivityScenario.launch(SideBySideNestedResultReceiverActivity::class.java)
        val result = UUID.randomUUID().toString()

        expectContext<SideBySideNestedResultReceiverActivity, SideBySideNestedResultReceiverActivityKey>()
            .navigation
            .forward(ResultReceiverFragmentKey())

        expectContext<ResultReceiverFragment, ResultReceiverFragmentKey>()
            .context
            .resultChannel
            .open(NestedFragmentResultKey())

        expectContext<NestedResultFragment, NestedFragmentResultKey>()
            .navigation
            .closeWithResult(result)

        assertEquals(
            result,
            expectContext<ResultReceiverFragment, ResultReceiverFragmentKey>()
                .context
                .result
        )
    }
}