package com.sidequest.widgetmaker.shots

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.sidequest.widgetmaker.spec.Starters
import com.sidequest.widgetmaker.spec.WidgetTheme
import com.sidequest.widgetmaker.ui.DesignPreview
import com.sidequest.widgetmaker.ui.PreviewSize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Draws every starter at every size and theme into build/screenshots, for looking at by eye. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1400dp-h1200dp-xhdpi")
class StarterShots {
    @get:Rule val compose = createComposeRule()

    @Test
    fun render() {
        val out = File("build/screenshots").apply { mkdirs() }
        WidgetTheme.entries.forEach { theme ->
            Starters.all.forEachIndexed { i, starter ->
                compose.setContent {
                    // A wallpaper-like backdrop, so glass reads as it would on a home screen.
                    Column(
                        Modifier.testTag("shot")
                            .background(Brush.linearGradient(listOf(Color(0xFF4B6CB7), Color(0xFFE1A95F))))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("${starter.title} · ${theme.label}", color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                            PreviewSize.entries.forEach { DesignPreview(starter.design, it, theme) }
                        }
                    }
                }
                val bitmap: Bitmap = compose.onNodeWithTag("shot").captureToImage().asAndroidBitmap()
                File(out, "%s-%d-%s.png".format(theme.name, i + 1, starter.title.replace(' ', '_'))).outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }
}
