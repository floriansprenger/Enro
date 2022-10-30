package dev.enro.example

import android.app.AlertDialog
import dev.enro.annotations.NavigationDestination
import dev.enro.core.NavigationInstruction
import dev.enro.core.NavigationKey
import dev.enro.core.activity
import dev.enro.core.getNavigationHandle
import dev.enro.core.synthetic.SyntheticDestination
import kotlinx.parcelize.Parcelize

@Parcelize
data class SimpleMessage(
    val title: String,
    val message: String,
    val positiveActionInstruction: NavigationInstruction.Open<*>? = null
) : NavigationKey.SupportsPresent

@NavigationDestination(SimpleMessage::class)
class SimpleMessageDestination : SyntheticDestination<SimpleMessage>() {
    override fun process() {
        val activity = navigationContext.activity
        AlertDialog.Builder(activity).apply {
            setTitle(key.title)
            setMessage(key.message)
            setNegativeButton("Close") { _, _ -> }

            if(key.positiveActionInstruction != null) {
                setPositiveButton("Launch") {_, _ ->
                    navigationContext
                        .getNavigationHandle()
                        .executeInstruction(key.positiveActionInstruction!!)
                }
            }

            show()
        }
    }
}