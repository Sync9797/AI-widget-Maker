package com.sidequest.widgetmaker.spec

/**
 * The ready-made designs, in the Expressive language: one hero per widget (a shape, a big number, a gauge),
 * tonal tiles for the details, pills for values, and one pop color for what's happening now. Colors here are
 * the Expressive blue the mockups use; each theme swaps them for its own (the phone's palette, frosted glass...).
 *
 * Every design is responsive: slim (one row), 2×2, wide and tall each show what fits. Listed by category, the
 * first of each being the one its menu shows.
 */
internal object StarterDesigns {
    private const val SOFT = "#FFEEF3FB"
    private const val INK = "#FF0D2142"
    private const val SUB = "#B30D2142"
    private const val TILE = "#FFD6E3F7"
    private const val STRONG = "#FF2F5C9E"
    private const val POP = "#FFFF9E7A"
    private const val LIGHT = "#FFEEF3FB"
    private const val LSUB = "#B3EEF3FB"

    /** A soft card. */
    private fun card(name: String, padding: Int, root: String, extra: String = "") =
        """{"name":"$name","background":"$SOFT","cornerRadius":28,"padding":$padding$extra,"root":$root}"""

    /** A hero shape, in the strong tone. */
    private fun hero(name: String, shape: String, padding: Int, root: String) =
        """{"name":"$name","background":"$STRONG","cornerRadius":28,"padding":$padding,"shape":"$shape","root":$root}"""

    /** Children with a small gap between them. */
    private fun gapped(parts: List<String>, gap: Int = 6) = parts.joinToString(""",{"type":"spacer","size":$gap},""")

    /** Forecast hour [n] as a pill tile, the first in the pop color (it's the soonest). Hours past 3 only when wide. */
    private fun hourPill(n: Int) = """
        {"type":"column","align":"center","grow":true,"background":"$TILE","cornerRadius":16,"padding":6,${if (n == 1) "\"tone\":\"pop\"," else ""}${if (n > 3) "\"show\":{\"minWidth\":250}," else ""}"children":[
         {"type":"text","text":"{weather.h$n.time}","size":11,"color":"$SUB","weight":"bold","align":"center","maxLines":1},
         {"type":"icon","name":"sun","live":"{weather.h$n.icon}","size":20,"color":"$POP"},
         {"type":"text","text":"{weather.h$n.temp}","size":15,"color":"$INK","weight":"medium","align":"center","maxLines":1}]}"""

    private fun hourPills(count: Int = 4) = """{"type":"row","grow":true,"children":[${gapped((1..count).map(::hourPill))}]}"""

    /** Forecast day [n] as a tile. */
    private fun dayTile(n: Int) = """
        {"type":"row","verticalAlign":"center","background":"$TILE","cornerRadius":16,"padding":8,"children":[
         {"type":"text","text":"{weather.d$n.day}","size":13,"color":"$INK","weight":"bold","grow":true,"maxLines":1},
         {"type":"icon","name":"sun","live":"{weather.d$n.icon}","size":18,"color":"$POP"},
         {"type":"text","text":"  {weather.d$n.high}","size":13,"color":"$INK","weight":"bold"},
         {"type":"text","text":"  {weather.d$n.low}","size":13,"color":"$SUB"}]}"""

    /**
     * Upcoming event [n] as a tile: its time in a chip (the first in the pop color), the title and its day. It opens
     * the event. [minHeight] hides it on small widgets.
     */
    private fun eventTile(n: Int, minHeight: Int = 0) = """
        {"type":"row","verticalAlign":"center","background":"$TILE","cornerRadius":16,"padding":8,"tap":"{calendar.$n.link}","show":{"minHeight":$minHeight,"ifSet":"{calendar.$n}"},"children":[
         {"type":"text","text":"{calendar.$n.time}","size":11,"color":"$INK","weight":"bold","maxLines":1,"chip":true,"tone":"${if (n == 1) "pop" else "strong"}"},
         {"type":"spacer","size":8},
         {"type":"column","grow":true,"children":[
          {"type":"text","text":"{calendar.$n}","size":13,"color":"$INK","weight":"bold","maxLines":1},
          {"type":"text","text":"{calendar.$n.day}","size":11,"color":"$SUB","maxLines":1}]}]}"""

    /** Events [from]..[to] as tiles, a gap between, the later ones only on taller widgets. */
    private fun eventTiles(from: Int, to: Int, tallFrom: Int = 3, tallAt: Int = 200) =
        gapped((from..to).map { eventTile(it, if (it >= tallFrom) tallAt + (it - tallFrom) * 50 else 0) })

    /** A round "+" in the pop color that starts a new event. */
    private const val ADD_EVENT = """{"type":"row","tone":"pop","cornerRadius":16,"padding":4,"tap":"calendar-new","children":[{"type":"text","text":" + ","size":16,"color":"#FF3A1608","weight":"bold","maxLines":1}]}"""

    /** A big date: the day of the month in a pop tile. */
    private fun dateBadge(size: Int) = """{"type":"row","tone":"pop","cornerRadius":40,"padding":${size / 4},"children":[{"type":"clock","format":"d","size":$size,"color":"#FF3A1608","weight":"medium"}]}"""

    private const val DEVICE_LEVELS = """"levels":[{"from":0,"color":"#FFFF5C5C"},{"from":20,"color":"#FFFFB74D"},{"from":40,"color":"#FF4CD964"}]"""

    /** Device [n]'s battery as a fixed-height pill: its icon and percent inside a bar filled to its level. */
    private fun deviceBar(n: Int, show: String) = """
        {"type":"column","show":{$show"ifSet":"{devices.$n.level}"},"children":[
         {"type":"progress","value":"{devices.$n.level}","height":34,"color":"#FF4CD964","trackColor":"$TILE","levelOf":"{devices.$n.level}",$DEVICE_LEVELS,
          "iconLive":"{devices.$n.icon}","text":"{devices.$n.pct}","textSize":15,"textColor":"$INK"},
         {"type":"spacer","size":8}]}"""

    /** Device [n]'s battery as a ring in a tile, its name under it. */
    private fun deviceRing(n: Int, show: String = "") = """
        {"type":"column","align":"center","grow":true,"background":"$TILE","cornerRadius":18,"padding":8,"show":{$show"ifSet":"{devices.$n.name}"},"children":[
         {"type":"ring","value":"{devices.$n.level}","text":"{devices.$n.num}","size":52,"thickness":7,"textSize":15,"color":"#FF4CD964","trackColor":"$SOFT","textColor":"$INK","levelOf":"{devices.$n.level}",$DEVICE_LEVELS},
         {"type":"text","text":"{devices.$n.name}","size":11,"color":"$SUB","weight":"bold","align":"center","maxLines":1,"show":{"minHeight":130}}]}"""

    /** A stat in a tile: a big number over its label. */
    private fun statTile(value: String, label: String, extra: String = "") = """
        {"type":"column","grow":true,"background":"$TILE","cornerRadius":18,"padding":10$extra,"children":[
         {"type":"text","text":"$value","size":22,"color":"$INK","weight":"light","maxLines":1},
         {"type":"text","text":"$label","size":11,"color":"$SUB","maxLines":1}]}"""

    private fun egg(n: Int, minHeight: Int) = """
        {"type":"column","background":"$TILE","cornerRadius":16,"padding":8,"show":{"minHeight":$minHeight,"ifSet":"{mq.egg.$n}"},"children":[
         {"type":"row","verticalAlign":"center","children":[
          {"type":"icon","name":"egg","size":16,"color":"$POP"},
          {"type":"text","text":"  {mq.egg.$n}","size":13,"color":"$INK","weight":"bold","grow":true,"maxLines":1},
          {"type":"text","text":"Hatch","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","tap":"{mq.egg.$n.hatch}","show":{"ifSet":"{mq.egg.$n.hatch}"}}]},
         {"type":"text","text":"{mq.egg.$n.left}","size":11,"color":"$SUB","maxLines":1},
         {"type":"spacer","size":4},
         {"type":"progress","value":"{mq.egg.$n.progress}","color":"$STRONG","trackColor":"$SOFT","height":8,"tone":"strong"}]}"""

    private fun adventure(n: Int, minHeight: Int) = """
        {"type":"column","background":"$TILE","cornerRadius":16,"padding":8,"show":{"minHeight":$minHeight,"ifSet":"{mq.adv.$n}"},"children":[
         {"type":"row","verticalAlign":"center","children":[
          {"type":"text","text":"{mq.adv.$n}","size":14,"color":"$INK","weight":"bold","maxLines":1,"grow":true},
          {"type":"text","text":"Claim","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","tap":"{mq.adv.$n.claim}","show":{"ifSet":"{mq.adv.$n.claim}"}}]},
         {"type":"text","text":"{mq.adv.$n.team} · {mq.adv.$n.left}","size":11,"color":"$SUB","maxLines":1},
         {"type":"spacer","size":4},
         {"type":"progress","value":"{mq.adv.$n.progress}","color":"$STRONG","trackColor":"$SOFT","height":8,"tone":"strong"}]}"""

    private fun quest(n: Int, minHeight: Int) = """
        {"type":"row","verticalAlign":"center","background":"$TILE","cornerRadius":16,"padding":6,"show":{"minHeight":$minHeight,"ifSet":"{mq.quest.$n}"},"children":[
         {"type":"row","tone":"pop","cornerRadius":14,"padding":5,"tap":"{mq.quest.$n.done}","children":[{"type":"icon","name":"check","size":18,"color":"#FF3A1608"}]},
         {"type":"spacer","size":10},
         {"type":"column","grow":true,"children":[
          {"type":"text","text":"{mq.quest.$n}","size":13,"color":"$INK","weight":"bold","maxLines":1},
          {"type":"text","text":"{mq.quest.$n.when}","size":11,"color":"$SUB","maxLines":1,"show":{"ifSet":"{mq.quest.$n.when}"}}]}]}"""

    private fun member(n: Int, minHeight: Int) = """
        {"type":"column","grow":true,"background":"$TILE","cornerRadius":16,"padding":8,"show":{"minHeight":$minHeight,"ifSet":"{mq.party.$n}"},"children":[
         {"type":"text","text":"{mq.party.$n}","size":13,"color":"$INK","weight":"bold","maxLines":1},
         {"type":"row","children":[{"type":"text","text":"{mq.party.$n.level}","size":10,"color":"$LIGHT","weight":"bold","chip":true,"tone":"strong","maxLines":1}]},
         {"type":"text","text":"{mq.party.$n.hp}","size":11,"color":"$SUB","maxLines":1,"show":{"minWidth":250}},
         {"type":"spacer","size":4},
         {"type":"progress","value":"{mq.party.$n.hpPct}","color":"$STRONG","trackColor":"$SOFT","height":8,"tone":"strong"}]}"""

