package nav.enro.example

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import nav.enro.annotations.NavigationDestination
import nav.enro.core.*
import nav.enro.core.context.NavigationContext
import nav.enro.core.context.activity
import nav.enro.core.navigator.SyntheticDestination
import nav.enro.example.core.data.UserRepository
import nav.enro.example.core.navigation.DashboardKey
import nav.enro.example.core.navigation.LaunchKey
import nav.enro.example.core.navigation.LoginKey

class MainActivity : AppCompatActivity() {
    private val navigation by navigationHandle<Nothing>()

    override fun onResume() {
        super.onResume()
        findViewById<View>(android.R.id.content)
            .animate()
            .setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    navigation.replace(LaunchKey)
                }
            })
            .start()
    }

    override fun onPause() {
        super.onPause()
        findViewById<View>(android.R.id.content)
            .animate()
            .cancel()
    }
}

@NavigationDestination(LaunchKey::class)
class LaunchDestination : SyntheticDestination<LaunchKey> {
    override fun process(
        navigationContext: NavigationContext<out Any, out NavigationKey>,
        instruction: NavigationInstruction.Open<LaunchKey>
    ) {
        val navigation = navigationContext.activity.getNavigationHandle<Nothing>()
        val userRepo = UserRepository.instance
        val user = userRepo.activeUser
        val key = when (user) {
            null -> LoginKey()
            else -> DashboardKey(user)
        }
        navigation.replaceRoot(key)
    }
}