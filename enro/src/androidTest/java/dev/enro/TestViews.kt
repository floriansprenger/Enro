package dev.enro

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import dev.enro.core.NavigationContainerKey
import dev.enro.core.NavigationKey
import dev.enro.core.compose.EnroContainer
import dev.enro.core.compose.navigationHandle
import dev.enro.core.compose.rememberNavigationContainer
import dev.enro.core.getNavigationHandle

abstract class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val key = try {
            getNavigationHandle().key
        } catch (t: Throwable) {
        }

        Log.e("TestActivity", "Opened $key")

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER

                addView(TextView(this@TestActivity).apply {
                    text = this@TestActivity::class.java.simpleName
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 32.0f)
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    gravity = Gravity.CENTER
                })

                addView(TextView(this@TestActivity).apply {
                    text = key.toString()
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f)
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    gravity = Gravity.CENTER
                })

                addView(TextView(this@TestActivity).apply {
                    id = debugText
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f)
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    gravity = Gravity.CENTER
                })

                addView(FrameLayout(this@TestActivity).apply {
                    id = primaryFragmentContainer
                    setBackgroundColor(0x22FF0000)
                    setPadding(50)
                })

                addView(FrameLayout(this@TestActivity).apply {
                    id = secondaryFragmentContainer
                    setBackgroundColor(0x220000FF)
                    setPadding(50)
                })
            }
        )
    }

    companion object {
        val debugText = View.generateViewId()
        val primaryFragmentContainer = View.generateViewId()
        val secondaryFragmentContainer = View.generateViewId()

        val primaryFragmentContainerKey = NavigationContainerKey.FromId(primaryFragmentContainer)
        val secondaryFragmentContainerKey = NavigationContainerKey.FromId(secondaryFragmentContainer)
    }
}

abstract class TestFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val key = try {
            getNavigationHandle().key
        } catch (t: Throwable) {
        "No Navigation Key"
        }

        Log.e("TestFragment", "Opened $key")

        return LinearLayout(requireContext().applicationContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFFFFFFF.toInt())

            addView(TextView(context).apply {
                text = this@TestFragment::class.java.simpleName
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 32.0f)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                gravity = Gravity.CENTER
            })

            addView(TextView(context).apply {
                text = key.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                gravity = Gravity.CENTER
            })

            addView(TextView(context).apply {
                id = debugText
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                gravity = Gravity.CENTER
            })

            addView(FrameLayout(context).apply {
                id = primaryFragmentContainer
                setPadding(50)
                setBackgroundColor(0x22FF0000)
            })

            addView(FrameLayout(context).apply {
                id = secondaryFragmentContainer
                setPadding(50)
                setBackgroundColor(0x220000FF)
            })
        }
    }

    companion object {
        val debugText = View.generateViewId()
        val primaryFragmentContainer = View.generateViewId()
        val secondaryFragmentContainer = View.generateViewId()

        val primaryFragmentContainerKey = NavigationContainerKey.FromId(primaryFragmentContainer)
        val secondaryFragmentContainerKey = NavigationContainerKey.FromId(secondaryFragmentContainer)
    }
}

abstract class TestDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val key = try {
            getNavigationHandle().key
        } catch (t: Throwable) {
        }

        Log.e("TestFragment", "Opened $key")

        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            addView(TextView(requireContext()).apply {
                text = this@TestDialogFragment::class.java.simpleName
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 32.0f)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                gravity = Gravity.CENTER
            })

            addView(TextView(requireContext()).apply {
                text = key.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                gravity = Gravity.CENTER
            })

            addView(TextView(requireContext()).apply {
                id = debugText
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                gravity = Gravity.CENTER
            })

            addView(FrameLayout(requireContext()).apply {
                id = primaryFragmentContainer
                setPadding(50)
                setBackgroundColor(0x22FF0000)
            })

            addView(FrameLayout(requireContext()).apply {
                id = secondaryFragmentContainer
                setPadding(50)
                setBackgroundColor(0x220000FF)
            })
        }
    }

    companion object {
        val debugText = View.generateViewId()
        val primaryFragmentContainer = View.generateViewId()
        val secondaryFragmentContainer = View.generateViewId()

    }
}

@Composable
fun TestComposable(
    name: String,
    primaryContainerAccepts: (NavigationKey) -> Boolean = { false },
    secondaryContainerAccepts: (NavigationKey) -> Boolean = { false }
) {
    val primaryContainer = rememberNavigationContainer(
        accept = primaryContainerAccepts
    )

    val secondaryContainer = rememberNavigationContainer(
        accept = secondaryContainerAccepts
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.defaultMinSize(minHeight = 224.dp, minWidth = 112.dp)
    ) {
        Text(
            text = name,
            fontSize = 32.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(20.dp)
        )
        Text(
            text = navigationHandle().key.toString(),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(20.dp)
        )
        EnroContainer(
            container = primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(Color(0x22FF0000))
                .padding(horizontal = 20.dp)
        )
        EnroContainer(
            container = secondaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(Color(0x220000FF))
                .padding(20.dp)
        )
    }
}