    private fun bounty(n: Int, minHeight: Int) = """
        {"type":"column","show":{"minHeight":$minHeight,"ifSet":"{mq.bounty.$n}"},"children":[
         {"type":"spacer","size":6},
         {"type":"row","children":[
          {"type":"text","text":"{mq.bounty.$n}","size":12,"color":"$INK","weight":"bold","grow":true,"maxLines":1},
          {"type":"text","text":"{mq.bounty.$n.progress}","size":11,"color":"$SUB","show":{"minWidth":250}}]},
         {"type":"spacer","size":3},
         {"type":"progress","value":"{mq.bounty.$n.pct}","color":"$STRONG","trackColor":"$TILE","height":8,"tone":"strong"}]}"""

    private fun coin(symbol: String, minHeight: Int) = """
        {"type":"row","verticalAlign":"center","background":"$TILE","cornerRadius":16,"padding":8,"show":{"minHeight":$minHeight},"children":[
         {"type":"text","text":"$symbol","size":11,"color":"$LIGHT","weight":"bold","chip":true,"tone":"strong"},
         {"type":"spacer","size":8},
         {"type":"text","text":"{crypto.$symbol.price}","size":14,"color":"$INK","weight":"bold","grow":true,"maxLines":1},
         {"type":"text","text":"{crypto.$symbol.change}","size":12,"color":"$INK","trendOf":"{crypto.$symbol.change}","maxLines":1}]}"""

    private fun headline(n: Int, minHeight: Int) = """
        {"type":"column","grow":true,"background":"$TILE","cornerRadius":16,"padding":10,"show":{"minHeight":$minHeight,"ifSet":"{news.$n}"},"children":[
         {"type":"text","text":"{news.$n.source}","size":10,"color":"$SUB","weight":"bold","maxLines":1},
         {"type":"text","text":"{news.$n}","size":13,"color":"$INK","weight":"bold","maxLines":2}]}"""

    private fun event(n: Int, minHeight: Int) = """
        {"type":"row","verticalAlign":"center","background":"$TILE","cornerRadius":16,"padding":8,"show":{"minHeight":$minHeight,"ifSet":"{events.$n}"},"children":[
         {"type":"column","grow":true,"children":[
          {"type":"text","text":"{events.$n}","size":13,"color":"$INK","weight":"bold","maxLines":1},
          {"type":"text","text":"{events.$n.venue}","size":11,"color":"$SUB","maxLines":1}]},
         {"type":"text","text":"{events.$n.when}","size":11,"color":"$LIGHT","weight":"bold","chip":true,"tone":"strong","maxLines":1,"show":{"minWidth":250}}]}"""

