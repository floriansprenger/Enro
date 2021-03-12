package dev.enro.core.controller.lifecycle

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import dev.enro.core.*

internal class NavigationContextLifecycleCallbacks (
    private val lifecycleController: NavigationLifecycleController
) {

    private val fragmentCallbacks = FragmentCallbacks()
    private val activityCallbacks = ActivityCallbacks()

    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(activityCallbacks)
    }

    inner class ActivityCallbacks :  Application.ActivityLifecycleCallbacks  {
        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?
        ) {
            if(activity !is FragmentActivity) return
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
            lifecycleController.onContextCreated(ActivityContext(activity), savedInstanceState)
        }

        override fun onActivitySaveInstanceState(
            activity: Activity,
            outState: Bundle
        ) {
            if(activity !is FragmentActivity) return
            lifecycleController.onContextSaved(activity.navigationContext, outState)
        }

        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    inner class FragmentCallbacks :  FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentCreated(
            fm: FragmentManager,
            fragment: Fragment,
            savedInstanceState: Bundle?
        ) {
            lifecycleController.onContextCreated(FragmentContext(fragment), savedInstanceState)
        }

        override fun onFragmentSaveInstanceState(
            fm: FragmentManager,
            fragment: Fragment,
            outState: Bundle
        ) {
            lifecycleController.onContextSaved(fragment.navigationContext, outState)
        }
    }
}