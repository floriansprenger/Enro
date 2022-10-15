package dev.enro.core.activity

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import dev.enro.core.*

public object DefaultActivityExecutor : NavigationExecutor<Any, ComponentActivity, NavigationKey>(
    fromType = Any::class,
    opensType = ComponentActivity::class,
    keyType = NavigationKey::class
) {
    override fun open(args: ExecutorArgs<out Any, out ComponentActivity, out NavigationKey>) {
        val fromContext = args.fromContext
        val instruction = args.instruction

        val intent = createIntent(args)

        if (instruction.navigationDirection == NavigationDirection.ReplaceRoot) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val activity = fromContext.activity
        if (instruction.navigationDirection == NavigationDirection.Replace || instruction.navigationDirection == NavigationDirection.ReplaceRoot) {
            activity.finish()
        }
        val animations = animationsFor(fromContext, instruction).asResource(activity.theme)

        val options = ActivityOptionsCompat.makeCustomAnimation(activity, animations.enter, animations.exit)
        activity.startActivity(intent, options.toBundle())
    }

    override fun close(context: NavigationContext<out ComponentActivity>) {
        ActivityCompat.finishAfterTransition(context.activity)
        context.binding ?: return

        val animations = animationsFor(context, NavigationInstruction.Close).asResource(context.activity.theme)
        context.activity.overridePendingTransition(animations.enter, animations.exit)
    }

    public fun createIntent(args: ExecutorArgs<out Any, out ComponentActivity, out NavigationKey>): Intent =
        Intent(args.fromContext.activity, args.binding.destinationType.java)
            .addOpenInstruction(args.instruction)
}