    val SOURCES = listOf(
        // Everyday
        Triple(
            "Countdown", "Days until something you're looking forward to, big on a flower, its date in the pop color; the next ones when it's big. Add countdowns in Settings.",
            hero("Countdown", "flower", 14, """
            {"type":"column","align":"center","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","show":{"maxHeight":129},"children":[
              {"type":"text","text":"{countdown.1.days}","size":26,"color":"$LIGHT","weight":"light","maxLines":1},
              {"type":"text","text":" {countdown.1.unit}","size":11,"color":"$LSUB","maxLines":1}]},
             {"type":"column","align":"center","show":{"minHeight":130},"children":[
              {"type":"text","text":"{countdown.1}","size":11,"color":"$LSUB","weight":"bold","align":"center","maxLines":1},
              {"type":"text","text":"{countdown.1.days}","size":52,"color":"$LIGHT","weight":"light","align":"center","maxLines":1},
              {"type":"text","text":"{countdown.1.unit}","size":12,"color":"$LIGHT","weight":"medium","align":"center","maxLines":1},
              {"type":"spacer","size":4},
              {"type":"text","text":"{countdown.1.date}","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","maxLines":1}]},
             {"type":"column","align":"center","show":{"minHeight":250},"children":[
              {"type":"spacer","size":8},
              {"type":"text","text":"{countdown.2.text}","size":11,"color":"$LSUB","align":"center","maxLines":1,"show":{"ifSet":"{countdown.2}"}},
              {"type":"text","text":"{countdown.3.text}","size":11,"color":"$LSUB","align":"center","maxLines":1,"show":{"ifSet":"{countdown.3}"}}]}]}"""),
        ),
        Triple(
            "Daily briefing", "Changes with the day: your greeting over tiles for the weather, what's next (in the pop color) and your steps; commute and Charge when tall.",
            card("Daily briefing", 14, """
            {"type":"column","tap":"weather","children":[
             {"type":"row","verticalAlign":"center","grow":true,"show":{"maxHeight":129},"children":[
              {"type":"icon","name":"weather","size":30,"color":"$POP"},
              {"type":"text","text":" {weather.temp}","size":24,"color":"$INK","weight":"light","maxLines":1},
              {"type":"spacer","size":10},
              {"type":"text","text":"{calendar.1}","size":12,"color":"$SUB","maxLines":1,"grow":true,"show":{"ifSet":"{calendar.1}"}}]},
             {"type":"column","grow":true,"show":{"minHeight":130},"children":[
              {"type":"row","verticalAlign":"center","children":[
               {"type":"text","text":"{greeting}","size":18,"color":"$INK","weight":"light","maxLines":1,"grow":true},
               {"type":"clock","format":"EEE d","size":11,"color":"$LIGHT","weight":"bold","tone":"strong","show":{"minWidth":250}}]},
              {"type":"spacer","size":8},
              {"type":"row","grow":true,"children":[
               {"type":"column","grow":true,"background":"$TILE","cornerRadius":18,"padding":10,"children":[
                {"type":"icon","name":"weather","size":26,"color":"$POP"},
                {"type":"spacer","grow":true},
                {"type":"text","text":"{weather.temp}","size":24,"color":"$INK","weight":"light","maxLines":1},
                {"type":"text","text":"{weather.outlook}","size":10,"color":"$SUB","maxLines":2,"show":{"minHeight":160}}]},
               {"type":"spacer","size":6},
               {"type":"column","grow":true,"background":"$TILE","cornerRadius":18,"padding":10,"tone":"pop","children":[
                {"type":"column","show":{"ifSet":"{time.morning}"},"children":[
                 {"type":"text","text":"Next","size":10,"color":"$INK","weight":"bold","maxLines":1},
                 {"type":"text","text":"{calendar.1.when}","size":16,"color":"$INK","weight":"medium","maxLines":1,"show":{"ifSet":"{calendar.1}"}},
                 {"type":"text","text":"{calendar.1}","size":11,"color":"$INK","maxLines":2,"show":{"ifSet":"{calendar.1}"}},
                 {"type":"text","text":"Nothing planned","size":11,"color":"$INK","maxLines":1,"show":{"ifEmpty":"{calendar.1}"}}]},
                {"type":"column","show":{"ifEmpty":"{time.morning}"},"children":[
                 {"type":"text","text":"Tomorrow","size":10,"color":"$INK","weight":"bold","maxLines":1},
                 {"type":"text","text":"{calendar.tomorrow.when}","size":16,"color":"$INK","weight":"medium","maxLines":1,"show":{"ifSet":"{calendar.tomorrow}"}},
                 {"type":"text","text":"{calendar.tomorrow}","size":11,"color":"$INK","maxLines":2,"show":{"ifSet":"{calendar.tomorrow}"}},
                 {"type":"text","text":"Nothing planned","size":11,"color":"$INK","maxLines":1,"show":{"ifEmpty":"{calendar.tomorrow}"}}]}]},
               {"type":"spacer","size":6,"show":{"minWidth":250}},
               {"type":"column","grow":true,"background":"$TILE","cornerRadius":18,"padding":10,"show":{"minWidth":250},"children":[
                {"type":"ring","value":"{health.stepsPct}","text":"","size":30,"thickness":5,"color":"$STRONG","trackColor":"$SOFT","tone":"strong"},
                {"type":"spacer","grow":true},
                {"type":"text","text":"{health.stepsLeft.text}","size":11,"color":"$INK","weight":"bold","maxLines":2}]}]},
              {"type":"column","show":{"minHeight":250},"children":[
               {"type":"spacer","size":6},
               {"type":"row","verticalAlign":"center","background":"$TILE","cornerRadius":16,"padding":8,"show":{"ifSet":"{traffic.commute}"},"children":[
                {"type":"icon","name":"car","size":16,"color":"$STRONG","tone":"strong"},
                {"type":"text","text":"  {traffic.commute} to {traffic.to}","size":13,"color":"$INK","weight":"bold","maxLines":1,"grow":true},
                {"type":"text","text":"{traffic.delay}","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","maxLines":1,"show":{"ifSet":"{traffic.delay}"}}]},
               {"type":"spacer","size":6,"show":{"ifSet":"{health.charge}"}},
               {"type":"row","verticalAlign":"center","background":"$TILE","cornerRadius":16,"padding":8,"show":{"ifSet":"{health.charge}"},"children":[
                {"type":"icon","name":"bolt","size":16,"color":"$STRONG","tone":"strong"},
                {"type":"text","text":"  Charge {health.charge}","size":13,"color":"$INK","weight":"bold","maxLines":1,"grow":true},
                {"type":"text","text":"{health.charge.label}","size":11,"color":"$SUB","maxLines":1}]}]}]}]}"""),
        ),
        Triple(
            "Smart stack", "One widget that shows what matters now: your next event when it starts within the hour, what's playing, or else the weather.",
            card("Smart stack", 16, """
            {"type":"column","verticalAlign":"center","tap":"weather","children":[
             {"type":"column","show":{"ifSet":"{calendar.soon}"},"tap":"{calendar.1.link}","children":[
              {"type":"text","text":"{calendar.soon.in}","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","maxLines":1},
              {"type":"spacer","size":6},
              {"type":"text","text":"{calendar.1}","size":20,"color":"$INK","weight":"medium","maxLines":2},
              {"type":"text","text":"{calendar.1.where}","size":12,"color":"$SUB","maxLines":1,"show":{"minHeight":110,"ifSet":"{calendar.1.where}"}}]},
             {"type":"row","verticalAlign":"center","show":{"ifSet":"{music.playing}","ifEmpty":"{calendar.soon}"},"children":[
              {"type":"art","size":56,"cornerRadius":28,"show":{"minWidth":220}},
              {"type":"spacer","size":12,"show":{"minWidth":220}},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{music.title}","size":15,"color":"$INK","weight":"bold","maxLines":1},
               {"type":"text","text":"{music.artist}","size":12,"color":"$SUB","maxLines":1,"show":{"minHeight":100}}]},
              {"type":"spacer","size":8},
              {"type":"row","tone":"pop","cornerRadius":18,"padding":8,"tap":"music:toggle","children":[{"type":"icon","name":"play","live":"{music.playIcon}","size":22,"color":"#FF3A1608"}]}]},
             {"type":"column","show":{"ifEmpty":"{calendar.soon}{music.playing}"},"children":[
              {"type":"row","verticalAlign":"center","children":[
               {"type":"icon","name":"weather","size":40,"color":"$POP"},
               {"type":"text","text":" {weather.temp}","size":36,"color":"$INK","weight":"light","maxLines":1}]},
              {"type":"text","text":"{weather.outlook}","size":12,"color":"$SUB","maxLines":1,"show":{"minHeight":100}},
              {"type":"text","text":"↑{weather.high} ↓{weather.low}","size":12,"color":"$INK","weight":"bold","show":{"minHeight":130}}]},
             {"type":"column","show":{"minHeight":240},"children":[
              {"type":"spacer","size":10},
              {"type":"column","show":{"ifEmpty":"{calendar.soon}{music.playing}"},"children":[${hourPills()}]},
              {"type":"column","show":{"ifSet":"{calendar.soon}"},"children":[${eventTile(2)}]},
              {"type":"row","verticalAlign":"center","show":{"ifSet":"{music.playing}","ifEmpty":"{calendar.soon}"},"children":[
               {"type":"icon","name":"previous","size":22,"color":"$INK","tap":"music:prev"},
               {"type":"spacer","size":10},
               {"type":"progress","value":"{music.progress}","color":"$STRONG","trackColor":"$TILE","height":8,"grow":true,"tone":"strong"},
               {"type":"spacer","size":10},
               {"type":"icon","name":"next","size":22,"color":"$INK","tap":"music:next"}]}]}]}"""),
        ),
        Triple(
            "Claude", "A shortcut that opens the Claude app: its icon in one color (light by day, dark by night) on a flower. 1×1 works too.",
            hero("Claude", "flower", 12, """
            {"type":"column","align":"center","verticalAlign":"center","tap":"app:com.anthropic.claude","children":[
             {"type":"appicon","packageName":"com.anthropic.claude","size":40,"mono":true,"show":{"maxHeight":129}},
             {"type":"column","align":"center","show":{"minHeight":130},"children":[
              {"type":"appicon","packageName":"com.anthropic.claude","size":56,"mono":true},
              {"type":"spacer","size":8},
              {"type":"text","text":"Claude","size":17,"color":"$LIGHT","weight":"bold","align":"center","maxLines":1},
              {"type":"text","text":"New chat","size":12,"color":"$LSUB","align":"center","maxLines":1,"show":{"minHeight":200}}]}]}"""),
        ),
        Triple(
            "Gemini", "A shortcut that opens the Gemini app: its icon in one color (light by day, dark by night) on a cookie. 1×1 works too.",
            hero("Gemini", "cookie", 12, """
            {"type":"column","align":"center","verticalAlign":"center","tap":"app:com.google.android.apps.bard","children":[
             {"type":"appicon","packageName":"com.google.android.apps.bard","size":40,"mono":true,"show":{"maxHeight":129}},
             {"type":"column","align":"center","show":{"minHeight":130},"children":[
              {"type":"appicon","packageName":"com.google.android.apps.bard","size":56,"mono":true},
              {"type":"spacer","size":8},
              {"type":"text","text":"Gemini","size":17,"color":"$LIGHT","weight":"bold","align":"center","maxLines":1},
              {"type":"text","text":"Ask anything","size":12,"color":"$LSUB","align":"center","maxLines":1,"show":{"minHeight":200}}]}]}"""),
        ),

        // Clock & date
        Triple(
            "Stacked clock", "The hour over the minutes in big numbers on a cookie, with the day in the pop color; the weather under it when tall.",
            hero("Stacked clock", "cookie", 14, """
            {"type":"column","align":"center","verticalAlign":"center","tap":"clock","children":[
             {"type":"clock","format":"h:mm","size":18,"color":"$LIGHT","weight":"bold","show":{"maxHeight":129}},
             {"type":"column","align":"center","show":{"minHeight":130},"children":[
              {"type":"clock","format":"hh","size":46,"color":"$LIGHT","weight":"bold"},
              {"type":"clock","format":"mm","size":46,"color":"$LSUB","weight":"light"},
              {"type":"clock","format":"EEE d","size":13,"color":"$POP","weight":"bold","tone":"pop"}]},
             {"type":"row","verticalAlign":"center","show":{"minHeight":250,"ifSet":"{weather.temp}"},"children":[
              {"type":"icon","name":"weather","size":22,"color":"$POP"},
              {"type":"text","text":" {weather.temp}","size":13,"color":"$LIGHT","weight":"bold","maxLines":1}]}]}"""),
        ),
        Triple(
            "Big clock", "An analog clock with the date in a chip; the time in big numbers beside it when wide, and the weather and what's next in tiles when tall.",
            card("Big clock", 14, """
            {"type":"column","tap":"clock","children":[
             {"type":"row","verticalAlign":"center","grow":true,"show":{"maxHeight":129},"children":[
              {"type":"analog","size":58,"handColor":"$INK","accent":"$POP","dialColor":"$STRONG"},
              {"type":"spacer","size":12},
              {"type":"column","grow":true,"children":[
               {"type":"clock","format":"h:mm","size":28,"color":"$INK","weight":"light"},
               {"type":"clock","format":"EEE d","size":12,"color":"$POP","weight":"bold","tone":"pop"}]}]},
             {"type":"row","verticalAlign":"center","grow":true,"show":{"minHeight":130},"children":[
              {"type":"column","align":"center","verticalAlign":"center","grow":true,"children":[
               {"type":"analog","size":100,"handColor":"$INK","accent":"$POP","dialColor":"$STRONG"},
               {"type":"spacer","size":8},
               {"type":"row","tone":"strong","cornerRadius":14,"padding":4,"children":[{"type":"clock","format":" EEE d ","size":12,"color":"$LIGHT","weight":"bold"}]}]},
              {"type":"column","grow":true,"show":{"minWidth":250},"children":[
               {"type":"clock","format":"h:mm","size":44,"color":"$INK","weight":"light"},
               {"type":"clock","format":"EEEE","size":14,"color":"$SUB","weight":"medium"},
               {"type":"clock","format":"MMMM d","size":13,"color":"$SUB"}]}]},
             {"type":"row","show":{"minHeight":250},"children":[
              {"type":"column","grow":true,"background":"$TILE","cornerRadius":18,"padding":10,"children":[
               {"type":"icon","name":"weather","size":24,"color":"$POP"},
               {"type":"text","text":"{weather.temp}","size":22,"color":"$INK","weight":"light","maxLines":1},
               {"type":"text","text":"↑{weather.high} ↓{weather.low}","size":11,"color":"$SUB","maxLines":1}]},
              {"type":"spacer","size":6,"show":{"ifSet":"{calendar.1}"}},
              {"type":"column","grow":true,"background":"$TILE","cornerRadius":18,"padding":10,"tone":"pop","show":{"ifSet":"{calendar.1}"},"children":[
               {"type":"text","text":"Next","size":10,"color":"$INK","weight":"bold","maxLines":1},
               {"type":"text","text":"{calendar.1.when}","size":15,"color":"$INK","weight":"medium","maxLines":1},
               {"type":"text","text":"{calendar.1}","size":11,"color":"$INK","maxLines":1}]}]}]}"""),
        ),

        // Weather
        Triple(
            "Weather gauge", "Now on a gauge from today's low to its high, with the weather's animated icon, the sky in words and the chance of rain; the next hours in pills when tall.",
            card("Weather gauge", 16, """
            {"type":"column","tap":"weather","children":[
             {"type":"row","verticalAlign":"center","grow":true,"show":{"maxHeight":129},"children":[
              {"type":"icon","name":"weather","size":44,"color":"$POP"},
              {"type":"spacer","size":8},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{weather.temp}","size":26,"color":"$INK","weight":"light","maxLines":1},
               {"type":"text","text":"{weather.condition}","size":12,"color":"$SUB","maxLines":1}]}]},
             {"type":"column","grow":true,"show":{"minHeight":130},"children":[
              {"type":"row","verticalAlign":"center","children":[
               {"type":"icon","name":"weather","size":28,"color":"$POP"},
               {"type":"spacer","size":6},
               {"type":"text","text":"{weather.condition}","size":12,"color":"$STRONG","weight":"bold","maxLines":1,"grow":true,"tone":"strong"},
               {"type":"text","text":"Rain {weather.rain}","size":12,"color":"$SUB","weight":"bold","maxLines":1,"show":{"minWidth":250,"ifSet":"{weather.rain}"}}]},
              {"type":"row","grow":true,"align":"center","verticalAlign":"center","children":[
               {"type":"ring","value":"{weather.temp}","min":"{weather.low}","max":"{weather.high}","sweep":240,"text":"{weather.temp}","size":96,"thickness":12,"textSize":30,"color":"$POP","trackColor":"$TILE","textColor":"$INK"}]},
              {"type":"row","children":[
               {"type":"text","text":"{weather.low}","size":12,"color":"$INK","weight":"bold","maxLines":1,"grow":true},
               {"type":"text","text":"{weather.high}","size":12,"color":"$INK","weight":"bold","maxLines":1}]},
              {"type":"column","show":{"minHeight":250},"children":[{"type":"spacer","size":10},${hourPills()}]}]}]}"""),
        ),
        Triple(
            "Clock and weather", "The weather big beside its icon, the time, and the next hours as pill tiles (the soonest in the pop color); the next days in tiles when tall.",
            card("Clock and weather", 16, """
            {"type":"column","tap":"weather","children":[
             {"type":"row","verticalAlign":"center","grow":true,"show":{"maxHeight":129},"children":[
              {"type":"icon","name":"weather","size":36,"color":"$POP"},
              {"type":"text","text":" {weather.temp}","size":28,"color":"$INK","weight":"light","maxLines":1,"grow":true},
              {"type":"clock","format":"h:mm","size":20,"color":"$SUB","weight":"medium"}]},
             {"type":"column","grow":true,"show":{"minHeight":130},"children":[
              {"type":"row","verticalAlign":"top","children":[
               {"type":"column","grow":true,"children":[
                {"type":"text","text":"{weather.temp}","size":40,"color":"$INK","weight":"light","maxLines":1},
                {"type":"text","text":"{weather.condition}","size":12,"color":"$SUB","maxLines":1}]},
               {"type":"column","align":"end","children":[
                {"type":"icon","name":"weather","size":44,"color":"$POP"},
                {"type":"clock","format":"h:mm","size":14,"color":"$INK","weight":"bold"}]}]},
              {"type":"spacer","size":8},
              ${hourPills()}]},
             {"type":"column","show":{"minHeight":290},"children":[
              {"type":"spacer","size":8},
              ${gapped((1..3).map(::dayTile))}]}]}"""),
        ),
        Triple(
            "Weather forecast", "Like One UI's weather: now, rain or snow on the way (else today's high, low and feels-like) and your town at 2x2; the next hours when wider; the next 3 days when taller.",
            card("Weather forecast", 16, """
            {"type":"column","children":[
             {"type":"row","grow":true,"children":[
              {"type":"column","grow":true,"children":[
               {"type":"row","show":{"minHeight":150},"children":[
                {"type":"spacer","grow":true},
                {"type":"icon","name":"weather","size":56,"color":"$POP"},
                {"type":"spacer","size":20,"show":{"minWidth":250}}]},
               {"type":"spacer","grow":true},
               {"type":"text","text":"{weather.temp}","size":32,"color":"$INK","weight":"light","maxLines":1},
               {"type":"text","text":"{weather.event}","size":12,"color":"$INK","maxLines":1,"show":{"minHeight":100,"ifSet":"{weather.event}"}},
               {"type":"text","text":"↑{weather.high} / ↓{weather.low} · Feels {weather.feelsLike}","size":12,"color":"$INK","maxLines":1,"show":{"minHeight":100,"ifEmpty":"{weather.event}"}},
               {"type":"row","show":{"minHeight":120},"children":[
                {"type":"icon","name":"pin","size":12,"color":"$SUB"},
                {"type":"text","text":" {weather.place}","size":12,"color":"$SUB","maxLines":1}]},
               {"type":"scale","value":"{weather.temp}","min":"{weather.low}","max":"{weather.high}","vertical":false,"ticks":25,"color":"$POP","tickColor":"$SUB","show":{"minHeight":190,"maxWidth":249}}]},
              {"type":"spacer","size":16,"show":{"minWidth":250}},
              {"type":"column","grow":true,"show":{"minWidth":250},"verticalAlign":"center","children":[
               ${gapped((1..4).map { n -> """
               {"type":"row","verticalAlign":"center","background":"$TILE","cornerRadius":14,"padding":6,${if (n > 2) "\"show\":{\"minHeight\":140}," else ""}"children":[
                {"type":"text","text":"{weather.h$n.time}","size":12,"color":"$SUB","grow":true,"maxLines":1},
                {"type":"icon","name":"umbrella","size":11,"color":"$SUB"},
                {"type":"text","text":" {weather.h$n.rain}","size":12,"color":"$SUB","grow":true,"maxLines":1},
                {"type":"icon","name":"sun","live":"{weather.h$n.icon}","size":16,"color":"$POP"},
                {"type":"text","text":"{weather.h$n.temp}","size":13,"color":"$INK","weight":"bold","align":"end","grow":true}]}""" }, 4)}]}]},
             {"type":"column","show":{"minHeight":300},"children":[
              {"type":"spacer","size":10},
              {"type":"chart","series":"{weather.hours.temps}","style":"ticks","height":34,"color":"$STRONG"},
              {"type":"spacer","size":8},
              ${gapped((1..3).map(::dayTile))}]},
             {"type":"row","show":{"minHeight":380},"children":[
              {"type":"spacer","size":8},
              ${statTile("{weather.feelsLike}", "Feels like")},{"type":"spacer","size":6},
              ${statTile("{weather.wind}", "Wind")},{"type":"spacer","size":6},
              ${statTile("{weather.rain}", "Rain")}]}]}"""),
        ),

        // Calendar
        Triple(
            "Upcoming events", "What's next: today's date big beside your events as tiles, the first in the pop color, with a + to add one. More events the taller it is.",
            card("Upcoming events", 14, """
            {"type":"column","tap":"calendar","children":[
             {"type":"row","verticalAlign":"center","grow":true,"show":{"maxHeight":129},"children":[
              ${dateBadge(18)},
              {"type":"spacer","size":10},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{calendar.1}","size":14,"color":"$INK","weight":"bold","maxLines":1,"show":{"ifSet":"{calendar.1}"}},
               {"type":"text","text":"{calendar.1.time} · {calendar.1.day}","size":11,"color":"$SUB","maxLines":1,"show":{"ifSet":"{calendar.1}"}},
               {"type":"text","text":"No events","size":13,"color":"$SUB","show":{"ifEmpty":"{calendar.1}"}}]},
              $ADD_EVENT]},
             {"type":"row","grow":true,"show":{"minHeight":130},"children":[
              {"type":"column","show":{"minWidth":250},"children":[
               {"type":"clock","format":"d","size":56,"color":"$INK","weight":"light"},
               {"type":"row","tone":"strong","cornerRadius":12,"padding":4,"children":[{"type":"clock","format":" MMM · EEE ","size":11,"color":"$LIGHT","weight":"bold"}]},
               {"type":"spacer","grow":true},
               $ADD_EVENT]},
              {"type":"spacer","size":14,"show":{"minWidth":250}},
              {"type":"column","grow":true,"children":[
               {"type":"row","verticalAlign":"center","show":{"maxWidth":249},"children":[
                {"type":"clock","format":"d","size":30,"color":"$INK","weight":"light"},
                {"type":"spacer","size":6},
                {"type":"clock","format":"MMM","size":12,"color":"$SUB","weight":"bold","grow":true},
                $ADD_EVENT]},
               {"type":"spacer","size":6,"show":{"maxWidth":249}},
               {"type":"text","text":"No events coming up","size":13,"color":"$SUB","show":{"ifEmpty":"{calendar.1}"}},
               ${eventTiles(1, 5)}]}]}]}"""),
        ),
        Triple(
            "Today's date", "The date: the day in a pop tile beside the weekday and month when slim, big in the middle when square, and what's coming up in tiles when tall.",
            card("Today's date", 14, """
            {"type":"column","tap":"calendar","children":[
             {"type":"row","verticalAlign":"center","grow":true,"show":{"maxHeight":129},"children":[
              ${dateBadge(22)},
              {"type":"spacer","size":12},
              {"type":"column","grow":true,"children":[
               {"type":"clock","format":"EEEE","size":20,"color":"$INK","weight":"light"},
               {"type":"clock","format":"MMMM yyyy","size":12,"color":"$SUB"}]}]},
             {"type":"column","align":"center","verticalAlign":"center","grow":true,"show":{"minHeight":130,"maxHeight":249},"children":[
              ${dateBadge(44)},
              {"type":"spacer","size":8},
              {"type":"clock","format":"EEEE","size":18,"color":"$INK","weight":"light"},
              {"type":"clock","format":"MMMM","size":12,"color":"$SUB","weight":"bold"}]},
             {"type":"column","grow":true,"show":{"minHeight":250},"children":[
              {"type":"row","verticalAlign":"center","children":[
               ${dateBadge(32)},
               {"type":"spacer","size":12},
               {"type":"column","grow":true,"children":[
                {"type":"clock","format":"EEEE","size":17,"color":"$INK","weight":"light"},
                {"type":"clock","format":"MMMM yyyy","size":12,"color":"$SUB"}]}]},
              {"type":"spacer","size":10},
              ${eventTiles(1, 3, tallFrom = 3, tallAt = 290)}]}]}"""),
        ),
        Triple(
            "Month and agenda", "Wide: this month beside what's next in tiles, with a + to add an event. Tall: the month. 2×2: today's date big. Slim: the date and your next event.",
            card("Month and agenda", 14, """
            {"type":"column","tap":"calendar","children":[
             {"type":"row","verticalAlign":"center","grow":true,"show":{"maxHeight":129},"children":[
              ${dateBadge(18)},
              {"type":"spacer","size":10},
              {"type":"column","grow":true,"children":[
               {"type":"clock","format":"EEE, MMM d","size":14,"color":"$INK","weight":"bold"},
               {"type":"text","text":"{calendar.1}","size":12,"color":"$SUB","maxLines":1,"show":{"ifSet":"{calendar.1}"}},
               {"type":"text","text":"No events","size":12,"color":"$SUB","show":{"ifEmpty":"{calendar.1}"}}]}]},
             {"type":"column","align":"center","verticalAlign":"center","grow":true,"show":{"minHeight":130,"maxWidth":299,"maxHeight":299},"children":[
              ${dateBadge(44)},
              {"type":"spacer","size":8},
              {"type":"clock","format":"EEEE","size":16,"color":"$INK","weight":"light"},
              {"type":"clock","format":"MMMM yyyy","size":12,"color":"$SUB","weight":"bold"}]},
             {"type":"month","grow":true,"size":12,"color":"$INK","accent":"$POP","show":{"minHeight":300,"maxWidth":299}},
             {"type":"row","verticalAlign":"top","grow":true,"show":{"minWidth":300,"minHeight":130},"children":[
              {"type":"month","grow":true,"header":false,"size":12,"color":"$INK","accent":"$POP"},
              {"type":"spacer","size":14},
              {"type":"column","grow":true,"children":[
               {"type":"row","verticalAlign":"center","children":[
                {"type":"clock","format":"MMMM d","size":16,"color":"$INK","weight":"bold","grow":true},
                $ADD_EVENT]},
               {"type":"spacer","size":6},
               {"type":"text","text":"No events","size":13,"color":"$SUB","show":{"ifEmpty":"{calendar.1}"}},
               ${eventTiles(1, 5, tallFrom = 3, tallAt = 230)}]}]}]}"""),
        ),
        Triple(
            "Month", "This month, today in the pop color and days with events marked. Two months when wide; week numbers when big.",
            card("Month", 12, """
            {"type":"column","tap":"calendar","children":[
             {"type":"month","grow":true,"size":11,"color":"$INK","accent":"$POP","show":{"minHeight":130,"maxWidth":299}},
             {"type":"month","grow":true,"months":2,"size":13,"color":"$INK","accent":"$POP","show":{"minWidth":300,"minHeight":130,"maxHeight":299}},
             {"type":"month","grow":true,"weekNumbers":true,"size":17,"color":"$INK","accent":"$POP","show":{"minWidth":300,"minHeight":300}},
             {"type":"row","verticalAlign":"center","grow":true,"show":{"maxHeight":129},"children":[
              ${dateBadge(18)},
              {"type":"spacer","size":10},
              {"type":"clock","format":"MMMM","size":20,"color":"$INK","weight":"light","grow":true},
              {"type":"clock","format":"EEE","size":13,"color":"$SUB","weight":"bold","show":{"minWidth":250}}]}]}"""),
        ),

        // Health & fitness
        Triple(
            "Steps", "Today's steps big inside a ring to your goal, on a cookie; what's left when tall. Pick the Pill shape for a slim oval.",
            hero("Steps", "cookie", 14, """
            {"type":"column","align":"center","verticalAlign":"center","tap":"health","children":[
             {"type":"row","verticalAlign":"center","show":{"maxHeight":129},"children":[
              {"type":"ring","value":"{health.stepsPct}","text":"","size":34,"thickness":6,"color":"$POP","trackColor":"#FF22477F"},
              {"type":"text","text":" {health.steps}","size":22,"color":"$LIGHT","weight":"light","maxLines":1}]},
             {"type":"column","align":"center","show":{"minHeight":130},"children":[
              {"type":"ring","value":"{health.stepsPct}","text":"{health.steps}","size":116,"thickness":10,"textSize":24,"color":"$POP","trackColor":"#FF22477F","textColor":"$LIGHT"},
              {"type":"text","text":"of {health.stepGoal}","size":11,"color":"$LSUB","align":"center","maxLines":1}]},
             {"type":"text","text":"{health.stepsLeft.text}","size":12,"color":"$LIGHT","weight":"bold","align":"center","maxLines":1,"show":{"minHeight":250}}]}"""),
        ),
        Triple(
            "Activity", "Today in tiles: steps, kcal, heart rate and effort, your last workout in a pop chip when wide, and last night's sleep when tall.",
            card("Activity", 14, """
            {"type":"column","tap":"health","children":[
             {"type":"row","verticalAlign":"center","grow":true,"show":{"maxHeight":129},"children":[
              {"type":"ring","value":"{health.stepsPct}","text":"","size":34,"thickness":6,"color":"$STRONG","trackColor":"$TILE","tone":"strong"},
              {"type":"text","text":" {health.steps}","size":22,"color":"$INK","weight":"light","maxLines":1,"grow":true},
              {"type":"text","text":"{health.kcal} kcal","size":12,"color":"$SUB","maxLines":1,"show":{"minWidth":250}}]},
             {"type":"column","grow":true,"show":{"minHeight":130},"children":[
              {"type":"row","verticalAlign":"center","children":[
               {"type":"text","text":"Today","size":18,"color":"$INK","weight":"light","grow":true,"maxLines":1},
               {"type":"text","text":"{health.workout} · {health.workout.duration}","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","maxLines":1,"show":{"minWidth":250,"ifSet":"{health.workout}"}}]},
              {"type":"spacer","size":8},
              {"type":"row","grow":true,"children":[
               ${statTile("{health.steps}", "steps")},{"type":"spacer","size":6},
               ${statTile("{health.kcal}", "kcal")},
               {"type":"spacer","size":6,"show":{"minWidth":250}},
               ${statTile("{health.heart}", "bpm", ",\"show\":{\"minWidth\":250}")},
               {"type":"spacer","size":6,"show":{"minWidth":250}},
               ${statTile("{health.effort}", "effort", ",\"show\":{\"minWidth\":250}")}]},
              {"type":"spacer","size":6,"show":{"minHeight":170}},
              {"type":"progress","value":"{health.stepsPct}","color":"$STRONG","trackColor":"$TILE","height":10,"tone":"strong","show":{"minHeight":170}}]},
             {"type":"column","show":{"minHeight":250},"children":[
              {"type":"spacer","size":8},
              {"type":"column","background":"$TILE","cornerRadius":18,"padding":10,"children":[
               {"type":"row","verticalAlign":"center","children":[
                {"type":"icon","name":"moon","size":16,"color":"$STRONG","tone":"strong"},
                {"type":"text","text":"  Slept {health.sleep}","size":13,"color":"$INK","weight":"bold","grow":true,"maxLines":1},
                {"type":"text","text":"Deep {health.sleep.deep}","size":11,"color":"$SUB","maxLines":1}]},
               {"type":"spacer","size":6},
               {"type":"sleep","height":12}]}]}]}"""),
        ),
        Triple(
            "Sleep", "Last night's sleep big, its stages as a bar, and your week as capsules when wide. From NOOP, Samsung Health and others.",
            card("Sleep", 16, """
            {"type":"column","tap":"health","children":[
             {"type":"row","verticalAlign":"center","grow":true,"show":{"maxHeight":129},"children":[
              {"type":"icon","name":"moon","size":24,"color":"$STRONG","tone":"strong"},
              {"type":"text","text":"  {health.sleep}","size":24,"color":"$INK","weight":"light","maxLines":1,"grow":true},
              {"type":"text","text":"{health.sleep.vsAvg}","size":11,"color":"$LIGHT","weight":"bold","chip":true,"tone":"strong","maxLines":1,"show":{"minWidth":250}}]},
             {"type":"row","grow":true,"show":{"minHeight":130},"children":[
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"Last night","size":11,"color":"$STRONG","weight":"bold","tone":"strong","maxLines":1},
               {"type":"text","text":"{health.sleep}","size":36,"color":"$INK","weight":"light","maxLines":1},
               {"type":"text","text":"{health.sleep.bed} – {health.sleep.wake}","size":11,"color":"$SUB","maxLines":1},
               {"type":"spacer","grow":true},
               {"type":"sleep","height":14},
               {"type":"spacer","size":6},
               {"type":"row","children":[
                {"type":"text","text":"Deep {health.sleep.deep}","size":11,"color":"$INK","weight":"bold","grow":true,"maxLines":1},
                {"type":"text","text":"REM {health.sleep.rem}","size":11,"color":"$INK","weight":"bold","maxLines":1}]}]},
              {"type":"spacer","size":14,"show":{"minWidth":300}},
              {"type":"row","verticalAlign":"bottom","show":{"minWidth":300},"children":[
               ${gapped((7 downTo 1).map { n -> """{"type":"column","align":"center","children":[{"type":"progress","value":"{health.sleep.n$n.pct}","vertical":true,"height":52,"color":"${if (n == 1) POP else STRONG}","trackColor":"$TILE","tone":"${if (n == 1) "pop" else "strong"}"},{"type":"text","text":"{health.sleep.n$n.day}","size":9,"color":"$SUB","weight":"bold","maxLines":1}]}""" }, 4)}]}]},
             {"type":"row","show":{"minHeight":250},"children":[
              {"type":"spacer","size":8},
              ${statTile("{health.hrv}", "HRV")},{"type":"spacer","size":6},
              ${statTile("{health.restingHeart}", "resting bpm")},{"type":"spacer","size":6},
              ${statTile("{health.sleep.avg}", "week average")}]}]}"""),
        ),
        Triple(
            "Heart rate", "Your heart rate now, big, with today's low and high in a pop chip and the day as a line chart. From NOOP, Samsung Health and others via Health Connect.",
            card("Heart rate", 16, """
            {"type":"column","tap":"health","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"heart","size":22,"color":"$POP"},
              {"type":"text","text":" {health.heart.last}","size":30,"color":"$INK","weight":"light","maxLines":1},
              {"type":"text","text":" bpm","size":12,"color":"$SUB","maxLines":1,"grow":true},
              {"type":"text","text":"{health.heartMin}–{health.heartMax}","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","maxLines":1,"show":{"minWidth":200}}]},
             {"type":"column","grow":true,"show":{"minHeight":130},"children":[
              {"type":"spacer","size":8},
              {"type":"column","grow":true,"background":"$TILE","cornerRadius":18,"padding":8,"children":[
               {"type":"chart","series":"{health.heart.series}","height":60,"color":"$STRONG","grow":true},
               {"type":"row","children":[
                {"type":"text","text":"{health.heart.start}","size":10,"color":"$SUB","grow":true,"maxLines":1},
                {"type":"text","text":"{health.heart.mid}","size":10,"color":"$SUB","grow":true,"align":"center","maxLines":1,"show":{"minWidth":250}},
                {"type":"text","text":"{health.heart.end}","size":10,"color":"$SUB","maxLines":1}]}]}]},
             {"type":"row","show":{"minHeight":250},"children":[
              {"type":"spacer","size":8},
              ${statTile("{health.restingHeart}", "resting")},{"type":"spacer","size":6},
              ${statTile("{health.hrv}", "HRV")}]}]}"""),
        ),
        Triple(
            "NOOP recovery", "Charge (estimated from HRV and resting heart rate) on a gauge, effort beside it; HRV, sleep, SpO2 and more in tiles as it grows. Turn on Health Connect in NOOP.",
            card("NOOP recovery", 16, """
            {"type":"column","tap":"health","children":[
             {"type":"row","verticalAlign":"center","grow":true,"show":{"maxHeight":129},"children":[
              {"type":"ring","value":"{health.charge}","text":"{health.charge}","size":46,"thickness":6,"textSize":13,"color":"#FF4CD964","trackColor":"$TILE","textColor":"$INK","levelOf":"{health.charge}","levels":[{"from":0,"color":"#FFFF5C5C"},{"from":34,"color":"#FFFFC857"},{"from":67,"color":"#FF4CD964"}]},
              {"type":"spacer","size":10},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"Charge","size":14,"color":"$INK","weight":"bold","maxLines":1},
               {"type":"text","text":"{health.charge.label}","size":11,"color":"$SUB","maxLines":1}]}]},
             {"type":"column","grow":true,"show":{"minHeight":130},"children":[
              {"type":"row","grow":true,"verticalAlign":"center","children":[
               {"type":"column","align":"center","grow":true,"children":[
                {"type":"ring","value":"{health.charge}","text":"{health.charge}","sweep":240,"size":92,"thickness":11,"textSize":26,"color":"#FF4CD964","trackColor":"$TILE","textColor":"$INK","levelOf":"{health.charge}","levels":[{"from":0,"color":"#FFFF5C5C"},{"from":34,"color":"#FFFFC857"},{"from":67,"color":"#FF4CD964"}]},
                {"type":"text","text":"{health.charge.label}","size":11,"color":"$INK","weight":"bold","align":"center","maxLines":1}]},
               {"type":"column","grow":true,"show":{"minWidth":250},"children":[
                ${statTile("{health.effort}", "effort")},{"type":"spacer","size":6},
                ${statTile("{health.sleep}", "sleep")}]}]},
              {"type":"column","show":{"minHeight":180,"maxWidth":249},"children":[
               {"type":"spacer","size":6},
               {"type":"text","text":"Effort {health.effort}","size":12,"color":"$LIGHT","weight":"bold","chip":true,"tone":"strong","maxLines":1}]}]},
             {"type":"row","show":{"minHeight":250},"children":[
              {"type":"spacer","size":8},
              ${statTile("{health.hrv}", "HRV")},{"type":"spacer","size":6},
              ${statTile("{health.restingHeart}", "resting")},{"type":"spacer","size":6},
              ${statTile("{health.spo2}", "SpO2")}]},
             {"type":"row","show":{"minHeight":320},"children":[
              {"type":"spacer","size":6},
              ${statTile("{health.respRate}", "breaths/min")},{"type":"spacer","size":6},
              ${statTile("{health.vo2max}", "VO2 max")},{"type":"spacer","size":6},
              ${statTile("{health.sleep.bed}–{health.sleep.wake}", "in bed")}]}]}"""),
        ),
        Triple(
            "NOOP rings", "Charge, Effort and Rest as rings in tiles, like NOOP's: NOOP's own scores straight from not-NOOP; with NOOP via Health Connect, estimates (Rest is last night's sleep against 8 hours).",
            card("NOOP rings", 12, """
            {"type":"column","tap":"health","children":[
             {"type":"row","grow":true,"children":[
              ${gapped(listOf(
                Triple("{health.charge}", "{health.charge}", "Charge") to ""","levelOf":"{health.charge}","levels":[{"from":0,"color":"#FFFF5C5C"},{"from":34,"color":"#FFFFC857"},{"from":67,"color":"#FF4CD964"}]""",
                Triple("{health.effort.pct}", "{health.effort}", "Effort") to ""","tone":"strong"""",
                Triple("{health.rest}", "{health.rest}", "Rest") to "",
              ).map { (t, extra) -> """
              {"type":"column","align":"center","verticalAlign":"center","grow":true,"background":"$TILE","cornerRadius":18,"padding":8,"children":[
               {"type":"ring","value":"${t.first}","text":"${t.second}","size":58,"thickness":7,"textSize":15,"color":"$POP","trackColor":"$SOFT","textColor":"$INK"$extra},
               {"type":"text","text":"${t.third}","size":11,"color":"$SUB","weight":"bold","align":"center","maxLines":1,"show":{"minHeight":130}}]}""" })}]},
             {"type":"text","text":"Slept {health.sleep} · {health.charge.label}","size":12,"color":"$INK","weight":"bold","align":"center","maxLines":1,"show":{"minHeight":200}}]}"""),
        ),
        Triple(
            "Stress", "Today's stress from not-NOOP on NOOP's 0-3 scale, big, with the day's curve, peak and average in tiles when there's room.",
            card("Stress", 16, """
            {"type":"column","tap":"health","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"text","text":"{noop.stress}","size":32,"color":"$INK","weight":"light","maxLines":1},
              {"type":"spacer","size":8},
              {"type":"text","text":"{noop.stress.label}","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","maxLines":1},
              {"type":"spacer","grow":true}]},
             {"type":"progress","value":"{noop.stress.pct}","color":"$STRONG","trackColor":"$TILE","height":10,"tone":"strong","show":{"maxHeight":129}},
             {"type":"column","grow":true,"show":{"minHeight":130},"children":[
              {"type":"spacer","size":8},
              {"type":"column","grow":true,"background":"$TILE","cornerRadius":18,"padding":8,"children":[
               {"type":"chart","series":"{noop.stress.series}","height":56,"min":0,"max":300,"color":"$STRONG","grow":true},
               {"type":"row","children":[
                {"type":"text","text":"{noop.stress.start}","size":10,"color":"$SUB","grow":true,"maxLines":1},
                {"type":"text","text":"{noop.stress.end}","size":10,"color":"$SUB","maxLines":1}]}]}]},
             {"type":"row","show":{"minHeight":220},"children":[
              {"type":"spacer","size":8},
              ${statTile("{noop.stress.peak}", "peak at {noop.stress.peakAt}")},{"type":"spacer","size":6},
              ${statTile("{noop.stress.avg}", "average")}]}]}"""),
        ),
        Triple(
            "Coach brief", "not-NOOP's daily Coach brief, point by point in tiles; scroll it to read it all. Turn the brief on in not-NOOP.",
            card("Coach brief", 14, """
            {"type":"column","tap":"health","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"sparkle","size":18,"color":"$POP"},
              {"type":"text","text":" {noop.coach.title}","size":15,"color":"$INK","weight":"bold","maxLines":1,"grow":true},
              {"type":"text","text":"{noop.coach.time}","size":10,"color":"$LIGHT","weight":"bold","chip":true,"tone":"strong","maxLines":1,"show":{"minWidth":250}}]},
             {"type":"text","text":"{noop.coach.1}","size":12,"color":"$SUB","maxLines":1,"show":{"maxHeight":129}},
             {"type":"column","grow":true,"scroll":true,"show":{"minHeight":130},"children":[
              ${(1..12).joinToString(",") { n -> """{"type":"column","show":{"ifSet":"{noop.coach.$n}"},"children":[{"type":"spacer","size":6},{"type":"column","background":"$TILE","cornerRadius":14,"padding":8,"children":[{"type":"text","text":"{noop.coach.$n}","size":12,"color":"$INK","maxLines":6}]}]}""" }}]},
             {"type":"text","text":"Turn on the brief in not-NOOP","size":12,"color":"$SUB","show":{"ifEmpty":"{noop.coach}"}}]}"""),
        ),

        // Music
        Triple(
            "Now playing", "The album art round beside the song and artist, a progress pill and the play button in the pop color; big art when tall. Allow music access in Settings.",
            card("Now playing", 14, """
            {"type":"column","tap":"music","children":[
             {"type":"row","verticalAlign":"center","grow":true,"show":{"maxHeight":129},"children":[
              {"type":"art","size":44,"cornerRadius":22},
              {"type":"spacer","size":10},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{music.title}","size":13,"color":"$INK","weight":"bold","maxLines":1},
               {"type":"text","text":"{music.artist}","size":11,"color":"$SUB","maxLines":1}]},
              {"type":"row","tone":"pop","cornerRadius":16,"padding":6,"tap":"music:toggle","children":[{"type":"icon","name":"play","live":"{music.playIcon}","size":20,"color":"#FF3A1608"}]}]},
             {"type":"column","grow":true,"show":{"minHeight":130},"children":[
              {"type":"art","size":120,"cornerRadius":60,"show":{"minHeight":300,"maxWidth":299}},
              {"type":"spacer","size":10,"show":{"minHeight":300,"maxWidth":299}},
              {"type":"row","verticalAlign":"center","grow":true,"children":[
               {"type":"art","size":72,"cornerRadius":36,"show":{"maxHeight":299}},
               {"type":"art","size":72,"cornerRadius":36,"show":{"minWidth":300}},
               {"type":"spacer","size":12},
               {"type":"column","grow":true,"children":[
                {"type":"text","text":"{music.title}","size":17,"color":"$INK","weight":"medium","maxLines":1},
                {"type":"text","text":"{music.artist}","size":12,"color":"$SUB","maxLines":1},
                {"type":"text","text":"{music.album}","size":11,"color":"$SUB","maxLines":1,"show":{"minHeight":200}}]}]},
              {"type":"spacer","size":8},
              {"type":"progress","value":"{music.progress}","color":"$STRONG","trackColor":"$TILE","height":8,"tone":"strong","show":{"ifSet":"{music.length}"}},
              {"type":"row","show":{"ifSet":"{music.length}","minHeight":170},"children":[
               {"type":"timer","of":"music","size":10,"color":"$SUB"},
               {"type":"spacer","grow":true},
               {"type":"text","text":"{music.length}","size":10,"color":"$SUB","maxLines":1}]},
              {"type":"spacer","size":8},
              {"type":"row","align":"center","verticalAlign":"center","children":[
               {"type":"row","background":"$TILE","cornerRadius":20,"padding":8,"tap":"music:prev","children":[{"type":"icon","name":"previous","size":20,"color":"$INK"}]},
               {"type":"spacer","size":14},
               {"type":"row","tone":"pop","cornerRadius":20,"padding":12,"tap":"music:toggle","children":[{"type":"icon","name":"play","live":"{music.playIcon}","size":26,"color":"#FF3A1608"}]},
               {"type":"spacer","size":14},
               {"type":"row","background":"$TILE","cornerRadius":20,"padding":8,"tap":"music:next","children":[{"type":"icon","name":"next","size":20,"color":"$INK"}]}]}]}]}"""),
        ),
        Triple(
            "Music mini", "What's playing with the album art round and play/pause in the pop color (and next when wider); the artist and a progress pill when bigger. Pick the Pill shape for a slim oval.",
            card("Music mini", 12, """
            {"type":"column","verticalAlign":"center","tap":"music","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"art","size":40,"cornerRadius":20,"show":{"minWidth":200}},
              {"type":"spacer","size":10,"show":{"minWidth":200}},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{music.title}","size":13,"color":"$INK","weight":"bold","maxLines":1},
               {"type":"text","text":"{music.artist}","size":11,"color":"$SUB","maxLines":1,"show":{"minHeight":110}}]},
              {"type":"row","tone":"pop","cornerRadius":18,"padding":7,"tap":"music:toggle","children":[{"type":"icon","name":"play","live":"{music.playIcon}","size":22,"color":"#FF3A1608"}]},
              {"type":"spacer","size":8,"show":{"minWidth":240}},
              {"type":"row","background":"$TILE","cornerRadius":18,"padding":7,"tap":"music:next","show":{"minWidth":240},"children":[{"type":"icon","name":"next","size":20,"color":"$INK"}]}]},
             {"type":"column","show":{"minHeight":150},"children":[
              {"type":"spacer","size":10},
              {"type":"progress","value":"{music.progress}","color":"$STRONG","trackColor":"$TILE","height":8,"tone":"strong","show":{"ifSet":"{music.length}"}},
              {"type":"row","show":{"ifSet":"{music.length}","minHeight":190},"children":[
               {"type":"timer","of":"music","size":10,"color":"$SUB"},
               {"type":"spacer","grow":true},
               {"type":"text","text":"{music.length}","size":10,"color":"$SUB","maxLines":1}]},
              {"type":"row","align":"center","show":{"minHeight":230},"children":[
               {"type":"row","background":"$TILE","cornerRadius":18,"padding":7,"tap":"music:prev","children":[{"type":"icon","name":"previous","size":20,"color":"$INK"}]},
               {"type":"spacer","size":24},
               {"type":"row","background":"$TILE","cornerRadius":18,"padding":7,"tap":"music:next","children":[{"type":"icon","name":"next","size":20,"color":"$INK"}]}]}]}]}"""),
        ),

        // Money, news & travel
        Triple(
            "Crypto ticker", "Bitcoin's price big with its change in a pop chip; more coins in tiles as it grows taller. Set your coins in Settings.",
            card("Crypto ticker", 16, """
            {"type":"column","tap":"crypto","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"text","text":"BTC","size":11,"color":"$LIGHT","weight":"bold","chip":true,"tone":"strong"},
              {"type":"spacer","grow":true},
              {"type":"text","text":"{crypto.BTC.change}","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","maxLines":1}]},
             {"type":"spacer","size":6,"show":{"minHeight":130}},
             {"type":"text","text":"{crypto.BTC.price}","size":34,"color":"$INK","weight":"light","maxLines":1},
             {"type":"text","text":"Updated {updated}","size":10,"color":"$SUB","maxLines":1,"show":{"minHeight":130}},
             {"type":"column","show":{"minHeight":180},"children":[
              {"type":"spacer","size":8},
              ${gapped(listOf(coin("ETH", 0), coin("SOL", 230), coin("XRP", 280)))}]}]}"""),
        ),
        Triple(
            "Commute", "How long the drive takes with traffic, big beside a car tile, the delay in a pop chip; your next event and the weather when bigger. Set up Traffic in Settings.",
            card("Commute", 14, """
            {"type":"column","tap":"commute","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"row","tone":"strong","cornerRadius":22,"padding":10,"children":[{"type":"icon","name":"car","size":22,"color":"$LIGHT"}]},
              {"type":"spacer","size":12},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{traffic.commute}","size":28,"color":"$INK","weight":"light","maxLines":1},
               {"type":"text","text":"to {traffic.to} · {traffic.distance}","size":11,"color":"$SUB","maxLines":1}]},
              {"type":"text","text":"{traffic.delay}","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","maxLines":1,"show":{"minWidth":200,"ifSet":"{traffic.delay}"}}]},
             {"type":"text","text":"{traffic.delay}","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","maxLines":1,"show":{"maxWidth":199,"minHeight":130,"ifSet":"{traffic.delay}"}},
             {"type":"column","show":{"minHeight":150},"children":[
              {"type":"spacer","size":8},
              ${eventTile(1)}]},
             {"type":"column","show":{"minHeight":250},"children":[
              {"type":"spacer","size":8},
              {"type":"row","verticalAlign":"center","children":[
               {"type":"icon","name":"weather","size":22,"color":"$POP"},
               {"type":"text","text":" {weather.temp} · {weather.condition}","size":13,"color":"$INK","weight":"bold","maxLines":1}]},
              {"type":"spacer","size":6},
              ${hourPills()}]}]}"""),
        ),
        Triple(
            "Local news", "Top headlines for your town: the first in a strong tile, more in tiles the taller it is.",
            card("Local news", 14, """
            {"type":"column","tap":"news","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"news","size":16,"color":"$STRONG","tone":"strong"},
              {"type":"text","text":" Local","size":13,"color":"$INK","weight":"bold","grow":true,"maxLines":1},
              {"type":"clock","format":"h:mm","size":11,"color":"$SUB","show":{"minWidth":250}}]},
             {"type":"spacer","size":6},
             {"type":"column","grow":true,"tone":"strong","cornerRadius":18,"padding":10,"show":{"ifSet":"{news.1}"},"children":[
              {"type":"text","text":"{news.1.source}","size":10,"color":"$LSUB","weight":"bold","maxLines":1,"show":{"minHeight":110}},
              {"type":"text","text":"{news.1}","size":14,"color":"$LIGHT","weight":"bold","maxLines":3}]},
             {"type":"row","show":{"minHeight":200},"children":[
              {"type":"spacer","size":6},
              ${headline(2, 0)},
              {"type":"spacer","size":6,"show":{"minWidth":250}},
              {"type":"column","grow":true,"show":{"minWidth":250},"children":[${headline(3, 0)}]}]},
             {"type":"column","show":{"minHeight":300},"children":[{"type":"spacer","size":6},${headline(4, 0)}]}]}"""),
        ),
        Triple(
            "Nearby events", "What's on near you as tiles, when in a strong chip: concerts, games, shows. Set up Events in Settings.",
            card("Nearby events", 14, """
            {"type":"column","tap":"events","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"ticket","size":16,"color":"$POP"},
              {"type":"text","text":" Near you","size":13,"color":"$INK","weight":"bold","grow":true,"maxLines":1}]},
             {"type":"spacer","size":6},
             {"type":"column","tone":"pop","cornerRadius":18,"padding":10,"show":{"ifSet":"{events.1}"},"children":[
              {"type":"text","text":"{events.1.when}","size":10,"color":"#FF3A1608","weight":"bold","maxLines":1},
              {"type":"text","text":"{events.1}","size":14,"color":"#FF3A1608","weight":"bold","maxLines":2},
              {"type":"text","text":"{events.1.venue}","size":11,"color":"#FF3A1608","maxLines":1,"show":{"minHeight":150}}]},
             {"type":"column","show":{"minHeight":180},"children":[
              {"type":"spacer","size":6},
              ${gapped(listOf(event(2, 0), event(3, 230), event(4, 290)))}]}]}"""),
        ),
        Triple(
            "Nearby traffic", "A map of live traffic around home (TomTom) with your commute in a pill under it, its delay in the pop color. Needs a TomTom key and your home in Data sources.",
            card("Nearby traffic", 6, """
            {"type":"column","tap":"commute","children":[
             {"type":"map","grow":true,"cornerRadius":22},
             {"type":"column","show":{"minHeight":130,"ifSet":"{traffic.commute}"},"children":[
              {"type":"spacer","size":6},
              {"type":"row","verticalAlign":"center","background":"$TILE","cornerRadius":20,"padding":8,"children":[
               {"type":"icon","name":"car","size":16,"color":"$STRONG","tone":"strong"},
               {"type":"text","text":"  {traffic.commute} to {traffic.to}","size":13,"color":"$INK","weight":"bold","maxLines":1,"grow":true},
               {"type":"text","text":"{traffic.delay}","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","maxLines":1,"show":{"ifSet":"{traffic.delay}"}}]}]},
             {"type":"text","text":"{traffic.distance}","size":11,"color":"$SUB","align":"center","maxLines":1,"show":{"minHeight":300}}]}"""),
        ),

        // Battery
        Triple(
            "Device batteries", "Your phone, your WHOOP strap (with not-NOOP) and connected Bluetooth devices as left-to-right pills that stay the same size. Hide any in the widget's settings. Needs Device batteries in Data sources.",
            card("Device batteries", 14, """
            {"type":"column","verticalAlign":"center","tap":"battery","children":[
             {"type":"row","verticalAlign":"center","show":{"maxHeight":129},"children":[
              {"type":"icon","name":"phone","live":"{devices.1.icon}","size":18,"color":"$STRONG","tone":"strong"},
              {"type":"text","text":" {devices.1.pct}","size":18,"color":"$INK","weight":"light","maxLines":1,"grow":true},
              {"type":"icon","name":"bluetooth","live":"{devices.2.icon}","size":18,"color":"$STRONG","tone":"strong","show":{"ifSet":"{devices.2.name}"}},
              {"type":"text","text":" {devices.2.pct}","size":18,"color":"$INK","weight":"light","maxLines":1,"grow":true,"show":{"ifSet":"{devices.2.name}"}},
              {"type":"icon","name":"bluetooth","live":"{devices.3.icon}","size":18,"color":"$STRONG","tone":"strong","show":{"minWidth":300,"ifSet":"{devices.3.name}"}},
              {"type":"text","text":" {devices.3.pct}","size":18,"color":"$INK","weight":"light","maxLines":1,"grow":true,"show":{"minWidth":300,"ifSet":"{devices.3.name}"}}]},
             {"type":"column","show":{"minHeight":130},"children":[
              ${deviceBar(1, "")},
              ${deviceBar(2, "")},
              ${deviceBar(3, "")},
              ${deviceBar(4, "\"minHeight\":200,")},
              ${deviceBar(5, "\"minHeight\":250,")},
              ${deviceBar(6, "\"minHeight\":300,")}]}]}"""),
        ),
        Triple(
            "Battery", "Your battery big over a pill that fills with it, charging in a pop chip; time left, memory and storage in tiles when bigger.",
            card("Battery", 16, """
            {"type":"column","verticalAlign":"center","tap":"battery","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"text","text":"{battery}%","size":30,"color":"$INK","weight":"light","maxLines":1,"grow":true},
              {"type":"text","text":"{charging}","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","maxLines":1,"show":{"ifSet":"{charging}"}}]},
             {"type":"spacer","size":8},
             {"type":"progress","value":"{battery}","color":"$STRONG","trackColor":"$TILE","height":22,"tone":"strong"},
             {"type":"column","show":{"minHeight":150},"children":[
              {"type":"spacer","size":10},
              {"type":"row","children":[
               ${statTile("{battery.left}", "left")},
               {"type":"spacer","size":6,"show":{"minWidth":250}},
               ${statTile("{battery.temp}", "temperature", ",\"show\":{\"minWidth\":250}")}]}]},
             {"type":"column","show":{"minHeight":250},"children":[
              {"type":"spacer","size":8},
              {"type":"column","background":"$TILE","cornerRadius":18,"padding":10,"children":[
               {"type":"row","children":[{"type":"text","text":"Memory","size":12,"color":"$INK","weight":"bold","grow":true},{"type":"text","text":"{phone.ram}","size":11,"color":"$SUB","maxLines":1}]},
               {"type":"spacer","size":4},
               {"type":"progress","value":"{phone.ramPct}","color":"$STRONG","trackColor":"$SOFT","height":8,"tone":"strong"},
               {"type":"spacer","size":8},
               {"type":"row","children":[{"type":"text","text":"Storage","size":12,"color":"$INK","weight":"bold","grow":true},{"type":"text","text":"{phone.storage}","size":11,"color":"$SUB","maxLines":1}]},
               {"type":"spacer","size":4},
               {"type":"progress","value":"{phone.storagePct}","color":"$STRONG","trackColor":"$SOFT","height":8,"tone":"strong"}]},
              {"type":"text","text":"Drain {battery.drain}","size":11,"color":"$SUB","maxLines":1,"show":{"minHeight":330}}]}]}"""),
        ),
        Triple(
            "Battery fill", "The whole widget fills with your charge, like glass filling up, the number big on top; orange then red when low. Bigger shows battery life, memory and storage. Pick the Pill or Circle shape for fun.",
            """{"name":"Battery fill","background":"$TILE","cornerRadius":28,"padding":16,
            "fill":{"value":"{battery}","color":"$STRONG","levels":[{"from":0,"color":"#FFFF5C5C"},{"from":16,"color":"#FFFFB74D"}],"levelsUpTo":30},"root":
            {"type":"column","verticalAlign":"center","tap":"battery","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"bolt","size":20,"color":"$INK","show":{"ifSet":"{charging}"}},
              {"type":"text","text":"{battery}%","size":34,"color":"$INK","weight":"light","maxLines":1,"grow":true},
              {"type":"text","text":"{battery.left}","size":11,"color":"$INK","weight":"bold","chip":true,"maxLines":1,"show":{"minWidth":250}}]},
             {"type":"text","text":"{battery.left}","size":12,"color":"$INK","weight":"bold","maxLines":1,"show":{"minHeight":130,"maxWidth":249}},
             {"type":"column","show":{"minHeight":220},"children":[
              {"type":"spacer","size":10},
              {"type":"row","children":[{"type":"text","text":"Memory {phone.ram}","size":12,"color":"$INK","weight":"bold","grow":true,"maxLines":1},{"type":"text","text":"{phone.ramPct}%","size":12,"color":"$INK","maxLines":1}]},
              {"type":"row","children":[{"type":"text","text":"Storage {phone.storage}","size":12,"color":"$INK","weight":"bold","grow":true,"maxLines":1},{"type":"text","text":"{phone.storagePct}%","size":12,"color":"$INK","maxLines":1}]},
              {"type":"text","text":"Drain {battery.drain} · {battery.temp}","size":11,"color":"$SUB","maxLines":1,"show":{"minHeight":260}}]}]}}""",
        ),
        Triple(
            "Device batteries (circles)", "Your devices' batteries as rings in tiles, like One UI's: phone, WHOOP strap (with not-NOOP), earbuds, watches. Needs Device batteries in Data sources.",
            card("Device batteries (circles)", 12, """
            {"type":"column","verticalAlign":"center","tap":"battery","children":[
             {"type":"row","verticalAlign":"top","grow":true,"children":[
              ${gapped(listOf(deviceRing(1), deviceRing(2), deviceRing(3, "\"minWidth\":300,"), deviceRing(4, "\"minWidth\":300,")))}]},
             {"type":"column","show":{"maxWidth":299,"minHeight":130},"children":[
              {"type":"spacer","size":6},
              {"type":"row","verticalAlign":"top","children":[${gapped(listOf(deviceRing(3), deviceRing(4)))}]}]},
             {"type":"column","show":{"minWidth":300,"minHeight":250},"children":[
              {"type":"spacer","size":6},
              {"type":"row","verticalAlign":"top","children":[${gapped(listOf(deviceRing(5), deviceRing(6), deviceRing(7), deviceRing(8)))}]}]}]}"""),
        ),

        // Side Quest
        Triple(
            "Side Quest day", "Today's torches big on a strong card with pills for each, your hero and steps; your quests in tiles when tall.",
            """{"name":"Side Quest day","background":"$STRONG","cornerRadius":28,"padding":16,"root":
            {"type":"column","children":[
             {"type":"row","verticalAlign":"center","show":{"maxHeight":129},"children":[
              {"type":"icon","name":"flame","size":24,"color":"$POP"},
              {"type":"text","text":" {mq.torches}","size":24,"color":"$LIGHT","weight":"light","maxLines":1,"grow":true},
              {"type":"text","text":"{mq.steps}","size":12,"color":"$LSUB","maxLines":1}]},
             {"type":"column","grow":true,"show":{"minHeight":130},"children":[
              {"type":"row","verticalAlign":"center","children":[
               {"type":"text","text":"{mq.hero}","size":12,"color":"$LSUB","weight":"bold","grow":true,"maxLines":1},
               {"type":"text","text":"{mq.level}","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","maxLines":1}]},
              {"type":"spacer","grow":true},
              {"type":"row","verticalAlign":"bottom","children":[
               {"type":"text","text":"{mq.torches}","size":52,"color":"$LIGHT","weight":"light","maxLines":1},
               {"type":"text","text":" torches","size":14,"color":"$LIGHT","weight":"medium","maxLines":1}]},
              {"type":"text","text":"{mq.steps} · eggs {mq.eggs}","size":12,"color":"$LSUB","maxLines":1},
              {"type":"text","text":"Slept {mq.sleep}","size":12,"color":"$LSUB","maxLines":1,"show":{"minHeight":190}}]},
             {"type":"column","show":{"minHeight":260},"children":[
              {"type":"spacer","size":8},
              ${gapped(listOf(quest(1, 0), quest(2, 0), quest(3, 330)))}]}]}}""",
        ),
        Triple(
            "Side Quest eggs", "Your next egg as a ring that fills on the way to hatching, with a Hatch chip when it's ready; more eggs in tiles the taller it is.",
            card("Side Quest eggs", 14, """
            {"type":"column","children":[
             {"type":"row","verticalAlign":"center","show":{"maxHeight":129},"children":[
              {"type":"ring","value":"{mq.egg.1.progress}","text":"","size":36,"thickness":6,"color":"$POP","trackColor":"$TILE"},
              {"type":"spacer","size":10},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{mq.egg.1}","size":13,"color":"$INK","weight":"bold","maxLines":1},
               {"type":"text","text":"{mq.egg.1.left}","size":11,"color":"$SUB","maxLines":1}]}]},
             {"type":"row","grow":true,"show":{"minHeight":130},"children":[
              {"type":"column","align":"center","verticalAlign":"center","grow":true,"children":[
               {"type":"ring","value":"{mq.egg.1.progress}","text":"{mq.eggs}","size":88,"thickness":10,"textSize":20,"color":"$POP","trackColor":"$TILE","textColor":"$INK"},
               {"type":"spacer","size":4},
               {"type":"text","text":"{mq.egg.1.left}","size":11,"color":"$INK","weight":"bold","align":"center","maxLines":1},
               {"type":"text","text":"Hatch","size":12,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","tap":"{mq.egg.1.hatch}","show":{"ifSet":"{mq.egg.1.hatch}"}}]},
              {"type":"spacer","size":10,"show":{"minWidth":250}},
              {"type":"column","grow":true,"show":{"minWidth":250},"children":[${gapped(listOf(egg(2, 0), egg(3, 200)))}]}]},
             {"type":"column","show":{"minHeight":260,"maxWidth":249},"children":[{"type":"spacer","size":6},${gapped(listOf(egg(2, 0), egg(3, 330)))}]},
             {"type":"column","show":{"minHeight":300,"minWidth":250},"children":[{"type":"spacer","size":6},${egg(4, 0)}]},
             {"type":"text","text":"{mq.eggs.ready}","size":11,"color":"$SUB","align":"center","maxLines":1,"show":{"minHeight":200,"ifSet":"{mq.eggs.ready}"}}]}"""),
        ),
        Triple(
            "Side Quest adventures", "Monsters out on adventures in tiles: where, who, a pill until they're back and a Claim chip when they are.",
            card("Side Quest adventures", 14, """
            {"type":"column","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"paw","size":16,"color":"$STRONG","tone":"strong"},
              {"type":"text","text":" {mq.adventures}","size":13,"color":"$INK","weight":"bold","grow":true,"maxLines":1}]},
             {"type":"spacer","size":6},
             ${gapped(listOf(adventure(1, 0), adventure(2, 200), adventure(3, 280)))},
             {"type":"column","show":{"minHeight":340},"children":[{"type":"spacer","size":6},${quest(1, 0)}]}]}"""),
        ),
        Triple(
            "Side Quest quests", "Your next quests in tiles, each with a check button in the pop color: tap it to complete the quest in the game.",
            card("Side Quest quests", 14, """
            {"type":"column","children":[
             {"type":"row","verticalAlign":"center","show":{"minHeight":130},"children":[
              {"type":"text","text":"Quests","size":18,"color":"$INK","weight":"light","grow":true,"maxLines":1},
              {"type":"text","text":"{mq.quests}","size":11,"color":"$LIGHT","weight":"bold","chip":true,"tone":"strong","maxLines":1}]},
             {"type":"spacer","size":6,"show":{"minHeight":130}},
             ${gapped(listOf(quest(1, 0), quest(2, 130), quest(3, 200), quest(4, 260), quest(5, 320)))},
             {"type":"text","text":"All done for now","size":13,"color":"$SUB","show":{"ifEmpty":"{mq.quest.1}"}}]}"""),
        ),
        Triple(
            "Side Quest progress", "Steps to your next torch big over its pill, the weekly bounty in pills, and a phone-free session's time left.",
            card("Side Quest progress", 14, """
            {"type":"column","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"flame","size":20,"color":"$POP"},
              {"type":"text","text":" {mq.torch.next}","size":20,"color":"$INK","weight":"light","maxLines":1,"grow":true},
              {"type":"text","text":"{mq.torches}","size":11,"color":"$LIGHT","weight":"bold","chip":true,"tone":"strong","maxLines":1,"show":{"minWidth":200}}]},
             {"type":"spacer","size":6},
             {"type":"progress","value":"{mq.torch.pct}","color":"$POP","trackColor":"$TILE","height":12,"tone":"pop"},
             {"type":"column","show":{"minHeight":140},"children":[
              {"type":"spacer","size":8},
              {"type":"row","verticalAlign":"center","children":[
               {"type":"text","text":"Bounty","size":13,"color":"$INK","weight":"bold","grow":true},
               {"type":"text","text":"{mq.bounty.ready}","size":11,"color":"#FF3A1608","weight":"bold","chip":true,"tone":"pop","maxLines":1,"show":{"ifSet":"{mq.bounty.ready}"}}]},
              ${bounty(1, 0)},${bounty(2, 200)},${bounty(3, 250)}]},
             {"type":"column","show":{"minHeight":290,"ifSet":"{mq.focus}"},"children":[
              {"type":"spacer","size":8},
              {"type":"column","background":"$TILE","cornerRadius":16,"padding":8,"children":[
               {"type":"text","text":"Phone-free {mq.focus}","size":12,"color":"$INK","weight":"bold","maxLines":1},
               {"type":"spacer","size":4},
               {"type":"progress","value":"{mq.focus.pct}","color":"$STRONG","trackColor":"$SOFT","height":8,"tone":"strong"}]}]}]}"""),
        ),
        Triple(
            "Side Quest party", "Your party's monsters in tiles: their level in a chip and their health as a pill.",
            card("Side Quest party", 14, """
            {"type":"column","children":[
             {"type":"row","children":[${gapped(listOf(member(1, 0), member(2, 0), "{\"type\":\"column\",\"grow\":true,\"show\":{\"minWidth\":250},\"children\":[${member(3, 0)}]}"))}]},
             {"type":"column","show":{"minHeight":170},"children":[
              {"type":"spacer","size":6},
              {"type":"row","children":[${gapped(listOf("{\"type\":\"column\",\"grow\":true,\"show\":{\"maxWidth\":249},\"children\":[${member(3, 0)}]}", member(4, 0), "{\"type\":\"column\",\"grow\":true,\"show\":{\"minWidth\":250},\"children\":[${member(5, 0)}]}"))}]}]},
             {"type":"column","show":{"minHeight":280},"children":[{"type":"spacer","size":6},${gapped(listOf(quest(1, 0), quest(2, 330)))}]}]}"""),
        ),
    )
}
