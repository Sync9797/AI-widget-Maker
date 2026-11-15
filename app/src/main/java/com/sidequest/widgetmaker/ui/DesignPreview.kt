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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.toArgb
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
    /** Devices this widget leaves out of its device batteries (its settings sheet). */
    hiddenDevices: Set<String> = emptySet(),
    /** Weather icons stay still (lists of previews, where dozens moving at once would be a lot). */
    still: Boolean = false,
    /** Told once the preview is fully drawn (its background ready), so a list can keep a picture of it. */
    onReady: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    // Shared by every preview and worked out off the main thread (see PreviewTokens).
    val shared by PreviewTokens.state(context).collectAsState()
    val tokens = if (hiddenDevices.isEmpty()) shared
    else shared.copy(feeds = com.sidequest.widgetmaker.feeds.DevicesMath.hide(shared.feeds, hiddenDevices))
    // Read each time, so previews follow Settings → Widget look as soon as it changes.
    val usedLook = look ?: DesignStore(context).look
    val panel = Shapes.panel(size.width.value.toInt(), size.height.value.toInt(), design, usedLook, shape)
    // The same panel the home screen widget gets (in the preview, One UI assumes the middle of the screen).
    val mood = com.sidequest.widgetmaker.spec.WeatherMood.forDesign(design, usedLook, tokens.feeds, System.currentTimeMillis())
    val sky = if (theme == WidgetTheme.timeOfDay) com.sidequest.widgetmaker.spec.Sky.of(tokens.feeds, System.currentTimeMillis()) else null
    // Its background (shape, blur, edge), shared with every preview that has the same one, and built off the
    // main thread so lists of previews open at once; a soft placeholder shows meanwhile.
    val dark = isSystemInDarkTheme()
    val artKey = PanelArtCache.Key(theme, panel, usedLook, mood, sky, dark)
    // produceState keeps its value when the key changes, so a preview given another design or theme would keep
    // the old background: start each key from its own cached art (or the placeholder) instead.
    val art by produceState(PanelArtCache.get(artKey), artKey) {
        value = PanelArtCache.get(artKey)
        if (value == null) value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            PanelArtCache.getOrBuild(context, artKey, tokens.feeds)
        }
    }
    val k = Shapes.scale(panel, usedLook)
    val icons = if (still && usedLook.weatherIcons.moves) com.sidequest.widgetmaker.spec.WeatherIcons.meteocons else usedLook.weatherIcons
    if (onReady != null && art != null) LaunchedEffect(art) { onReady() }
    // Shrinks to fit narrow screens, drawn exactly as at full size.
    BoxWithConstraints(modifier) {
        // Shrinks to the room it's given, across and (when limited, like in a widget's settings sheet) down.
        val scale = minOf(1f, maxWidth / size.width, if (constraints.hasBoundedHeight) maxHeight / size.height else 1f)
        Box(Modifier.size(size.width * scale, size.height * scale), contentAlignment = Alignment.Center) {
            Box(
                Modifier.requiredSize(size.width, size.height).graphicsLayer(scaleX = scale, scaleY = scale),
                contentAlignment = Alignment.Center,
            ) {
                val art = art ?: run {
                    Box(
                        Modifier.size(panel.width.dp, panel.height.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f), RoundedCornerShape(minOf(panel.radius, 28).dp)),
                    )
                    return@Box
                }
                val paint = PreviewPaint(art.styler, dark, k, icons)
                Box(Modifier.size(panel.width.dp, panel.height.dp)) {
                    Image(
                        bitmap = remember(art) { art.fill.asImageBitmap() },
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
                    art.edge?.let { edge ->
                        Image(remember(edge) { edge.asImageBitmap() }, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    }
                    Box(Modifier.fillMaxSize().padding(start = (panel.padH * k).dp, end = (panel.padH * k).dp, top = (panel.padTop * k).dp, bottom = (panel.padV * k).dp)) {
                        val fit = Fit((panel.width / k).toInt(), (panel.height / k).toInt(), tokens, inner = (panel.height / k).toInt() - panel.padTop - panel.padV)
                        val phoneText = remember { com.sidequest.widgetmaker.widget.PhoneText(context) }
                        // Sized to fit, as on the home screen (see Fitter).
                        val fitted = remember(design, panel, k, tokens) {
                            com.sidequest.widgetmaker.spec.Fitter.fit(
                                design.root, (panel.width - 2 * panel.padH).toFloat(), (panel.height - panel.padTop - panel.padV).toFloat(), k, tokens,
                                measure = phoneText, fontScale = phoneText.fontScale, shows = fit::shows,
                            )
                        }
                        val fittedPaint = if (fitted.k == k) paint else PreviewPaint(art.styler, paint.dark, fitted.k, icons)
                        if (fit.shows(fitted.root)) PreviewNode(fitted.root, tokens, fittedPaint, fit, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewNode(node: Node, tokens: Tokens, paint: PreviewPaint, fit: Fit, modifier: Modifier) {
    // A month: the rows and columns of text it lays out to, as on the home screen.
    if (node is com.sidequest.widgetmaker.spec.MonthNode) {
        PreviewNode(com.sidequest.widgetmaker.spec.MonthGrid.expand(node, tokens.now.toLocalDate(), tokens.feeds, tokens.locale), tokens, paint, fit, modifier)
        return
    }
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
            modifier = if (node.highlight) {
                modifier.background(textColor(node, tokens).let { (role, c) -> paint(role, c) }, RoundedCornerShape((6 * paint.k).dp))
                    .padding(horizontal = (2 * paint.k).dp, vertical = (1 * paint.k).dp)
            } else if (node.chip) {
                modifier.background(paint(Role.Container, "#262FA7B8"), RoundedCornerShape(50))
                    .padding(horizontal = (9 * paint.k).dp, vertical = (4 * paint.k).dp)
            } else modifier,
            color = textColor(node, tokens).let { (role, c) -> if (node.highlight) Color(com.sidequest.widgetmaker.spec.Styler.contrast(paint(role, c).toArgb())) else paint(role, c) },
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
            color = paint(if (node.accent) Role.Accent else Role.Text, node.color),
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
                // Animated as on the home screen: the current weather's frames, 10 a second
                // (now and then: 6 seconds of it, then 24 at rest on the first frame).
                val animate = paint.weatherIcons.moves &&
                    com.sidequest.widgetmaker.widget.Meteocons.isNow(node) && mc.size > 1
                val calm = paint.weatherIcons == com.sidequest.widgetmaker.spec.WeatherIcons.calm
                var frame by remember(mc) { mutableStateOf(0) }
                if (animate) LaunchedEffect(mc, calm) {
                    val step = com.sidequest.widgetmaker.widget.Meteocons.FRAME_MS.toLong()
                    val play = com.sidequest.widgetmaker.widget.Meteocons.CALM_PLAY_MS / step
                    while (true) {
                        repeat(if (calm) play.toInt() else Int.MAX_VALUE) {
                            delay(step)
                            frame = (frame + 1) % mc.size
                        }
                        frame = 0
                        delay(com.sidequest.widgetmaker.widget.Meteocons.CALM_PLAY_MS.toLong() * com.sidequest.widgetmaker.widget.Meteocons.CALM_STILLS)
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
        is ProgressNode -> if (node.capsule) {
            val h = (node.height * paint.k).dp
            val f = tokens.progress(node.value)
            val on = accentColor(node.levelOf, node.levels, node.color, tokens).let { (r, c) -> paint(r, c) }
            Box(modifier.width(h * com.sidequest.widgetmaker.widget.CAPSULE_WIDTH).height(h).clip(RoundedCornerShape(50)).background(paint(Role.Track, node.trackColor)), contentAlignment = Alignment.TopCenter) {
                if (f > 0f) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(h * f).clip(RoundedCornerShape(50)).background(on))
                node.text?.let {
                    Text(
                        tokens.fill(it), maxLines = 1,
                        modifier = Modifier.padding(top = (6 * paint.k).dp).background(paint(Role.Background, null), RoundedCornerShape(50))
                            .padding(horizontal = (6 * paint.k).dp, vertical = (2 * paint.k).dp),
                        color = paint(Role.Text, node.textColor), fontSize = (node.textSize * paint.k).sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
        } else if (node.segments > 0) {
            val filled = Math.round(tokens.progress(node.value) * node.segments)
            val on = accentColor(node.levelOf, node.levels, node.color, tokens).let { (r, c) -> paint(r, c) }
            val off = paint(Role.Track, node.trackColor)
            val gap = (1.5f * paint.k).dp
            if (node.vertical) {
                Column(modifier.width((node.height * 0.4f * paint.k).dp).height((node.height * paint.k).dp)) {
                    for (i in 0 until node.segments) {
                        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = gap).clip(RoundedCornerShape((3 * paint.k).dp))
                            .background(if (i >= node.segments - filled) on else off))
                    }
                }
            } else {
                Row(modifier.fillMaxWidth().height((node.height * paint.k).dp)) {
                    for (i in 0 until node.segments) {
                        Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = gap).clip(RoundedCornerShape((3 * paint.k).dp))
                            .background(if (i < filled) on else off))
                    }
                }
            }
        } else if (node.pill) {
            val h = (node.height * paint.k).dp
            Box(modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(h / 2)), contentAlignment = Alignment.CenterStart) {
                LinearProgressIndicator(
                    progress = { tokens.progress(node.value) },
                    modifier = Modifier.fillMaxSize(),
                    color = accentColor(node.levelOf, node.levels, node.color, tokens).let { (r, c) -> paint(r, c) },
                    trackColor = paint(Role.Track, node.trackColor),
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                )
                Row(Modifier.fillMaxSize().padding(horizontal = h * 0.4f), verticalAlignment = Alignment.CenterVertically) {
                    val iconNode = IconNode(node.icon ?: com.sidequest.widgetmaker.spec.IconName.battery, live = node.iconLive, size = (node.height * 0.55f).toInt())
                    if (node.icon != null || node.iconLive != null) Image(
                        painter = painterResource(iconRes(iconNode, tokens)),
                        contentDescription = null,
                        modifier = Modifier.size((iconNode.size * paint.k).dp),
                        colorFilter = ColorFilter.tint(Color(com.sidequest.widgetmaker.spec.Styler.contrast(paint(Role.Text, node.textColor).toArgb()))),
                    )
                    Spacer(Modifier.weight(1f))
                    node.text?.let {
                        Text(tokens.fill(it), maxLines = 1, color = paint(Role.Text, node.textColor), fontSize = (node.textSize * paint.k).sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else LinearProgressIndicator(
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
            val bmp = remember(values, fit.width, node.height, paint.k, node.style) {
                val w = (fit.width * density).toInt(); val h = (node.height * paint.k * density).toInt()
                (if (node.style == "ticks") ChartArt.ticks(values, w, h, 2.4f * density, min = node.min, max = node.max)
                else ChartArt.line(values, w, h, 2.2f * density, node.min, node.max)).asImageBitmap()
            }
            Image(bmp, null, modifier.fillMaxWidth().height((node.height * paint.k).dp), contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(paint(Role.Accent, node.color)))
        }
        is RingNode -> {
            @Suppress("NAME_SHADOWING") val node = node.fitted(fit.inner)
            val density = LocalContext.current.resources.displayMetrics.density
            val px = (node.size * paint.k * density).toInt()
            val thick = node.thickness * paint.k * density
            val fraction = node.fraction(tokens)
            val track = remember(px, thick, node.sweep) { ChartArt.ring(px, thick, null, node.sweep).asImageBitmap() }
            val arc = remember(px, thick, fraction, node.sweep) { ChartArt.ring(px, thick, fraction, node.sweep).asImageBitmap() }
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
        is com.sidequest.widgetmaker.spec.MonthNode -> Unit // Drawn as its rows and columns, above.
        is com.sidequest.widgetmaker.spec.AnalogNode -> {
            // The same clock as on the home screen, drawn here at the current time.
            val dial = paint(Role.Track, node.dialColor)
            val hand = paint(Role.Text, node.handColor)
            val accent = paint(Role.Accent, node.accent)
            val now = java.time.LocalTime.now()
            androidx.compose.foundation.Canvas(modifier.size((node.size * paint.k).dp)) {
                val r = size.minDimension / 2
                val c = center
                for (i in 0 until 60) {
                    val a = Math.toRadians(i * 6.0)
                    val big = i % 5 == 0
                    val r1 = r * 0.92f; val r2 = r * (if (big) 0.80f else 0.87f)
                    drawLine(
                        dial,
                        androidx.compose.ui.geometry.Offset(c.x + r1 * Math.sin(a).toFloat(), c.y - r1 * Math.cos(a).toFloat()),
                        androidx.compose.ui.geometry.Offset(c.x + r2 * Math.sin(a).toFloat(), c.y - r2 * Math.cos(a).toFloat()),
                        strokeWidth = r * (if (big) 0.04f else 0.016f),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
                fun handLine(angle: Double, length: Float, width: Float, color: Color) {
                    val a = Math.toRadians(angle)
                    drawLine(
                        color,
                        androidx.compose.ui.geometry.Offset(c.x - r * 0.12f * Math.sin(a).toFloat(), c.y + r * 0.12f * Math.cos(a).toFloat()),
                        androidx.compose.ui.geometry.Offset(c.x + r * length * Math.sin(a).toFloat(), c.y - r * length * Math.cos(a).toFloat()),
                        strokeWidth = r * width,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
                handLine((now.hour % 12 + now.minute / 60.0) * 30, 0.46f, 0.08f, hand)
                handLine(now.minute * 6.0, 0.72f, 0.05f, accent)
                drawCircle(accent, r * 0.05f, c)
            }
        }
        is com.sidequest.widgetmaker.spec.ScaleNode -> {
            val density = LocalContext.current.resources.displayMetrics.density
            val (wDp, hDp) = if (node.vertical) 30f * paint.k to (if (node.grow || node.length <= 0) fit.inner * 0.8f else node.length.toFloat()) * paint.k
            else fit.width.toFloat() to 22f * paint.k
            val sized = if (node.vertical) {
                if (node.grow || node.length <= 0) modifier.width(wDp.dp).fillMaxHeight() else modifier.size(wDp.dp, hDp.dp)
            } else modifier.fillMaxWidth().height(hDp.dp)
            val f = node.fraction(tokens)
            val ruler = remember(wDp, hDp, node.ticks, node.vertical) {
                ChartArt.scale((wDp * density).toInt(), (hDp * density).toInt(), node.ticks, node.vertical, density, false, null).asImageBitmap()
            }
            val mark = remember(wDp, hDp, node.ticks, node.vertical, f) {
                ChartArt.scale((wDp * density).toInt(), (hDp * density).toInt(), node.ticks, node.vertical, density, true, f).asImageBitmap()
            }
            Box(sized) {
                Image(ruler, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, colorFilter = ColorFilter.tint(paint(Role.Text, node.tickColor)))
                if (f != null) Image(mark, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, colorFilter = ColorFilter.tint(paint(Role.Accent, node.color)))
            }
        }
        is com.sidequest.widgetmaker.spec.AppIconNode -> {
            val context = LocalContext.current
            val px = (node.size * paint.k * context.resources.displayMetrics.density).toInt()
            val icon = remember(node.packageName, px) { com.sidequest.widgetmaker.widget.AppIcons.bitmap(context, node.packageName, px) }
            val size = (node.size * paint.k).dp
            if (icon != null) Image(icon.asImageBitmap(), contentDescription = null, modifier = modifier.size(size))
            else Image(
                painter = painterResource(R.drawable.icon_sparkle), contentDescription = null, modifier = modifier.size(size),
                colorFilter = ColorFilter.tint(paint(Role.Accent, "#FFFFFFFF")),
            )
        }
        is com.sidequest.widgetmaker.spec.MapNode -> {
            val context = LocalContext.current
            val stamp = tokens.feeds["map.traffic"]
            val picture = remember(stamp) {
                com.sidequest.widgetmaker.feeds.TrafficMap.file(context).takeIf { stamp != null && it.exists() }
                    ?.let { android.graphics.BitmapFactory.decodeFile(it.path) }
            }
            val sized = (if (node.grow) modifier.fillMaxWidth() else modifier.fillMaxWidth().height((node.height * paint.k).dp))
                .clip(RoundedCornerShape((node.cornerRadius * paint.k).dp))
            if (picture != null) {
                Image(picture.asImageBitmap(), contentDescription = "Traffic map", modifier = sized, contentScale = ContentScale.Crop)
            } else {
                Box(sized.background(paint(Role.Container, "#26FFFFFF")), contentAlignment = Alignment.Center) {
                    Text(
                        "Add a TomTom key and your home in Data sources",
                        color = paint(Role.Text, "#B3FFFFFF"),
                        fontSize = (12 * paint.k).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp),
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
