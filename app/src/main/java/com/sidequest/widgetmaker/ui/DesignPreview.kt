package com.sidequest.widgetmaker.ui

import androidx.compose.foundation.Image
import com.sidequest.widgetmaker.R
import androidx.compose.runtime.remember
import com.sidequest.widgetmaker.spec.ArtNode
import com.sidequest.widgetmaker.spec.SleepNode
import com.sidequest.widgetmaker.spec.ChartNode
import com.sidequest.widgetmaker.spec.RingNode
import com.sidequest.widgetmaker.widget.ChartArt
import com.sidequest.widgetmaker.spec.TimerNode
import com.sidequest.widgetmaker.widget.accentColor
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidequest.widgetmaker.spec.ClockNode
import com.sidequest.widgetmaker.spec.ColumnNode
import com.sidequest.widgetmaker.spec.HAlign
import com.sidequest.widgetmaker.spec.IconNode
import com.sidequest.widgetmaker.spec.Node
import com.sidequest.widgetmaker.spec.ProgressNode
import com.sidequest.widgetmaker.spec.RowNode
import com.sidequest.widgetmaker.spec.SpacerNode
import com.sidequest.widgetmaker.spec.DividerNode
import com.sidequest.widgetmaker.spec.TextNode
import com.sidequest.widgetmaker.spec.Tokens
import com.sidequest.widgetmaker.spec.VAlign
import com.sidequest.widgetmaker.spec.Weight
import com.sidequest.widgetmaker.spec.Role
import com.sidequest.widgetmaker.spec.Look
import com.sidequest.widgetmaker.spec.Shapes
import com.sidequest.widgetmaker.spec.WidgetShape
import com.sidequest.widgetmaker.data.DesignStore
import com.sidequest.widgetmaker.widget.PanelArt
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.sidequest.widgetmaker.spec.Styler
import com.sidequest.widgetmaker.spec.WidgetDesign
import com.sidequest.widgetmaker.spec.WidgetTheme
import com.sidequest.widgetmaker.widget.AiWidget
import com.sidequest.widgetmaker.widget.Fit
import com.sidequest.widgetmaker.widget.iconRes
import com.sidequest.widgetmaker.widget.textColor
import kotlinx.coroutines.delay
import java.util.Calendar

/** Home-screen sizes to preview a design at (about 2x1, 2x2 and 4x2 cells). */
enum class PreviewSize(val label: String, val width: Dp, val height: Dp) {
    Small("2×1", 160.dp, 80.dp),
    Strip("4×1", 320.dp, 80.dp),
    Square("2×2", 160.dp, 160.dp),
    Tall("2×4", 160.dp, 320.dp),
    Wide("4×2", 320.dp, 160.dp),
    Large("4×4", 320.dp, 320.dp),
}

/**
 * The design drawn in the app, matching what the widget draws (same layout
 * rules, same live values), so you can see it before adding it.
 */
