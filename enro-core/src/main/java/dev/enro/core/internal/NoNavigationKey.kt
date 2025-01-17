package dev.enro.core.internal

import android.os.Bundle
import dev.enro.core.EnroInternalNavigationKey
import dev.enro.core.NavigationBinding
import dev.enro.core.NavigationKey
import kotlinx.parcelize.Parcelize
import kotlin.reflect.KClass

@Parcelize
internal class NoNavigationKey(
    val contextType: Class<*>,
    val arguments: Bundle?
) : NavigationKey, EnroInternalNavigationKey

internal class NoKeyNavigationBinding : NavigationBinding<NoNavigationKey, Nothing> {
    override val keyType: KClass<NoNavigationKey> = NoNavigationKey::class
    override val destinationType: KClass<Nothing> = Nothing::class
    override val baseType: KClass<in Nothing> = Nothing::class
}