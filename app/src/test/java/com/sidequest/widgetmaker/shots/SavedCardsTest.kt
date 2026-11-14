package com.sidequest.widgetmaker.shots

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.sidequest.widgetmaker.spec.SavedDesign
import com.sidequest.widgetmaker.spec.Starters
import com.sidequest.widgetmaker.spec.WidgetTheme
import com.sidequest.widgetmaker.ui.MakerTheme
import com.sidequest.widgetmaker.ui.SavedCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Every ready-made design as a saved card in a two-column list, as Active and Mine show them: none may crash. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1100dp-h4000dp-night-xhdpi")
class SavedCardsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everySavedCardDraws() {
        val saved = Starters.all.mapIndexed { i, s -> SavedDesign(id = "s$i", prompt = s.title, design = s.design, createdAt = 0L) }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MakerTheme {
                LazyVerticalStaggeredGrid(StaggeredGridCells.Fixed(2), Modifier.fillMaxSize()) {
                    items(saved, key = { it.id }) { s -> SavedCard(s, 2, {}, {}, {}, WidgetTheme.oneui) }
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
    }
}