@Composable
fun DesignPreview(
    design: WidgetDesign,
    size: PreviewSize,
    theme: WidgetTheme,
    modifier: Modifier = Modifier,
    /** The look to preview; null for the saved one (Settings → Widget look). */
    look: Look? = null,
    shape: WidgetShape? = null,
) {
    val context = LocalContext.current
    // Shared by every preview and worked out off the main thread (see PreviewTokens).
    val tokens by PreviewTokens.state(context).collectAsState()
    // Read each time, so previews follow Settings → Widget look as soon as it changes.
    val usedLook = look ?: DesignStore(context).look
    val panel = Shapes.panel(size.width.value.toInt(), size.height.value.toInt(), design, usedLook, shape)
    // The same panel the home screen widget gets (in the preview, One UI assumes the middle of the screen).
    val art = remember(theme, panel, usedLook) { PanelArt.build(context, theme, panel, usedLook, null) }
    val k = Shapes.scale(panel, usedLook)
    val paint = PreviewPaint(art.styler, isSystemInDarkTheme(), k, usedLook.weatherIcons)
    // Shrinks to fit narrow screens, drawn exactly as at full size.
    BoxWithConstraints(modifier) {
        val scale = minOf(1f, maxWidth / size.width)
        Box(Modifier.size(size.width * scale, size.height * scale), contentAlignment = Alignment.Center) {
            Box(
                Modifier.requiredSize(size.width, size.height).graphicsLayer(scaleX = scale, scaleY = scale),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(panel.width.dp, panel.height.dp)) {
                    Image(
                        bitmap = art.fill.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                        colorFilter = if (art.tinted) ColorFilter.tint(paint(Role.Background, design.background)) else null,
                    )
                    design.fill?.let { fill ->
                        val (fraction, color) = com.sidequest.widgetmaker.spec.fillLevel(fill, tokens)
                        val level = remember(panel, fraction) { com.sidequest.widgetmaker.widget.FillArt.build(context, panel, fraction)?.asImageBitmap() }
                        level?.let {
                            Image(
                                it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds,
                                colorFilter = ColorFilter.tint(paint(color.first, color.second)),
                            )
                        }
                    }
                    art.edge?.let {
                        Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    }
                    Box(Modifier.fillMaxSize().padding(start = (panel.padH * k).dp, end = (panel.padH * k).dp, top = (panel.padTop * k).dp, bottom = (panel.padV * k).dp)) {
                        val fit = Fit((panel.width / k).toInt(), (panel.height / k).toInt(), tokens, inner = (panel.height / k).toInt() - panel.padTop - panel.padV)
                        if (fit.shows(design.root)) PreviewNode(design.root, tokens, paint, fit, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewNode(node: Node, tokens: Tokens, paint: PreviewPaint, fit: Fit, modifier: Modifier) {
    when (node) {
        is ColumnNode -> Column(
            modifier = container(modifier, paint, node.background, node.cornerRadius, node.padding)
                .then(if (node.scroll) Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()) else Modifier),
            horizontalAlignment = when (node.align) {
                HAlign.start -> Alignment.Start
                HAlign.center -> Alignment.CenterHorizontally
                HAlign.end -> Alignment.End
            },
            verticalArrangement = when (node.verticalAlign) {
                VAlign.top -> Arrangement.Top
                VAlign.center -> Arrangement.Center
                VAlign.bottom -> Arrangement.Bottom
            },
        ) { node.children.filter(fit::shows).forEach { ColumnChild(it, tokens, paint, fit) } }
        is RowNode -> Row(
            modifier = container(modifier, paint, node.background, node.cornerRadius, node.padding),
            horizontalArrangement = when (node.align) {
                HAlign.start -> Arrangement.Start
                HAlign.center -> Arrangement.Center
                HAlign.end -> Arrangement.End
            },
            verticalAlignment = when (node.verticalAlign) {
                VAlign.top -> Alignment.Top
                VAlign.center -> Alignment.CenterVertically
                VAlign.bottom -> Alignment.Bottom
            },
        ) { node.children.filter(fit::shows).forEach { RowChild(it, tokens, paint, fit) } }
        is TextNode -> Text(
            text = tokens.fill(node.text),
            modifier = modifier,
            color = textColor(node, tokens).let { (role, c) -> paint(role, c) },
            fontSize = (node.size * paint.k).sp,
            lineHeight = (node.size * paint.k * 1.2f).sp,
            fontWeight = weight(node.weight),
            textAlign = when (node.align) {
                HAlign.start -> TextAlign.Start
                HAlign.center -> TextAlign.Center
                HAlign.end -> TextAlign.End
            },
            maxLines = node.maxLines,
            overflow = TextOverflow.Ellipsis,
        )
        is ClockNode -> Text(
            text = android.text.format.DateFormat.format(node.format, Calendar.getInstance()).toString(),
            modifier = modifier,
            color = paint(Role.Text, node.color),
            fontSize = (node.size * paint.k).sp,
            lineHeight = (node.size * paint.k).sp,
            fontWeight = weight(node.weight),
            maxLines = 1,
        )
        is IconNode -> {
            val context = LocalContext.current
            val mc = if (paint.weatherIcons == com.sidequest.widgetmaker.spec.WeatherIcons.classic) null
            else com.sidequest.widgetmaker.widget.Meteocons.nameFor(node, tokens)
                ?.let { com.sidequest.widgetmaker.widget.Meteocons.frames(context, it) }?.takeIf { it.isNotEmpty() }
            if (mc != null) {
                // Animated as on the home screen: the current weather's frames, 10 a second.
                val animate = paint.weatherIcons == com.sidequest.widgetmaker.spec.WeatherIcons.animated &&
                    com.sidequest.widgetmaker.widget.Meteocons.isNow(node) && mc.size > 1
                var frame by remember(mc) { mutableStateOf(0) }
                if (animate) LaunchedEffect(mc) {
                    while (true) {
                        delay(com.sidequest.widgetmaker.widget.Meteocons.FRAME_MS.toLong())
                        frame = (frame + 1) % mc.size
                    }
                }
                Image(
                    painter = painterResource(mc[if (animate) frame else 0]),
                    contentDescription = null,
                    modifier = modifier.size((node.size * paint.k * com.sidequest.widgetmaker.widget.mcScale(node.size)).dp),
                )
            } else Image(
                painter = painterResource(iconRes(node, tokens)),
                contentDescription = null,
                modifier = modifier.size((node.size * paint.k).dp),
                colorFilter = ColorFilter.tint(accentColor(node.levelOf, node.levels, node.color, tokens).let { (r, c) -> paint(r, c) }),
            )
        }
        is ProgressNode -> LinearProgressIndicator(
            progress = { tokens.progress(node.value) },
            modifier = modifier.fillMaxWidth().height((node.height * paint.k).dp).clip(RoundedCornerShape((node.height * paint.k).dp)),
            color = accentColor(node.levelOf, node.levels, node.color, tokens).let { (r, c) -> paint(r, c) },
            trackColor = paint(Role.Track, node.trackColor),
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        is TimerNode -> Text(
            text = tokens.feeds["music.elapsed"] ?: "0:00",
            modifier = modifier,
            color = paint(Role.Text, node.color),
            fontSize = (node.size * paint.k).sp,
            fontWeight = weight(node.weight),
            maxLines = 1,
        )
        is ChartNode -> {
            val density = LocalContext.current.resources.displayMetrics.density
            val values = ChartArt.parse(tokens.fill(node.series))
            val bmp = remember(values, fit.width, node.height, paint.k) {
                ChartArt.line(values, (fit.width * density).toInt(), (node.height * paint.k * density).toInt(), 2.2f * density, node.min, node.max).asImageBitmap()
            }
            Image(bmp, null, modifier.fillMaxWidth().height((node.height * paint.k).dp), contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(paint(Role.Accent, node.color)))
        }
        is RingNode -> {
            @Suppress("NAME_SHADOWING") val node = node.fitted(fit.inner)
            val density = LocalContext.current.resources.displayMetrics.density
            val px = (node.size * paint.k * density).toInt()
            val thick = node.thickness * paint.k * density
            val fraction = tokens.progress(node.value)
            val track = remember(px, thick) { ChartArt.ring(px, thick, null).asImageBitmap() }
            val arc = remember(px, thick, fraction) { ChartArt.ring(px, thick, fraction).asImageBitmap() }
            val (role, color) = accentColor(node.levelOf, node.levels, node.color, tokens)
            Box(modifier.size((node.size * paint.k).dp), contentAlignment = Alignment.Center) {
                Image(track, null, Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(paint(Role.Track, node.trackColor)))
                Image(arc, null, Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(paint(role, color)))
                Text(tokens.fill(node.text), color = paint(Role.Text, node.textColor), fontSize = (node.textSize * paint.k).sp,
                    fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        is SleepNode -> {
            val bar = remember(tokens.feeds["health.sleep.stages"], node.height) {
                com.sidequest.widgetmaker.widget.SleepBar.draw(tokens.feeds["health.sleep.stages"], heightPx = node.height * 3).asImageBitmap()
            }
            Image(bar, null, modifier.fillMaxWidth().height((node.height * paint.k).dp), contentScale = ContentScale.FillBounds)
        }
        is ArtNode -> {
            val context = LocalContext.current
            val cover = remember { com.sidequest.widgetmaker.music.Music.art(context)?.asImageBitmap() }
            val shape = RoundedCornerShape((node.cornerRadius * paint.k).dp)
            if (cover != null) {
                Image(cover, null, modifier.size((node.size * paint.k).dp).clip(shape), contentScale = ContentScale.Crop)
            } else {
                Box(modifier.size((node.size * paint.k).dp).clip(shape).background(paint(Role.Container, "#26FFFFFF")), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.icon_music),
                        contentDescription = null,
                        modifier = Modifier.size((node.size * paint.k / 2).dp),
                        colorFilter = ColorFilter.tint(paint(Role.Accent, "#CCFFFFFF")),
                    )
                }
            }
        }
        is SpacerNode -> Spacer(if (node.grow) modifier else modifier.size((node.size * paint.k).dp))
        is DividerNode -> Box(modifier.fillMaxWidth().height(node.thickness.dp).background(paint(Role.Container, node.color)))
    }
}

@Composable
private fun ColumnScope.ColumnChild(node: Node, tokens: Tokens, paint: PreviewPaint, fit: Fit) =
    PreviewNode(node, tokens, paint, fit, if (node.grow) Modifier.weight(1f) else Modifier)

@Composable
private fun RowScope.RowChild(node: Node, tokens: Tokens, paint: PreviewPaint, fit: Fit) =
    PreviewNode(node, tokens, paint, fit, if (node.grow) Modifier.weight(1f) else Modifier)

private fun container(modifier: Modifier, paint: PreviewPaint, background: String?, radius: Int, padding: Int): Modifier {
    var m = modifier
    if (background != null) m = m.clip(RoundedCornerShape((radius * paint.k).dp)).background(paint(Role.Container, background))
    if (padding > 0) m = m.padding((padding * paint.k).dp)
    return m
}

/** Theme colors for the preview, in the app's current light or dark mode. */
private class PreviewPaint(
    val styler: Styler,
    val dark: Boolean,
    val k: Float = 1f,
    val weatherIcons: com.sidequest.widgetmaker.spec.WeatherIcons = com.sidequest.widgetmaker.spec.WeatherIcons.classic,
) {
    operator fun invoke(role: Role, design: String?) = Color(styler.argb(role, design, dark))
}

private fun weight(weight: Weight) = when (weight) {
    Weight.light -> FontWeight.Light
    Weight.normal -> FontWeight.Normal
    Weight.medium -> FontWeight.Medium
    Weight.bold -> FontWeight.Bold
}
