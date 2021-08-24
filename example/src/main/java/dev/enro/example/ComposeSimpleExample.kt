package dev.enro.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.enro.annotations.NavigationDestination
import dev.enro.core.NavigationKey
import dev.enro.core.compose.navigationHandle
import dev.enro.core.forward
import dev.enro.core.replace
import dev.enro.core.replaceRoot
import kotlinx.parcelize.Parcelize

@Parcelize
data class ComposeSimpleExampleKey(
    val name: String,
    val launchedFrom: String,
    val backstack: List<String> = emptyList()
) : NavigationKey

@Composable
@NavigationDestination(ComposeSimpleExampleKey::class)
fun ComposeSimpleExample() {
    val navigation = navigationHandle<ComposeSimpleExampleKey>()
    val scrollState = rememberScrollState()

    EnroExampleTheme {
        Surface {
            val topContentHeight = remember { mutableStateOf(0)}
            val bottomContentHeight = remember { mutableStateOf(0)}
            val availableHeight = remember { mutableStateOf(0)}
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 8.dp)
                    .onGloballyPositioned { availableHeight.value = it.size.height },
            ) {
                Column(
                    modifier = Modifier.onGloballyPositioned { topContentHeight.value = it.size.height }
                ) {
                    Text(
                        text = "Example Composable",
                        style = MaterialTheme.typography.h4,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.example_content),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "Current Destination:",
                        modifier = Modifier.padding(top = 24.dp),
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        text = navigation.key.name,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = "Launched From:",
                        modifier = Modifier.padding(top = 24.dp),
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        text = navigation.key.launchedFrom,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = "Current Stack:",
                        modifier = Modifier.padding(top = 24.dp),
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        text = (navigation.key.backstack + navigation.key.name).joinToString(" -> "),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                val density = LocalDensity.current
                Spacer(modifier = Modifier.height(
                    if(scrollState.maxValue == 0) (availableHeight.value - topContentHeight.value - bottomContentHeight.value).div(density.density).dp - 1.dp else 0.dp
                ))

                Column(
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.onGloballyPositioned { bottomContentHeight.value = it.size.height }.padding(top = 16.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                        onClick = {
                            val next = ComposeSimpleExampleKey(
                                name = navigation.key.getNextDestinationName(),
                                launchedFrom = navigation.key.name,
                                backstack = navigation.key.backstack + navigation.key.name
                            )
                            navigation.forward(next)
                        }) {
                        Text("Forward")
                    }

                    OutlinedButton(
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                        onClick = {
                            val next = SimpleExampleKey(
                                name = navigation.key.getNextDestinationName(),
                                launchedFrom = navigation.key.name,
                                backstack = navigation.key.backstack + navigation.key.name
                            )
                            navigation.forward(next)
                        }) {
                        Text("Forward (Fragment)")
                    }

                    OutlinedButton(
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                        onClick = {
                            val next = ComposeSimpleExampleKey(
                                name = navigation.key.getNextDestinationName(),
                                launchedFrom = navigation.key.name,
                                backstack = navigation.key.backstack
                            )
                            navigation.replace(next)
                        }) {
                        Text("Replace")
                    }

                    OutlinedButton(
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                        onClick = {
                            val next = ComposeSimpleExampleKey(
                                name = navigation.key.getNextDestinationName(),
                                launchedFrom = navigation.key.name,
                                backstack = emptyList()
                            )
                            navigation.replaceRoot(next)

                        }) {
                        Text("Replace Root")
                    }
                }
            }
        }
    }

}

private fun ComposeSimpleExampleKey.getNextDestinationName(): String {
    if (name.length != 1) return "A"
    return (name[0] + 1).toString()
}