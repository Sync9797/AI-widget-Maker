package com.sidequest.widgetmaker.spec

/**
 * Ready-made designs that work without any AI: pick one, save it, add it.
 * Each is responsive (more appears as the widget grows) and suits every theme.
 */
object Starters {
    data class Starter(val title: String, val about: String, val design: WidgetDesign) {
        val category: Category get() = categoryOf(this)
    }

    /** How the app groups ready-made designs. */
    enum class Category(val label: String) {
        everyday("Everyday"),
        clock("Clock & date"),
        weather("Weather"),
        calendar("Calendar"),
        health("Health & fitness"),
        music("Music"),
        info("Money, news & travel"),
        battery("Battery"),
        game("Side Quest"),
    }

    private val EVERYDAY = setOf("Daily briefing", "Smart stack", "Countdown")

    fun categoryOf(s: Starter): Category {
        if (s.title in EVERYDAY) return Category.everyday
        return when (Taps.mainKind(s.design)) {
            TapKind.weather -> Category.weather
            TapKind.calendar -> if (s.design.root.let { DesignParser.encode(s.design).contains("calendar.") }) Category.calendar else Category.clock
            TapKind.clock -> Category.clock
            TapKind.health -> Category.health
            TapKind.music -> Category.music
            TapKind.crypto, TapKind.news, TapKind.events, TapKind.commute -> Category.info
            TapKind.battery -> Category.battery
            TapKind.game -> Category.game
            null -> Category.everyday
        }
    }

    val all: List<Starter> by lazy {
        SOURCES.map { (title, about, json) -> Starter(title, about, DesignParser.parse(json)) }
    }

    /** One forecast hour; hours 4 and 5 only fit on wide widgets. */
    private fun hour(n: Int) = """
        {"type":"column","align":"center","grow":true,${if (n > 3) "\"show\":{\"minWidth\":250}," else ""}"children":[
         {"type":"text","text":"{weather.h$n.time}","size":11,"color":"#99FFFFFF","align":"center","maxLines":1},
         {"type":"icon","name":"sun","live":"{weather.h$n.icon}","size":20,"color":"#FFFFD27A"},
         {"type":"text","text":"{weather.h$n.temp}","size":13,"color":"#FFFFFFFF","align":"center"}]}"""

    private fun day(n: Int) = """
        {"type":"row","children":[
         {"type":"text","text":"{weather.d$n.day}","size":13,"color":"#CCFFFFFF","grow":true},
         {"type":"icon","name":"sun","live":"{weather.d$n.icon}","size":16,"color":"#FFFFD27A"},
         {"type":"text","text":"  {weather.d$n.high}","size":13,"color":"#FFFFFFFF"},
         {"type":"text","text":" {weather.d$n.low}","size":13,"color":"#99FFFFFF"}]}"""

    private fun headline(n: Int, minHeight: Int) = """
        {"type":"column","show":{"minHeight":$minHeight,"ifSet":"{news.$n}"},"children":[
         {"type":"spacer","size":8},
         {"type":"text","text":"{news.$n}","size":13,"color":"#FFFFFFFF","weight":"medium","maxLines":2},
         {"type":"text","text":"{news.$n.source}","size":11,"color":"#80FFFFFF","maxLines":1}]}"""

    private fun event(n: Int, minHeight: Int) = """
        {"type":"column","show":{"minHeight":$minHeight,"ifSet":"{events.$n}"},"children":[
         {"type":"spacer","size":8},
         {"type":"text","text":"{events.$n}","size":13,"color":"#FFFFFFFF","weight":"medium","maxLines":1},
         {"type":"text","text":"{events.$n.when} · {events.$n.venue}","size":11,"color":"#99FFFFFF","maxLines":1}]}"""

    private fun appointment(n: Int, minHeight: Int) = """
        {"type":"row","show":{"minHeight":$minHeight,"ifSet":"{calendar.$n}"},"children":[
         {"type":"column","grow":true,"children":[
          {"type":"spacer","size":8},
          {"type":"text","text":"{calendar.$n}","size":13,"color":"#FFFFFFFF","weight":"medium","maxLines":1},
          {"type":"text","text":"{calendar.$n.when}","size":11,"color":"#99FFFFFF","maxLines":1}]}]}"""

    /** One hour in a forecast list: time, chance of rain, conditions, temperature, then a divider. */
    private fun hourRow(n: Int, minHeight: Int, divider: Boolean = true, clock24: Boolean = false): String {
        val time = if (clock24) "time24" else "time"
        val line = if (divider) DIVIDER_GAP else ""
        return """
        {"type":"column","show":{"minHeight":$minHeight},"children":[
         {"type":"row","verticalAlign":"center","children":[
          {"type":"text","text":"{weather.h$n.$time}","size":13,"color":"#B3FFFFFF","grow":true},
          {"type":"spacer","size":6},
          {"type":"icon","name":"umbrella","size":12,"color":"#99FFFFFF"},
          {"type":"text","text":" {weather.h$n.rain}","size":13,"color":"#B3FFFFFF","grow":true,"levelOf":"{weather.h$n.rain}","levels":[{"from":0,"color":"#B3FFFFFF"},{"from":30,"color":"#FF9CCBFF"},{"from":60,"color":"#FF5AA9FF"}]},
          {"type":"icon","name":"sun","live":"{weather.h$n.icon}","size":18,"color":"#FFDCE8FF"},
          {"type":"text","text":"{weather.h$n.temp}","size":14,"color":"#FFFFFFFF","weight":"bold","align":"end","grow":true}]}$line]}"""
    }

    private const val DIVIDER_GAP =
        """,{"type":"spacer","size":5},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":5}"""

    private fun egg(n: Int, minHeight: Int) = """
        {"type":"column","show":{"minHeight":$minHeight,"ifSet":"{mq.egg.$n}"},"children":[
         {"type":"spacer","size":8},
         {"type":"row","verticalAlign":"center","children":[
          {"type":"icon","name":"egg","size":14,"color":"#FFFFE1A8"},
          {"type":"text","text":"  {mq.egg.$n}","size":13,"color":"#FFFFFFFF","weight":"medium","grow":true,"maxLines":1},
          {"type":"spacer","size":8},{"type":"text","text":"{mq.egg.$n.left}","size":11,"color":"#B3FFFFFF","maxLines":1,"show":{"minWidth":250}},
          {"type":"text","text":"  Hatch","size":12,"color":"#FFFFC857","weight":"bold","tap":"{mq.egg.$n.hatch}","show":{"ifSet":"{mq.egg.$n.hatch}"}}]},
         {"type":"text","text":"{mq.egg.$n.left}","size":11,"color":"#B3FFFFFF","maxLines":1,"show":{"maxWidth":249}},
         {"type":"spacer","size":4},
         {"type":"progress","value":"{mq.egg.$n.progress}","color":"#FFFFC857","trackColor":"#26FFFFFF","height":5}]}"""

    private fun adventure(n: Int, minHeight: Int) = """
        {"type":"column","show":{"minHeight":$minHeight,"ifSet":"{mq.adv.$n}"},"children":[
         {"type":"spacer","size":10},
         {"type":"row","verticalAlign":"center","children":[
          {"type":"text","text":"{mq.adv.$n}","size":14,"color":"#FFFFFFFF","weight":"bold","maxLines":1,"grow":true},
          {"type":"spacer","size":8},{"type":"text","text":"Claim","size":12,"color":"#FF8FE3B0","weight":"bold","tap":"{mq.adv.$n.claim}","show":{"ifSet":"{mq.adv.$n.claim}"}}]},
         {"type":"text","text":"{mq.adv.$n.team} · {mq.adv.$n.left}","size":12,"color":"#B3FFFFFF","maxLines":1},
         {"type":"spacer","size":4},
         {"type":"progress","value":"{mq.adv.$n.progress}","color":"#FF8FE3B0","trackColor":"#26FFFFFF","height":5}]}"""

    private fun quest(n: Int, minHeight: Int) = """
        {"type":"column","show":{"minHeight":$minHeight,"ifSet":"{mq.quest.$n}"},"children":[
         {"type":"spacer","size":8},
         {"type":"row","verticalAlign":"center","children":[
          {"type":"icon","name":"check","size":22,"color":"#FF8AB4F8","tap":"{mq.quest.$n.done}"},
          {"type":"spacer","size":10},
          {"type":"column","grow":true,"children":[
           {"type":"text","text":"{mq.quest.$n}","size":14,"color":"#FFFFFFFF","weight":"medium","maxLines":1},
           {"type":"text","text":"{mq.quest.$n.when}","size":11,"color":"#99FFFFFF","maxLines":1,"show":{"ifSet":"{mq.quest.$n.when}"}}]}]}]}"""

    private fun member(n: Int, minHeight: Int) = """
        {"type":"column","show":{"minHeight":$minHeight,"ifSet":"{mq.party.$n}"},"children":[
         {"type":"spacer","size":8},
         {"type":"row","verticalAlign":"center","children":[
          {"type":"text","text":"{mq.party.$n}","size":13,"color":"#FFFFFFFF","weight":"medium","grow":true,"maxLines":1},
          {"type":"spacer","size":8},{"type":"text","text":"{mq.party.$n.level}","size":11,"color":"#99FFFFFF"},
          {"type":"text","text":"  {mq.party.$n.hp}","size":11,"color":"#CCFFFFFF","show":{"minWidth":250}}]},
         {"type":"spacer","size":4},
         {"type":"progress","value":"{mq.party.$n.hpPct}","color":"#FF4CD137","trackColor":"#26FFFFFF","height":5}]}"""

    private fun bountyLine(n: Int, minHeight: Int) = """
        {"type":"column","show":{"minHeight":$minHeight,"ifSet":"{mq.bounty.$n}"},"children":[
         {"type":"spacer","size":6},
         {"type":"row","children":[
          {"type":"text","text":"{mq.bounty.$n}","size":12,"color":"#E6FFFFFF","grow":true,"maxLines":1},
          {"type":"spacer","size":8},{"type":"text","text":"{mq.bounty.$n.progress}","size":11,"color":"#99FFFFFF","show":{"minWidth":250}}]},
         {"type":"spacer","size":3},
         {"type":"progress","value":"{mq.bounty.$n.pct}","color":"#FFFF9F43","trackColor":"#26FFFFFF","height":4}]}"""

    private fun coin(symbol: String, minHeight: Int) = """
        {"type":"row","show":{"minHeight":$minHeight},"children":[
         {"type":"text","text":"$symbol","size":14,"color":"#CCFFFFFF","weight":"bold","grow":true},
         {"type":"spacer","size":8},{"type":"text","text":"{crypto.$symbol.price}","size":14,"color":"#FFFFFFFF","weight":"medium"},
         {"type":"text","text":"  {crypto.$symbol.change}","size":13,"color":"#FFFFFFFF",
          "trendOf":"{crypto.$symbol.change}","show":{"minWidth":250}}]}"""

    private val SOURCES = listOf(
        Triple(
            "Big clock", "The time, large. Adds the date, then a greeting and battery as it grows.",
            """{"name":"Big clock","background":"#E6141B2D","cornerRadius":28,"padding":16,"root":
            {"type":"column","tap":"clock","align":"center","verticalAlign":"center","children":[
             {"type":"text","text":"{greeting}","size":14,"color":"#B3FFFFFF","weight":"medium","show":{"minHeight":200}},
             {"type":"clock","format":"h:mm","size":40,"color":"#FFFFFFFF","weight":"light","show":{"maxHeight":119}},
             {"type":"clock","format":"h:mm","size":44,"color":"#FFFFFFFF","weight":"light","show":{"minHeight":120,"maxWidth":249}},
             {"type":"clock","format":"h:mm","size":56,"color":"#FFFFFFFF","weight":"light","show":{"minHeight":120,"minWidth":250}},
             {"type":"clock","format":"EEE, MMM d","size":14,"color":"#CCFFFFFF","weight":"normal","show":{"minHeight":120,"maxWidth":249}},
             {"type":"clock","format":"EEEE, MMMM d","size":14,"color":"#CCFFFFFF","weight":"normal","show":{"minHeight":120,"minWidth":250}},
             {"type":"spacer","size":12,"show":{"minHeight":200}},
             {"type":"row","show":{"minHeight":200},"children":[
              {"type":"icon","name":"battery","size":14,"color":"#FF7CF29C","levelOf":"{battery}","levels":[{"from":0,"color":"#FFFF5C5C"},{"from":16,"color":"#FFFFB74D"},{"from":31,"color":"#FF7CF29C"}]},
              {"type":"text","text":" {battery}%  {charging}","size":13,"color":"#B3FFFFFF"}]},{"type":"column","show":{"minHeight":250},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","children":[{"type":"icon","name":"weather","size":14,"color":"#FFFFD27A"},{"type":"text","text":" {weather.temp} · {weather.outlook}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"H {weather.high} L {weather.low}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{calendar.1}"},"children":[{"type":"icon","name":"calendar","size":14,"color":"#FF8AB4FF"},{"type":"text","text":" {calendar.1}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{calendar.1.when}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"spacer","size":8,"show":{"minHeight":300}},{"type":"column","show":{"minHeight":300},"children":[{"type":"row","children":[${hour(1)},${hour(2)},${hour(3)},${hour(4)}]}]}]}]}}""",
        ),
        Triple(
            "Clock and weather", "Time and today's weather side by side; the hourly forecast when it's taller.",
            """{"name":"Clock and weather","background":"#E6102236","cornerRadius":26,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","children":[
              {"type":"column","grow":true,"children":[
               {"type":"clock","format":"h:mm","size":40,"color":"#FFFFFFFF","weight":"light"},
               {"type":"clock","format":"EEE d MMM","size":13,"color":"#B3FFFFFF"}]},
              {"type":"column","align":"end","show":{"minWidth":220},"children":[
               {"type":"row","children":[
                {"type":"icon","name":"weather","size":26,"color":"#FFFFD27A"},
                {"type":"text","text":" {weather.temp}","size":28,"color":"#FFFFFFFF","weight":"light"}]},
               {"type":"text","text":"{weather.condition}","size":12,"color":"#B3FFFFFF","align":"end","maxLines":1}]}]},
             {"type":"row","show":{"maxWidth":219,"minHeight":120},"children":[
              {"type":"icon","name":"weather","size":16,"color":"#FFFFD27A"},
              {"type":"text","text":" {weather.temp} {weather.condition}","size":13,"color":"#CCFFFFFF","maxLines":1}]},
             {"type":"spacer","size":14,"show":{"minHeight":200}},
             {"type":"row","show":{"minHeight":200},"children":[${hour(1)},${hour(2)},${hour(3)},${hour(4)}]},{"type":"column","show":{"minHeight":270},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},${day(1)},{"type":"spacer","size":4},${day(2)},{"type":"spacer","size":4},${day(3)}]}]}}""",
        ),
        Triple(
            "Weather forecast", "Like One UI's weather: now, the outlook and your town at 2x2; the next hours when wider; the next 3 days when taller.",
            """{"name":"Weather forecast","background":"#F2163170","cornerRadius":30,"padding":16,"root":
            {"type":"column","children":[
             {"type":"row","grow":true,"children":[
              {"type":"column","grow":true,"children":[
               {"type":"row","show":{"minHeight":150},"children":[
                {"type":"spacer","grow":true},
                {"type":"icon","name":"weather","size":56,"color":"#FFC9DDE6"},
                {"type":"spacer","size":20,"show":{"minWidth":250}}]},
               {"type":"spacer","grow":true},
               {"type":"text","text":"{weather.temp}","size":30,"color":"#FFFFFFFF","weight":"medium","maxLines":1},
               {"type":"text","text":"{weather.outlook}","size":13,"color":"#E6FFFFFF","maxLines":1,"show":{"minHeight":100}},
               {"type":"row","show":{"minHeight":120},"children":[
                {"type":"icon","name":"pin","size":13,"color":"#B3FFFFFF"},
                {"type":"text","text":" {weather.place}","size":13,"color":"#B3FFFFFF","maxLines":1}]}]},
              {"type":"spacer","size":20,"show":{"minWidth":250}},
              {"type":"column","grow":true,"show":{"minWidth":250},"verticalAlign":"center","children":[
               ${hourRow(1, 0)},${hourRow(2, 0)},${hourRow(3, 140)},${hourRow(4, 140, divider = false)}]}]},
             {"type":"column","show":{"minHeight":300},"children":[
              {"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},
              ${day(1)},{"type":"spacer","size":4},${day(2)},{"type":"spacer","size":4},${day(3)}]},{"type":"column","show":{"minHeight":380},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","children":[{"type":"text","text":"Feels {weather.feelsLike}","size":12,"color":"#CCFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"Wind {weather.wind}","size":12,"color":"#CCFFFFFF","maxLines":1,"grow":true},{"type":"text","text":"Rain {weather.rain}","size":12,"color":"#CCFFFFFF","maxLines":1}]}]}]}}""",
        ),
        Triple(
            "Crypto ticker", "Bitcoin's price and change; more coins as it grows taller. Set your coins in Settings.",
            """{"name":"Crypto ticker","background":"#E6111418","cornerRadius":22,"padding":14,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","children":[
              {"type":"icon","name":"coin","size":18,"color":"#FFF7B731"},
              {"type":"text","text":" Crypto","size":13,"color":"#B3FFFFFF","weight":"medium","grow":true},
              {"type":"spacer","size":8},{"type":"text","text":"{updated}","size":11,"color":"#66FFFFFF","show":{"minWidth":250}}]},
             {"type":"spacer","size":6},
             {"type":"text","text":"{crypto.BTC.price}","size":26,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
             {"type":"text","text":"BTC {crypto.BTC.change} today","size":13,"color":"#FFFFFFFF","trendOf":"{crypto.BTC.change}"},
             {"type":"spacer","size":10,"show":{"minHeight":170}},
             ${coin("ETH", 170)},
             {"type":"spacer","size":6,"show":{"minHeight":210}},
             ${coin("SOL", 210)},
             {"type":"spacer","size":6,"show":{"minHeight":250}},
             ${coin("XRP", 250)},{"type":"column","show":{"minHeight":280},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"text","text":"Prices from CoinGecko · updated {updated}","size":11,"color":"#80FFFFFF","maxLines":1}]}]}}""",
        ),
        Triple(
            "Commute", "How long the drive takes with traffic. Set up Traffic in Settings.",
            """{"name":"Commute","background":"#E61B2A22","cornerRadius":24,"padding":16,"root":
            {"type":"column","tap":"commute","verticalAlign":"center","children":[
             {"type":"row","children":[
              {"type":"icon","name":"car","size":20,"color":"#FF7CF29C"},
              {"type":"text","text":"  To {traffic.to}","size":13,"color":"#B3FFFFFF","weight":"medium","grow":true}]},
             {"type":"text","text":"{traffic.commute}","size":32,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
             {"type":"text","text":"{traffic.delay} · {traffic.distance}","size":13,"color":"#CCFFFFFF","show":{"minHeight":120}},
             {"type":"spacer","size":12,"show":{"minHeight":200}},
             {"type":"row","show":{"minHeight":200},"children":[
              {"type":"icon","name":"weather","size":16,"color":"#FFFFD27A"},
              {"type":"text","text":" {weather.temp} {weather.condition}","size":13,"color":"#B3FFFFFF","maxLines":1}]},{"type":"column","show":{"minHeight":230},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","show":{"ifSet":"{calendar.1}"},"children":[{"type":"icon","name":"calendar","size":14,"color":"#FF8AB4FF"},{"type":"text","text":" {calendar.1}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{calendar.1.when}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"spacer","size":8},{"type":"row","children":[${hour(1)},${hour(2)},${hour(3)},${hour(4)}]}]}]}}""",
        ),
        Triple(
            "Local news", "Top headlines for your town; more of them the taller it is.",
            """{"name":"Local news","background":"#F2F4EFE6","cornerRadius":20,"padding":16,"root":
            {"type":"column","children":[
             {"type":"row","children":[
              {"type":"icon","name":"news","size":16,"color":"#FFB3261E"},
              {"type":"text","text":"  LOCAL NEWS","size":12,"color":"#FFB3261E","weight":"bold","grow":true},
              {"type":"clock","format":"EEE d MMM","size":11,"color":"#99000000","show":{"minWidth":250}}]},
             {"type":"spacer","size":6},
             {"type":"text","text":"{news.1}","size":15,"color":"#FF1A1A1A","weight":"bold","maxLines":3},
             {"type":"text","text":"{news.1.source}","size":11,"color":"#80000000","maxLines":1,"show":{"minHeight":110}},
             ${headline(2, 190).replace("#FFFFFFFF", "#FF1A1A1A").replace("#80FFFFFF", "#80000000")},
             ${headline(3, 260).replace("#FFFFFFFF", "#FF1A1A1A").replace("#80FFFFFF", "#80000000")},
             ${headline(4, 330).replace("#FFFFFFFF", "#FF1A1A1A").replace("#80FFFFFF", "#80000000")}]}}""",
        ),
        Triple(
            "Nearby events", "What's on near you: concerts, games, shows. Set up Events in Settings.",
            """{"name":"Nearby events","background":"#E62A1440","cornerRadius":24,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","show":{"minHeight":120},"children":[
              {"type":"icon","name":"ticket","size":16,"color":"#FFFF7AC6"},
              {"type":"text","text":"  What's on","size":13,"color":"#FFFF7AC6","weight":"bold","grow":true}]},
             {"type":"spacer","size":6,"show":{"minHeight":120}},
             {"type":"text","text":"{events.1}","size":16,"color":"#FFFFFFFF","weight":"bold","maxLines":1,"show":{"maxHeight":119}},
             {"type":"text","text":"{events.1}","size":16,"color":"#FFFFFFFF","weight":"bold","maxLines":2,"show":{"minHeight":120}},
             {"type":"text","text":"{events.1.when} · {events.1.venue}","size":12,"color":"#B3FFFFFF","maxLines":1},
             ${event(2, 180)},
             ${event(3, 240)},
             ${event(4, 300)}]}}""",
        ),
        Triple(
            "Up next", "Your next calendar events; more of them the taller it is. Turn on Your calendar in Settings.",
            """{"name":"Up next","background":"#E61A2433","cornerRadius":24,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","show":{"minHeight":120},"children":[
              {"type":"icon","name":"calendar","size":16,"color":"#FF8AB4F8"},
              {"type":"text","text":"  Up next","size":13,"color":"#FF8AB4F8","weight":"bold","grow":true},
              {"type":"spacer","size":8},{"type":"text","text":"{calendar.today}","size":11,"color":"#99FFFFFF","show":{"minWidth":250}}]},
             {"type":"spacer","size":6,"show":{"minHeight":120}},
             {"type":"text","text":"{calendar.1}","size":16,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
             {"type":"text","text":"{calendar.1.when}","size":13,"color":"#FF8AB4F8","maxLines":1},
             {"type":"text","text":"{calendar.1.where}","size":12,"color":"#99FFFFFF","maxLines":1,"show":{"minHeight":150}},
             ${appointment(2, 190)},
             ${appointment(3, 240)},
             ${appointment(4, 290)}]}}""",
        ),
        Triple(
            "Big date", "Material 3 Expressive: the month, the weekday and a huge day number; your next events beside it when wider.",
            """{"name":"Big date","background":"#F22A221C","cornerRadius":32,"padding":18,"root":
            {"type":"column","children":[{"type":"row","grow":true,"verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","show":{"maxHeight":119},"children":[
              {"type":"clock","format":"d","size":38,"color":"#FFFFFFFF","weight":"bold"},
              {"type":"spacer","size":10},
              {"type":"column","children":[
               {"type":"clock","format":"MMM yyyy","size":12,"color":"#FFFFFFFF","weight":"bold"},
               {"type":"clock","format":"EEEE","size":12,"color":"#CCFFFFFF","weight":"normal"}]}]},
             {"type":"column","align":"center","verticalAlign":"center","grow":true,"show":{"minHeight":120},"children":[
              {"type":"clock","format":"MMM yyyy","size":15,"color":"#FFFFFFFF","weight":"bold"},
              {"type":"clock","format":"EEEE","size":15,"color":"#CCFFFFFF","weight":"normal"},
              {"type":"clock","format":"d","size":64,"color":"#FFFFFFFF","weight":"bold","show":{"maxHeight":139}},
              {"type":"clock","format":"d","size":96,"color":"#FFFFFFFF","weight":"bold","show":{"minHeight":140}}]},
             {"type":"column","grow":true,"show":{"minWidth":250},"children":[
              {"type":"text","text":"{calendar.1}","size":14,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
              {"type":"text","text":"{calendar.1.when}","size":12,"color":"#B3FFFFFF","maxLines":1},
              {"type":"spacer","size":8,"show":{"minHeight":120}},
              {"type":"text","text":"{calendar.2}","size":14,"color":"#FFFFFFFF","weight":"bold","maxLines":1,"show":{"minHeight":120}},
              {"type":"text","text":"{calendar.2.when}","size":12,"color":"#B3FFFFFF","maxLines":1,"show":{"minHeight":120}},
              {"type":"spacer","size":8,"show":{"minHeight":200}},
              {"type":"text","text":"{calendar.3}","size":14,"color":"#FFFFFFFF","weight":"bold","maxLines":1,"show":{"minHeight":200}},
              {"type":"text","text":"{calendar.3.when}","size":12,"color":"#B3FFFFFF","maxLines":1,"show":{"minHeight":200}}]}]},{"type":"column","show":{"minHeight":240,"maxWidth":249},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","show":{"ifSet":"{calendar.1}"},"children":[{"type":"icon","name":"calendar","size":14,"color":"#FF8AB4FF"},{"type":"text","text":" {calendar.1}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{calendar.1.when}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{calendar.2}"},"children":[{"type":"icon","name":"calendar","size":14,"color":"#FF8AB4FF"},{"type":"text","text":" {calendar.2}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{calendar.2.when}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"row","verticalAlign":"center","children":[{"type":"icon","name":"weather","size":14,"color":"#FFFFD27A"},{"type":"text","text":" {weather.temp} · {weather.outlook}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"H {weather.high} L {weather.low}","size":12,"color":"#99FFFFFF","maxLines":1}]}]}]}}""",
        ),
        Triple(
            "Day cookie", "A scalloped Material 3 Expressive cookie with the weekday and day number.",
            """{"name":"Day cookie","background":"#FFD8E4FF","cornerRadius":24,"padding":10,"shape":"cookie","root":
            {"type":"column","align":"center","verticalAlign":"center","children":[
             {"type":"clock","format":"EEE","size":14,"color":"#FF1B2A4A","weight":"bold"},
             {"type":"clock","format":"d","size":44,"color":"#FF1B2A4A","weight":"bold","show":{"maxHeight":199}},
             {"type":"clock","format":"d","size":64,"color":"#FF1B2A4A","weight":"bold","show":{"minHeight":200}},
             {"type":"clock","format":"MMMM","size":14,"color":"#CC1B2A4A","weight":"medium","show":{"minHeight":200}}]}}""",
        ),
        Triple(
            "Hourly weather", "Now on the left with the outlook (\"Rain possible tonight\"), the next hours with rain chances on the right, in 24-hour time.",
            """{"name":"Hourly weather","background":"#F2163170","cornerRadius":30,"padding":16,"root":
            {"type":"column","children":[{"type":"row","grow":true,"children":[
             {"type":"column","grow":true,"children":[
              {"type":"row","show":{"minHeight":150},"children":[
               {"type":"spacer","grow":true},
               {"type":"icon","name":"weather","size":56,"color":"#FFC9DDE6"},
               {"type":"spacer","size":20,"show":{"minWidth":250}}]},
              {"type":"spacer","grow":true},
              {"type":"text","text":"{weather.temp}","size":30,"color":"#FFFFFFFF","weight":"medium","maxLines":1},
              {"type":"text","text":"{weather.outlook}","size":13,"color":"#E6FFFFFF","maxLines":1,"show":{"minHeight":100}},
              {"type":"row","show":{"minHeight":120},"children":[
               {"type":"icon","name":"pin","size":13,"color":"#B3FFFFFF"},
               {"type":"text","text":" {weather.place}","size":13,"color":"#B3FFFFFF","maxLines":1}]}]},
             {"type":"spacer","size":20,"show":{"minWidth":250}},
             {"type":"column","grow":true,"show":{"minWidth":250},"verticalAlign":"center","children":[
              ${hourRow(1, 0, clock24 = true)},${hourRow(2, 0, clock24 = true)},${hourRow(3, 140, clock24 = true)},${hourRow(4, 140, divider = false, clock24 = true)}]}]},{"type":"column","show":{"minHeight":300},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"column","show":{"maxWidth":249},"children":[{"type":"row","children":[${hour(1)},${hour(2)},${hour(3)}]},{"type":"spacer","size":8}]},${day(1)},{"type":"spacer","size":4},${day(2)},{"type":"spacer","size":4},${day(3)}]}]}}""",
        ),
        Triple(
            "Battery fill", "The whole widget fills with your charge, like glass filling up: your wallpaper color on One UI, your phone's color on Material 3, orange then red when low. Bigger shows battery life, memory and storage. Pick the Pill shape for a slim oval.",
            """{"name":"Battery fill","background":"#66303036","cornerRadius":26,"padding":14,
            "fill":{"value":"{battery}","color":"#FF9FD8CF","levels":[{"from":0,"color":"#FFFF5C5C"},{"from":16,"color":"#FFFFB74D"}],"levelsUpTo":30},"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"bolt","size":18,"color":"#FFFFFFFF","show":{"ifSet":"{charging}"}},
              {"type":"text","text":"{battery}%","size":22,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
              {"type":"text","text":"  {charging}","size":12,"color":"#CCFFFFFF","maxLines":1,"show":{"minWidth":250}}]},
             {"type":"text","text":"{battery.left}","size":12,"color":"#CCFFFFFF","maxLines":1,"show":{"minHeight":100,"maxHeight":219}},{"type":"column","show":{"minHeight":220},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","children":[{"type":"icon","name":"bolt","size":14,"color":"#FF7CF29C"},{"type":"text","text":" {battery.left}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true}]},{"type":"row","show":{"minHeight":250},"children":[{"type":"text","text":"Drain {battery.drain}","size":12,"color":"#99FFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{battery.temp}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"spacer","size":8},{"type":"column","children":[{"type":"row","children":[{"type":"text","text":"Memory","size":12,"color":"#CCFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{phone.ram}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"progress","value":"{phone.ramPct}","color":"#FF7CF29C","trackColor":"#26FFFFFF","height":5,"levelOf":"{phone.ramPct}","levels":[{"from":0,"color":"#FF7CF29C"},{"from":70,"color":"#FFFFB74D"},{"from":90,"color":"#FFFF5C5C"}]},{"type":"spacer","size":6}]},{"type":"column","show":{"minHeight":290},"children":[{"type":"column","children":[{"type":"row","children":[{"type":"text","text":"Storage","size":12,"color":"#CCFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{phone.storage}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"progress","value":"{phone.storagePct}","color":"#FF7CF29C","trackColor":"#26FFFFFF","height":5,"levelOf":"{phone.storagePct}","levels":[{"from":0,"color":"#FF7CF29C"},{"from":70,"color":"#FFFFB74D"},{"from":90,"color":"#FFFF5C5C"}]},{"type":"spacer","size":6}]}]}]}]}}""",
        ),
        Triple(
            "Agenda card", "Side Quest's date card: what's next at the top, today's date big at the bottom.",
            """{"name":"Agenda card","background":"#E61C2433","cornerRadius":28,"padding":16,"root":
            {"type":"column","children":[
             {"type":"text","text":"{calendar.1}","size":18,"color":"#FFFFFFFF","weight":"bold","maxLines":1,"show":{"maxHeight":179}},
             {"type":"text","text":"{calendar.1}","size":18,"color":"#FFFFFFFF","weight":"bold","maxLines":2,"show":{"minHeight":180}},
             {"type":"text","text":"{calendar.1.when}","size":13,"color":"#FF8AB4F8","maxLines":1,"show":{"minHeight":100}},
             {"type":"text","text":"{calendar.2} · {calendar.2.when}","size":12,"color":"#B3FFFFFF","maxLines":1,"show":{"minHeight":230,"ifSet":"{calendar.2}"}},
             {"type":"text","text":"{calendar.3} · {calendar.3.when}","size":12,"color":"#B3FFFFFF","maxLines":1,"show":{"minHeight":270,"ifSet":"{calendar.3}"}},
             {"type":"spacer","grow":true},
             {"type":"clock","format":"d","size":40,"color":"#FFFFFFFF","weight":"bold","show":{"minHeight":140,"maxHeight":199}},
             {"type":"clock","format":"d","size":52,"color":"#FFFFFFFF","weight":"bold","show":{"minHeight":200}},
             {"type":"clock","format":"EEEE, MMM d","size":14,"color":"#FFFFFFFF","weight":"bold","show":{"maxHeight":139}},
             {"type":"clock","format":"EEEE, MMM","size":14,"color":"#FFFFFFFF","weight":"bold","show":{"minHeight":140}},{"type":"column","show":{"minHeight":320},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","children":[{"type":"icon","name":"weather","size":14,"color":"#FFFFD27A"},{"type":"text","text":" {weather.temp} · {weather.outlook}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"H {weather.high} L {weather.low}","size":12,"color":"#99FFFFFF","maxLines":1}]}]}]}}""",
        ),
        Triple(
            "Steps", "Today's steps and a bar to your goal; how many are left when taller. Pick the Pill shape for a slim oval.",
            """{"name":"Steps","background":"#E62A1B12","cornerRadius":26,"padding":14,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"text","text":"Steps","size":13,"color":"#CCFFFFFF","weight":"medium","show":{"minHeight":130}},
             {"type":"spacer","grow":true,"show":{"minHeight":130}},
             {"type":"row","verticalAlign":"bottom","children":[
              {"type":"text","text":"{health.steps}","size":22,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
              {"type":"text","text":" /{health.stepGoal}","size":12,"color":"#99FFFFFF","maxLines":1,"show":{"minWidth":200}}]},
             {"type":"text","text":"{health.stepsLeft.text}","size":12,"color":"#B3FFFFFF","maxLines":1,"show":{"minHeight":130}},
             {"type":"spacer","size":8,"show":{"minHeight":100}},
             {"type":"progress","value":"{health.stepsPct}","color":"#FF4CD137","trackColor":"#40FFFFFF","height":10,"levelOf":"{health.stepsPct}","levels":[{"from":0,"color":"#FFFFB74D"},{"from":50,"color":"#FFFFE066"},{"from":100,"color":"#FF4CD137"}]},{"type":"column","show":{"minHeight":230},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","children":[{"type":"column","grow":true,"children":[{"type":"icon","name":"flame","size":14,"color":"#FFFF8A50"},{"type":"text","text":"{health.kcal}","size":14,"color":"#FFFFFFFF","maxLines":1,"weight":"bold"},{"type":"text","text":"kcal","size":11,"color":"#99FFFFFF","maxLines":1}]},{"type":"column","grow":true,"children":[{"type":"icon","name":"heart","size":14,"color":"#FFFF5C7A"},{"type":"text","text":"{health.heart}","size":14,"color":"#FFFFFFFF","maxLines":1,"weight":"bold"},{"type":"text","text":"bpm","size":11,"color":"#99FFFFFF","maxLines":1}]},{"type":"column","grow":true,"children":[{"type":"icon","name":"bolt","size":14,"color":"#FF4FA3FF"},{"type":"text","text":"{health.exercise}","size":14,"color":"#FFFFFFFF","maxLines":1,"weight":"bold"},{"type":"text","text":"active","size":11,"color":"#99FFFFFF","maxLines":1}]}]},{"type":"column","show":{"minHeight":300,"ifSet":"{health.workout}"},"children":[{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","children":[{"type":"icon","name":"steps","size":14,"color":"#FF4CD137"},{"type":"text","text":" {health.workout} · {health.workout.duration}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{health.workout.when}","size":12,"color":"#99FFFFFF","maxLines":1}]}]}]}]}}""",
        ),
        Triple(
            "Activity", "Steps, kcal, heart rate and effort today; your last workout when wider, and sleep when taller.",
            """{"name":"Activity","background":"#E6141C1A","cornerRadius":26,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"column","grow":true,"children":[
               {"type":"row","verticalAlign":"center","children":[
                {"type":"icon","name":"steps","size":14,"color":"#FF4CD137"},
                {"type":"text","text":" {health.steps}","size":20,"color":"#FFFFFFFF","weight":"bold","maxLines":1}]},
               {"type":"progress","value":"{health.stepsPct}","color":"#FF4CD137","trackColor":"#26FFFFFF","height":4,"show":{"minHeight":100},"levelOf":"{health.stepsPct}","levels":[{"from":0,"color":"#FFFFB74D"},{"from":50,"color":"#FFFFE066"},{"from":100,"color":"#FF4CD137"}]}]},
              {"type":"spacer","size":12},
              {"type":"column","align":"end","show":{"minWidth":250},"children":[
               {"type":"text","text":"{health.workout} · {health.workout.duration}","size":12,"color":"#E6FFFFFF","maxLines":1},
               {"type":"text","text":"{health.workout.when}","size":11,"color":"#99FFFFFF","maxLines":1}]}]},
             {"type":"spacer","size":10,"show":{"minHeight":120}},
             {"type":"row","show":{"minHeight":120},"children":[
              {"type":"column","grow":true,"children":[
               {"type":"icon","name":"flame","size":16,"color":"#FFFF8A50"},
               {"type":"text","text":"{health.kcal}","size":16,"color":"#FFFFFFFF","weight":"bold"},
               {"type":"text","text":"kcal","size":11,"color":"#99FFFFFF"}]},
              {"type":"column","grow":true,"children":[
               {"type":"icon","name":"heart","size":16,"color":"#FFFF5C7A"},
               {"type":"text","text":"{health.heart}","size":16,"color":"#FFFFFFFF","weight":"bold"},
               {"type":"text","text":"bpm","size":11,"color":"#99FFFFFF"}]},
              {"type":"column","grow":true,"children":[
               {"type":"icon","name":"bolt","size":16,"color":"#FF7FB3FF"},
               {"type":"text","text":"{health.effort}","size":16,"color":"#FFFFFFFF","weight":"bold"},
               {"type":"text","text":"effort","size":11,"color":"#99FFFFFF"}]}]},
             {"type":"spacer","size":10,"show":{"minHeight":220}},
             {"type":"divider","show":{"minHeight":220}},
             {"type":"spacer","size":10,"show":{"minHeight":220}},
             {"type":"row","verticalAlign":"center","show":{"minHeight":220},"children":[
              {"type":"icon","name":"moon","size":16,"color":"#FFC3B5FF"},
              {"type":"text","text":"  Slept {health.sleep}","size":14,"color":"#FFFFFFFF","grow":true,"maxLines":1},
              {"type":"spacer","size":8},{"type":"text","text":"deep {health.sleep.deep}","size":12,"color":"#99FFFFFF","show":{"minWidth":250}}]},{"type":"column","show":{"minHeight":300},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","show":{"ifSet":"{health.charge}"},"children":[{"type":"icon","name":"bolt","size":14,"color":"#FF2BE38A"},{"type":"text","text":" Charge {health.charge}%","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{health.charge.label}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"spacer","size":6},{"type":"sleep","height":10}]}]}}""",
        ),
        Triple(
            "Side Quest eggs", "Your eggs and how far to walk until they hatch; more eggs the taller it is.",
            """{"name":"Side Quest eggs","background":"#E6241B33","cornerRadius":26,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"egg","size":18,"color":"#FFFFC857"},
              {"type":"text","text":"  {mq.eggs} eggs","size":15,"color":"#FFFFFFFF","weight":"bold","grow":true},
              {"type":"spacer","size":8},{"type":"text","text":"{mq.eggs.ready} ready","size":12,"color":"#FFFFC857","show":{"minWidth":200}}]},
             ${egg(1, 100)},
             ${egg(2, 170)},
             ${egg(3, 230)},
             ${egg(4, 290)}]}}""",
        ),
        Triple(
            "Side Quest adventures", "Monsters out on adventures: where, who, and how long until they're back.",
            """{"name":"Side Quest adventures","background":"#E6122A24","cornerRadius":26,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"paw","size":18,"color":"#FF8FE3B0"},
              {"type":"text","text":"  {mq.adventures} away","size":15,"color":"#FFFFFFFF","weight":"bold","grow":true,"maxLines":1}]},
             ${adventure(1, 100)},
             ${adventure(2, 190)},
             ${adventure(3, 260)},{"type":"column","show":{"minHeight":260},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","show":{"ifSet":"{mq.quest.1}"},"children":[{"type":"icon","name":"check","size":14,"color":"#FF7CF29C","tap":"{mq.quest.1.done}"},{"type":"text","text":" {mq.quest.1}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{mq.quest.2}"},"children":[{"type":"icon","name":"check","size":14,"color":"#FF7CF29C","tap":"{mq.quest.2.done}"},{"type":"text","text":" {mq.quest.2}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{mq.quest.3}"},"children":[{"type":"icon","name":"check","size":14,"color":"#FF7CF29C","tap":"{mq.quest.3.done}"},{"type":"text","text":" {mq.quest.3}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true}]}]}]}}""",
        ),
        Triple(
            "Side Quest day", "Your hero, today's steps and torches, and last night's sleep.",
            """{"name":"Side Quest day","background":"#E61E1A2E","cornerRadius":26,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"text","text":"{mq.hero} · Level {mq.level}","size":13,"color":"#B3FFFFFF","weight":"medium","maxLines":1,"show":{"minHeight":120}},
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"steps","size":18,"color":"#FFFFC857"},
              {"type":"text","text":" {mq.steps}","size":22,"color":"#FFFFFFFF","weight":"bold","grow":true,"maxLines":1},
              {"type":"icon","name":"moon","size":16,"color":"#FFC3B5FF","show":{"minWidth":250}},
              {"type":"text","text":" {mq.sleep}","size":16,"color":"#FFFFFFFF","weight":"bold","show":{"minWidth":250}}]},
             {"type":"text","text":"steps today","size":11,"color":"#99FFFFFF","show":{"minHeight":100}},
             {"type":"spacer","size":10,"show":{"minHeight":160}},
             {"type":"row","show":{"minHeight":160},"children":[
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{mq.torches}","size":16,"color":"#FFFFFFFF","weight":"bold"},
               {"type":"text","text":"torches","size":11,"color":"#99FFFFFF"}]},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{mq.sleep}","size":14,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
               {"type":"text","text":"sleep","size":11,"color":"#99FFFFFF"}]},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{mq.eggs}","size":16,"color":"#FFFFFFFF","weight":"bold"},
               {"type":"text","text":"eggs","size":11,"color":"#99FFFFFF"}]}]},{"type":"column","show":{"minHeight":240},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","show":{"ifSet":"{mq.quest.1}"},"children":[{"type":"icon","name":"check","size":14,"color":"#FF7CF29C","tap":"{mq.quest.1.done}"},{"type":"text","text":" {mq.quest.1}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{mq.quest.2}"},"children":[{"type":"icon","name":"check","size":14,"color":"#FF7CF29C","tap":"{mq.quest.2.done}"},{"type":"text","text":" {mq.quest.2}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{mq.quest.3}"},"children":[{"type":"icon","name":"check","size":14,"color":"#FF7CF29C","tap":"{mq.quest.3.done}"},{"type":"text","text":" {mq.quest.3}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true}]}]}]}}""",
        ),
        Triple(
            "Side Quest quests", "Your next quests with a check button: tap it to complete the quest in the game.",
            """{"name":"Side Quest quests","background":"#E6182230","cornerRadius":26,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","show":{"minHeight":120},"children":[
              {"type":"text","text":"Quests","size":15,"color":"#FFFFFFFF","weight":"bold","grow":true},
              {"type":"spacer","size":8},{"type":"text","text":"{mq.quests} open","size":12,"color":"#99FFFFFF"}]},
             ${quest(1, 0)},
             ${quest(2, 150)},
             ${quest(3, 200)},
             ${quest(4, 250)},
             ${quest(5, 300)}]}}""",
        ),
        Triple(
            "Side Quest progress", "Steps to your next torch, the weekly bounty, and a phone-free session's time left.",
            """{"name":"Side Quest progress","background":"#E6261A14","cornerRadius":26,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"flame","size":16,"color":"#FFFF9F43"},
              {"type":"text","text":"  {mq.torches} torches","size":14,"color":"#FFFFFFFF","weight":"bold","grow":true},
              {"type":"spacer","size":8},{"type":"text","text":"{mq.torch.next}","size":11,"color":"#99FFFFFF","show":{"minWidth":250}}]},
             {"type":"spacer","size":4},
             {"type":"progress","value":"{mq.torch.pct}","color":"#FFFF9F43","trackColor":"#26FFFFFF","height":6},
             {"type":"column","show":{"minHeight":100,"ifSet":"{mq.focus}"},"children":[
              {"type":"spacer","size":10},
              {"type":"text","text":"{mq.focus}","size":13,"color":"#FF8AB4F8","weight":"medium","maxLines":1},
              {"type":"spacer","size":4},
              {"type":"progress","value":"{mq.focus.pct}","color":"#FF8AB4F8","trackColor":"#26FFFFFF","height":5}]},
             {"type":"spacer","size":10,"show":{"minHeight":150}},
             {"type":"row","show":{"minHeight":150},"children":[
              {"type":"text","text":"Weekly bounty","size":13,"color":"#FFFFFFFF","weight":"bold","grow":true},
              {"type":"spacer","size":8},{"type":"text","text":"{mq.bounty.ready}","size":11,"color":"#FFFFC857","show":{"ifSet":"{mq.bounty.ready}"}}]},
             ${bountyLine(1, 150)},
             ${bountyLine(2, 190)},
             ${bountyLine(3, 230)}]}}""",
        ),
        Triple(
            "Side Quest party", "Your party's monsters with their level and health.",
            """{"name":"Side Quest party","background":"#E6142618","cornerRadius":26,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"paw","size":16,"color":"#FF4CD137"},
              {"type":"text","text":"  Party","size":14,"color":"#FFFFFFFF","weight":"bold"}]},
             ${member(1, 0)},
             ${member(2, 140)},
             ${member(3, 190)},
             ${member(4, 240)},
             ${member(5, 290)},{"type":"column","show":{"minHeight":260},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","show":{"ifSet":"{mq.quest.1}"},"children":[{"type":"icon","name":"check","size":14,"color":"#FF7CF29C","tap":"{mq.quest.1.done}"},{"type":"text","text":" {mq.quest.1}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{mq.quest.2}"},"children":[{"type":"icon","name":"check","size":14,"color":"#FF7CF29C","tap":"{mq.quest.2.done}"},{"type":"text","text":" {mq.quest.2}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{mq.quest.3}"},"children":[{"type":"icon","name":"check","size":14,"color":"#FF7CF29C","tap":"{mq.quest.3.done}"},{"type":"text","text":" {mq.quest.3}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true}]}]}]}}""",
        ),
        Triple(
            "Battery", "Your battery level with a bar; the time and date when it's bigger.",
            """{"name":"Battery","background":"#E6162016","cornerRadius":24,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","children":[
              {"type":"icon","name":"bolt","size":22,"color":"#FF7CF29C"},
              {"type":"text","text":" {battery}%","size":30,"color":"#FFFFFFFF","weight":"bold","grow":true},
              {"type":"spacer","size":8},{"type":"text","text":"{charging}","size":12,"color":"#FF7CF29C","show":{"minWidth":220}}]},
             {"type":"spacer","size":10},
             {"type":"progress","value":"{battery}","color":"#FF7CF29C","trackColor":"#26FFFFFF","height":8,"levelOf":"{battery}","levels":[{"from":0,"color":"#FFFF5C5C"},{"from":16,"color":"#FFFFB74D"},{"from":31,"color":"#FF7CF29C"}]},
             {"type":"spacer","size":14,"show":{"minHeight":180}},
             {"type":"row","show":{"minHeight":180},"children":[
              {"type":"clock","format":"h:mm","size":22,"color":"#FFFFFFFF","weight":"light","grow":true},
              {"type":"clock","format":"EEE d MMM","size":13,"color":"#B3FFFFFF"}]},{"type":"column","show":{"minHeight":220},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","children":[{"type":"icon","name":"bolt","size":14,"color":"#FF7CF29C"},{"type":"text","text":" {battery.left}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true}]},{"type":"row","show":{"minHeight":250},"children":[{"type":"text","text":"Drain {battery.drain}","size":12,"color":"#99FFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{battery.temp}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"spacer","size":8},{"type":"column","children":[{"type":"row","children":[{"type":"text","text":"Memory","size":12,"color":"#CCFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{phone.ram}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"progress","value":"{phone.ramPct}","color":"#FF7CF29C","trackColor":"#26FFFFFF","height":5,"levelOf":"{phone.ramPct}","levels":[{"from":0,"color":"#FF7CF29C"},{"from":70,"color":"#FFFFB74D"},{"from":90,"color":"#FFFF5C5C"}]},{"type":"spacer","size":6}]},{"type":"column","show":{"minHeight":290},"children":[{"type":"column","children":[{"type":"row","children":[{"type":"text","text":"Storage","size":12,"color":"#CCFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{phone.storage}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"progress","value":"{phone.storagePct}","color":"#FF7CF29C","trackColor":"#26FFFFFF","height":5,"levelOf":"{phone.storagePct}","levels":[{"from":0,"color":"#FF7CF29C"},{"from":70,"color":"#FFFFB74D"},{"from":90,"color":"#FFFF5C5C"}]},{"type":"spacer","size":6}]}]}]}]}}""",
        ),
        Triple(
            "NOOP recovery", "Charge (estimated from HRV and resting heart rate) and effort from NOOP; HRV, sleep, SpO2 and more as it grows. Turn on Health Connect in NOOP.",
            """{"name":"NOOP recovery","background":"#F20B0F14","cornerRadius":28,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"Charge","size":12,"color":"#99FFFFFF","show":{"minHeight":100}},
               {"type":"row","verticalAlign":"bottom","children":[
                {"type":"text","text":"{health.charge}%","size":28,"color":"#FF2BE38A","weight":"bold","maxLines":1,"levelOf":"{health.charge}","levels":[{"from":0,"color":"#FFFF5C5C"},{"from":34,"color":"#FFFFD54F"},{"from":67,"color":"#FF2BE38A"}]},
                {"type":"text","text":" {health.charge.label}","size":13,"color":"#CCFFFFFF","maxLines":1}]}]},
              {"type":"column","align":"end","show":{"minWidth":250},"children":[
               {"type":"text","text":"Effort","size":12,"color":"#99FFFFFF","show":{"minHeight":100}},
               {"type":"text","text":"{health.effort}","size":24,"color":"#FF4FA3FF","weight":"bold"}]}]},
             {"type":"spacer","size":6,"show":{"minHeight":100}},
             {"type":"progress","value":"{health.charge}","color":"#FF2BE38A","trackColor":"#26FFFFFF","height":6,"show":{"minHeight":100},"levelOf":"{health.charge}","levels":[{"from":0,"color":"#FFFF5C5C"},{"from":34,"color":"#FFFFD54F"},{"from":67,"color":"#FF2BE38A"}]},
             {"type":"spacer","size":12,"show":{"minHeight":150}},
             {"type":"row","show":{"minHeight":150},"children":[
              {"type":"column","grow":true,"children":[
               {"type":"icon","name":"heart","size":15,"color":"#FF2BE38A"},
               {"type":"text","text":"{health.hrv}","size":15,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
               {"type":"text","text":"HRV","size":11,"color":"#99FFFFFF"}]},
              {"type":"column","grow":true,"show":{"minWidth":250},"children":[
               {"type":"icon","name":"heart","size":15,"color":"#FFFF5C7A"},
               {"type":"text","text":"{health.restingHeart}","size":15,"color":"#FFFFFFFF","weight":"bold"},
               {"type":"text","text":"resting bpm","size":11,"color":"#99FFFFFF","maxLines":1}]},
              {"type":"column","grow":true,"children":[
               {"type":"icon","name":"moon","size":15,"color":"#FFB4A0FF"},
               {"type":"text","text":"{health.sleep}","size":15,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
               {"type":"text","text":"sleep","size":11,"color":"#99FFFFFF"}]}]},
             {"type":"spacer","size":10,"show":{"minHeight":230}},
             {"type":"row","show":{"minHeight":230},"children":[
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{health.spo2}","size":14,"color":"#FFFFFFFF","weight":"bold"},
               {"type":"text","text":"SpO2","size":11,"color":"#99FFFFFF"}]},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{health.respRate}","size":14,"color":"#FFFFFFFF","weight":"bold"},
               {"type":"text","text":"br/min","size":11,"color":"#99FFFFFF","maxLines":1}]},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{health.vo2max}","size":14,"color":"#FFFFFFFF","weight":"bold"},
               {"type":"text","text":"VO2 max","size":11,"color":"#99FFFFFF","maxLines":1}]}]},{"type":"column","show":{"minHeight":320},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","children":[{"type":"text","text":"Last night {health.sleep}","size":12,"color":"#CCFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{health.sleep.bed} – {health.sleep.wake}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"spacer","size":6},{"type":"sleep","height":10}]}]}}""",
        ),
        Triple(
            "Now playing", "The song, artist and album art with play/pause, next and previous; big art when tall. Allow music access in Settings.",
            """{"name":"Now playing","background":"#F2181A20","cornerRadius":28,"padding":14,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"art","size":120,"cornerRadius":18,"show":{"minHeight":250}},
             {"type":"spacer","size":12,"show":{"minHeight":250}},
             {"type":"row","verticalAlign":"center","children":[
              {"type":"art","size":56,"cornerRadius":12,"show":{"minWidth":220,"maxHeight":249}},
              {"type":"spacer","size":12,"show":{"minWidth":220,"maxHeight":249}},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{music.title}","size":15,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
               {"type":"text","text":"{music.artist}","size":12,"color":"#B3FFFFFF","maxLines":1,"show":{"minHeight":100}},
               {"type":"spacer","size":6,"show":{"minHeight":100}},
               {"type":"row","verticalAlign":"center","show":{"minHeight":140,"ifSet":"{music.length}"},"children":[
                {"type":"timer","of":"music","size":11,"color":"#99FFFFFF"},
                {"type":"spacer","size":8},
                {"type":"progress","value":"{music.progress}","color":"#FFFFFFFF","trackColor":"#33FFFFFF","height":4,"grow":true},
                {"type":"spacer","size":8},
                {"type":"text","text":"{music.length}","size":11,"color":"#99FFFFFF","maxLines":1}]},
               {"type":"spacer","size":6,"show":{"minHeight":140,"ifSet":"{music.length}"}},
               {"type":"row","verticalAlign":"center","children":[
                {"type":"icon","name":"previous","size":24,"color":"#E6FFFFFF","tap":"music:prev"},
                {"type":"spacer","size":16},
                {"type":"icon","name":"play","live":"{music.playIcon}","size":30,"color":"#FFFFFFFF","tap":"music:toggle"},
                {"type":"spacer","size":16},
                {"type":"icon","name":"next","size":24,"color":"#E6FFFFFF","tap":"music:next"},
                {"type":"spacer","grow":true},
                {"type":"text","text":"{music.app}","size":11,"color":"#80FFFFFF","maxLines":1,"show":{"minWidth":300}}]}]}]},{"type":"column","show":{"minHeight":300,"ifSet":"{music.album}"},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"text","text":"{music.album} · {music.app}","size":12,"color":"#99FFFFFF","maxLines":1}]}]}}""",
        ),
        Triple(
            "Music mini", "What's playing with play/pause (and next when wider); the album art and artist when bigger. Pick the Pill shape for a slim oval.",
            """{"name":"Music mini","background":"#E6202124","cornerRadius":26,"padding":12,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"art","size":64,"cornerRadius":14,"show":{"minHeight":150}},
             {"type":"spacer","size":8,"show":{"minHeight":150}},
             {"type":"row","verticalAlign":"center","children":[
              {"type":"art","size":36,"cornerRadius":18,"show":{"minWidth":200,"maxHeight":149}},
              {"type":"spacer","size":10,"show":{"minWidth":200,"maxHeight":149}},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{music.title}","size":13,"color":"#FFFFFFFF","weight":"medium","maxLines":1},
               {"type":"text","text":"{music.artist}","size":11,"color":"#B3FFFFFF","maxLines":1,"show":{"minHeight":130}}]},
              {"type":"icon","name":"play","live":"{music.playIcon}","size":26,"color":"#FFFFFFFF","tap":"music:toggle"},
              {"type":"spacer","size":10,"show":{"minWidth":240}},
              {"type":"icon","name":"next","size":22,"color":"#CCFFFFFF","tap":"music:next","show":{"minWidth":240}}]},{"type":"column","show":{"minHeight":230},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","show":{"ifSet":"{music.length}"},"children":[{"type":"timer","of":"music","size":11,"color":"#99FFFFFF"},{"type":"spacer","size":8},{"type":"progress","value":"{music.progress}","color":"#FFFFFFFF","trackColor":"#33FFFFFF","height":4,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{music.length}","size":11,"color":"#99FFFFFF","maxLines":1}]},{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","children":[{"type":"spacer","grow":true},{"type":"icon","name":"previous","size":24,"color":"#E6FFFFFF","tap":"music:prev"},{"type":"spacer","size":24},{"type":"icon","name":"next","size":24,"color":"#E6FFFFFF","tap":"music:next"},{"type":"spacer","grow":true}]}]}]}}""",
        ),
        Triple(
            "Sleep", "Last night's sleep with its stages (awake, light, deep, REM) and how it compares with your week. From NOOP, Samsung Health and others.",
            """{"name":"Sleep","background":"#F2121530","cornerRadius":28,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"moon","size":18,"color":"#FFC79BFF"},
              {"type":"text","text":" {health.sleep}","size":24,"color":"#FFFFFFFF","weight":"bold","maxLines":1,"grow":true},
              {"type":"spacer","size":8},{"type":"text","text":"avg {health.sleep.avg}","size":12,"color":"#99FFFFFF","maxLines":1,"show":{"minWidth":250,"ifSet":"{health.sleep.avg}"}}]},
             {"type":"text","text":"{health.sleep.vsAvg}","size":12,"color":"#B3FFFFFF","maxLines":1,"show":{"minHeight":100,"ifSet":"{health.sleep.vsAvg}"}},
             {"type":"spacer","size":8,"show":{"minHeight":100}},
             {"type":"sleep","height":14,"show":{"minHeight":100}},
             {"type":"spacer","size":10,"show":{"minHeight":150}},
             {"type":"row","show":{"minHeight":150},"children":[
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"Deep","size":11,"color":"#FF7F8BEA"},
               {"type":"text","text":"{health.sleep.deep}","size":14,"color":"#FFFFFFFF","weight":"bold","maxLines":1}]},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"REM","size":11,"color":"#FFC79BFF"},
               {"type":"text","text":"{health.sleep.rem}","size":14,"color":"#FFFFFFFF","weight":"bold","maxLines":1}]},
              {"type":"column","grow":true,"show":{"minWidth":250},"children":[
               {"type":"text","text":"Light","size":11,"color":"#FF8AB4FF"},
               {"type":"text","text":"{health.sleep.light}","size":14,"color":"#FFFFFFFF","weight":"bold","maxLines":1}]},
              {"type":"column","grow":true,"show":{"minWidth":250},"children":[
               {"type":"text","text":"Awake","size":11,"color":"#FFFF9E6B"},
               {"type":"text","text":"{health.sleep.awake}","size":14,"color":"#FFFFFFFF","weight":"bold","maxLines":1}]}]},{"type":"column","show":{"minHeight":210},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","children":[{"type":"text","text":"Bed {health.sleep.bed}","size":12,"color":"#CCFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"Up {health.sleep.wake}","size":12,"color":"#CCFFFFFF","maxLines":1}]},{"type":"spacer","size":8,"show":{"minHeight":260}},{"type":"column","show":{"minHeight":260,"ifSet":"{health.hrv}"},"children":[{"type":"row","children":[{"type":"column","grow":true,"children":[{"type":"text","text":"{health.restingHeart}","size":14,"color":"#FFFFFFFF","maxLines":1,"weight":"bold"},{"type":"text","text":"resting bpm","size":11,"color":"#99FFFFFF","maxLines":1}]},{"type":"column","grow":true,"children":[{"type":"text","text":"{health.hrv}","size":14,"color":"#FFFFFFFF","maxLines":1,"weight":"bold"},{"type":"text","text":"HRV","size":11,"color":"#99FFFFFF","maxLines":1}]},{"type":"column","grow":true,"children":[{"type":"text","text":"{health.respRate}","size":14,"color":"#FFFFFFFF","maxLines":1,"weight":"bold"},{"type":"text","text":"br/min","size":11,"color":"#99FFFFFF","maxLines":1}]}]}]},{"type":"column","show":{"minHeight":290,"minWidth":250},"children":[{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","show":{"ifSet":"{health.sleep.n1}"},"children":[{"type":"text","text":"{health.sleep.n1.day}","size":11,"color":"#99FFFFFF","maxLines":1},{"type":"progress","value":"{health.sleep.n1.pct}","color":"#FFC79BFF","trackColor":"#1FFFFFFF","height":6,"grow":true},{"type":"text","text":"  {health.sleep.n1}","size":11,"color":"#CCFFFFFF","maxLines":1}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{health.sleep.n2}"},"children":[{"type":"text","text":"{health.sleep.n2.day}","size":11,"color":"#99FFFFFF","maxLines":1},{"type":"progress","value":"{health.sleep.n2.pct}","color":"#FFC79BFF","trackColor":"#1FFFFFFF","height":6,"grow":true},{"type":"text","text":"  {health.sleep.n2}","size":11,"color":"#CCFFFFFF","maxLines":1}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{health.sleep.n3}"},"children":[{"type":"text","text":"{health.sleep.n3.day}","size":11,"color":"#99FFFFFF","maxLines":1},{"type":"progress","value":"{health.sleep.n3.pct}","color":"#FFC79BFF","trackColor":"#1FFFFFFF","height":6,"grow":true},{"type":"text","text":"  {health.sleep.n3}","size":11,"color":"#CCFFFFFF","maxLines":1}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{health.sleep.n4}"},"children":[{"type":"text","text":"{health.sleep.n4.day}","size":11,"color":"#99FFFFFF","maxLines":1},{"type":"progress","value":"{health.sleep.n4.pct}","color":"#FFC79BFF","trackColor":"#1FFFFFFF","height":6,"grow":true},{"type":"text","text":"  {health.sleep.n4}","size":11,"color":"#CCFFFFFF","maxLines":1}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{health.sleep.n5}"},"children":[{"type":"text","text":"{health.sleep.n5.day}","size":11,"color":"#99FFFFFF","maxLines":1},{"type":"progress","value":"{health.sleep.n5.pct}","color":"#FFC79BFF","trackColor":"#1FFFFFFF","height":6,"grow":true},{"type":"text","text":"  {health.sleep.n5}","size":11,"color":"#CCFFFFFF","maxLines":1}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{health.sleep.n6}"},"children":[{"type":"text","text":"{health.sleep.n6.day}","size":11,"color":"#99FFFFFF","maxLines":1},{"type":"progress","value":"{health.sleep.n6.pct}","color":"#FFC79BFF","trackColor":"#1FFFFFFF","height":6,"grow":true},{"type":"text","text":"  {health.sleep.n6}","size":11,"color":"#CCFFFFFF","maxLines":1}]},{"type":"row","verticalAlign":"center","show":{"ifSet":"{health.sleep.n7}"},"children":[{"type":"text","text":"{health.sleep.n7.day}","size":11,"color":"#99FFFFFF","maxLines":1},{"type":"progress","value":"{health.sleep.n7.pct}","color":"#FFC79BFF","trackColor":"#1FFFFFFF","height":6,"grow":true},{"type":"text","text":"  {health.sleep.n7}","size":11,"color":"#CCFFFFFF","maxLines":1}]}]}]}]}}""",
        ),
        Triple(
            "Daily briefing", "Changes with the day: weather, your first event, commute and Charge in the morning; steps left and tomorrow's first event later on.",
            """{"name":"Daily briefing","background":"#F2172030","cornerRadius":28,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"text","text":"{greeting}","size":13,"color":"#B3FFFFFF","maxLines":1,"show":{"minHeight":100}},
             {"type":"column","show":{"ifSet":"{time.morning}"},"children":[
              {"type":"row","verticalAlign":"center","children":[
               {"type":"icon","name":"weather","size":22,"color":"#FFFFD27A"},
               {"type":"text","text":" {weather.temp}","size":22,"color":"#FFFFFFFF","weight":"bold"},
               {"type":"text","text":"  {weather.outlook}","size":12,"color":"#CCFFFFFF","maxLines":1,"grow":true,"show":{"minWidth":250}}]},
              {"type":"spacer","size":8,"show":{"minHeight":120}},
              {"type":"row","verticalAlign":"center","show":{"minHeight":120,"ifSet":"{calendar.1}"},"children":[
               {"type":"icon","name":"calendar","size":14,"color":"#FF8AB4FF"},
               {"type":"text","text":" {calendar.1}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},
               {"type":"spacer","size":8,"show":{"minWidth":230}},{"type":"text","text":"{calendar.1.when}","size":12,"color":"#99FFFFFF","maxLines":1,"show":{"minWidth":230}}]},
              {"type":"text","text":"     {calendar.1.when}","size":11,"color":"#99FFFFFF","maxLines":1,"show":{"maxWidth":229,"minHeight":120,"ifSet":"{calendar.1}"}},
              {"type":"row","verticalAlign":"center","show":{"minHeight":150,"ifSet":"{traffic.commute}"},"children":[
               {"type":"icon","name":"car","size":14,"color":"#FFFFB74D"},
               {"type":"text","text":" {traffic.commute} to {traffic.to}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},
               {"type":"spacer","size":8,"show":{"minWidth":230}},{"type":"text","text":"{traffic.delay}","size":12,"color":"#99FFFFFF","maxLines":1,"show":{"minWidth":230}}]},
              {"type":"row","verticalAlign":"center","show":{"minHeight":170,"ifSet":"{health.charge}"},"children":[
               {"type":"icon","name":"bolt","size":14,"color":"#FF2BE38A","levelOf":"{health.charge}","levels":[{"from":0,"color":"#FFFF5C5C"},{"from":34,"color":"#FFFFD54F"},{"from":67,"color":"#FF2BE38A"}]},
               {"type":"text","text":" Charge {health.charge}%","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},
               {"type":"spacer","size":8,"show":{"minWidth":230}},{"type":"text","text":"{health.charge.label}","size":12,"color":"#99FFFFFF","maxLines":1,"show":{"minWidth":230}}]}]},
             {"type":"column","show":{"ifEmpty":"{time.morning}"},"children":[
              {"type":"row","verticalAlign":"center","children":[
               {"type":"icon","name":"steps","size":18,"color":"#FF4CD137","levelOf":"{health.stepsPct}","levels":[{"from":0,"color":"#FFFFB74D"},{"from":50,"color":"#FFFFE066"},{"from":100,"color":"#FF4CD137"}]},
               {"type":"text","text":" {health.stepsLeft.text}","size":16,"color":"#FFFFFFFF","weight":"bold","maxLines":1,"grow":true}]},
              {"type":"progress","value":"{health.stepsPct}","color":"#FF4CD137","trackColor":"#26FFFFFF","height":5,"show":{"minHeight":100},"levelOf":"{health.stepsPct}","levels":[{"from":0,"color":"#FFFFB74D"},{"from":50,"color":"#FFFFE066"},{"from":100,"color":"#FF4CD137"}]},
              {"type":"spacer","size":10,"show":{"minHeight":120}},
              {"type":"text","text":"Tomorrow","size":11,"color":"#99FFFFFF","show":{"minHeight":120,"ifSet":"{calendar.tomorrow}"}},
              {"type":"row","verticalAlign":"center","show":{"minHeight":120,"ifSet":"{calendar.tomorrow}"},"children":[
               {"type":"icon","name":"calendar","size":14,"color":"#FF8AB4FF"},
               {"type":"text","text":" {calendar.tomorrow}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},
               {"type":"spacer","size":8},{"type":"text","text":"{calendar.tomorrow.when}","size":12,"color":"#99FFFFFF","maxLines":1}]},
              {"type":"row","verticalAlign":"center","show":{"minHeight":160},"children":[
               {"type":"icon","name":"weather","size":14,"color":"#FFFFD27A"},
               {"type":"text","text":" {weather.temp} · {weather.outlook}","size":12,"color":"#CCFFFFFF","maxLines":1}]}]},{"type":"column","show":{"minHeight":260},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","verticalAlign":"center","show":{"ifSet":"{calendar.2}"},"children":[{"type":"icon","name":"calendar","size":14,"color":"#FF8AB4FF"},{"type":"text","text":" {calendar.2}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{calendar.2.when}","size":12,"color":"#99FFFFFF","maxLines":1}]},{"type":"spacer","size":8},{"type":"row","children":[${hour(1)},${hour(2)},${hour(3)},${hour(4)}]}]}]}}""",
        ),
        Triple(
            "Countdown", "Days until something you're looking forward to; the next ones when taller. Add countdowns in Settings.",
            """{"name":"Countdown","background":"#F23A1D4A","cornerRadius":28,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","show":{"maxHeight":129},"children":[
              {"type":"text","text":"{countdown.1}","size":13,"color":"#CCFFFFFF","maxLines":2,"grow":true},
              {"type":"spacer","size":8},{"type":"text","text":"{countdown.1.days}","size":26,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
              {"type":"text","text":" {countdown.1.unit}","size":12,"color":"#B3FFFFFF","maxLines":1}]},
             {"type":"text","text":"{countdown.1}","size":13,"color":"#CCFFFFFF","maxLines":1,"show":{"minHeight":130}},
             {"type":"row","verticalAlign":"bottom","show":{"minHeight":130},"children":[
              {"type":"text","text":"{countdown.1.days}","size":40,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
              {"type":"text","text":" {countdown.1.unit}","size":14,"color":"#B3FFFFFF","maxLines":1}]},
             {"type":"text","text":"{countdown.1.date}","size":12,"color":"#99FFFFFF","show":{"minHeight":130}},
             {"type":"spacer","size":10,"show":{"minHeight":200,"ifSet":"{countdown.2}"}},
             {"type":"divider","color":"#26FFFFFF","show":{"minHeight":200,"ifSet":"{countdown.2}"}},
             {"type":"spacer","size":8,"show":{"minHeight":200,"ifSet":"{countdown.2}"}},
             {"type":"row","show":{"minHeight":200,"ifSet":"{countdown.2}"},"children":[
              {"type":"text","text":"{countdown.2}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},
              {"type":"spacer","size":8},{"type":"text","text":"{countdown.2.text}","size":13,"color":"#B3FFFFFF"}]},
             {"type":"row","show":{"minHeight":230,"ifSet":"{countdown.3}"},"children":[
              {"type":"text","text":"{countdown.3}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},
              {"type":"spacer","size":8},{"type":"text","text":"{countdown.3.text}","size":13,"color":"#B3FFFFFF"}]},{"type":"column","show":{"minHeight":290,"ifSet":"{countdown.4}"},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"row","show":{"ifSet":"{countdown.4}"},"children":[{"type":"text","text":"{countdown.4}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{countdown.4.text}","size":13,"color":"#B3FFFFFF","maxLines":1}]},{"type":"row","show":{"ifSet":"{countdown.5}"},"children":[{"type":"text","text":"{countdown.5}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{countdown.5.text}","size":13,"color":"#B3FFFFFF","maxLines":1}]}]}]}}""",
        ),
        Triple(
            "Smart stack", "One widget that shows what matters now: your next event when it starts within the hour, what's playing, or else the weather.",
            """{"name":"Smart stack","background":"#F21A1C24","cornerRadius":28,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"column","show":{"ifSet":"{calendar.soon}"},"tap":"{calendar.1.link}","children":[
              {"type":"row","verticalAlign":"center","children":[
               {"type":"icon","name":"calendar","size":16,"color":"#FF8AB4FF"},
               {"type":"text","text":" {calendar.soon.in}","size":12,"color":"#FF8AB4FF","weight":"medium"}]},
              {"type":"text","text":"{calendar.1}","size":18,"color":"#FFFFFFFF","weight":"bold","maxLines":2},
              {"type":"text","text":"{calendar.1.where}","size":12,"color":"#99FFFFFF","maxLines":1,"show":{"minHeight":110,"ifSet":"{calendar.1.where}"}}]},
             {"type":"row","verticalAlign":"center","show":{"ifSet":"{music.playing}","ifEmpty":"{calendar.soon}"},"children":[
              {"type":"art","size":52,"cornerRadius":12,"show":{"minWidth":220}},
              {"type":"spacer","size":12,"show":{"minWidth":220}},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{music.title}","size":15,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
               {"type":"text","text":"{music.artist}","size":12,"color":"#B3FFFFFF","maxLines":1,"show":{"minHeight":100}},
               {"type":"row","verticalAlign":"center","children":[
                {"type":"icon","name":"previous","size":22,"color":"#E6FFFFFF","tap":"music:prev"},
                {"type":"spacer","size":14},
                {"type":"icon","name":"play","live":"{music.playIcon}","size":28,"color":"#FFFFFFFF","tap":"music:toggle"},
                {"type":"spacer","size":14},
                {"type":"icon","name":"next","size":22,"color":"#E6FFFFFF","tap":"music:next"}]}]}]},
             {"type":"column","show":{"ifEmpty":"{calendar.soon}{music.playing}"},"children":[
              {"type":"row","verticalAlign":"center","children":[
               {"type":"icon","name":"weather","size":30,"color":"#FFC9DDE6"},
               {"type":"text","text":" {weather.temp}","size":28,"color":"#FFFFFFFF","weight":"medium","maxLines":1}]},
              {"type":"text","text":"{weather.outlook}","size":12,"color":"#CCFFFFFF","maxLines":1,"show":{"minHeight":100}},
              {"type":"text","text":"H {weather.high}  L {weather.low}","size":12,"color":"#99FFFFFF","show":{"minHeight":130}}]},{"type":"column","show":{"minHeight":240},"children":[{"type":"spacer","size":10},{"type":"divider","color":"#26FFFFFF"},{"type":"spacer","size":8},{"type":"column","show":{"ifEmpty":"{calendar.soon}{music.playing}"},"children":[{"type":"row","children":[${hour(1)},${hour(2)},${hour(3)},${hour(4)}]}]},{"type":"column","show":{"ifSet":"{calendar.soon}"},"children":[{"type":"row","verticalAlign":"center","show":{"ifSet":"{calendar.2}"},"children":[{"type":"icon","name":"calendar","size":14,"color":"#FF8AB4FF"},{"type":"text","text":" {calendar.2}","size":13,"color":"#FFFFFFFF","maxLines":1,"grow":true},{"type":"spacer","size":8},{"type":"text","text":"{calendar.2.when}","size":12,"color":"#99FFFFFF","maxLines":1}]}]},{"type":"column","show":{"ifSet":"{music.playing}","ifEmpty":"{calendar.soon}"},"children":[{"type":"row","verticalAlign":"center","show":{"ifSet":"{music.length}"},"children":[{"type":"timer","of":"music","size":11,"color":"#99FFFFFF"},{"type":"spacer","size":8},{"type":"progress","value":"{music.progress}","color":"#FFFFFFFF","trackColor":"#33FFFFFF","height":4,"grow":true}]}]}]}]}}""",
        ),
        Triple(
            "Heart rate", "Like NOOP's heart rate card: now, today's low and high, and the day as a line chart. From NOOP, Samsung Health and others via Health Connect.",
            """{"name":"Heart rate","background":"#FAF5F1E8","cornerRadius":28,"padding":16,"root":
            {"type":"column","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"heart","size":16,"color":"#FFD2542A"},
              {"type":"text","text":" Heart rate","size":15,"color":"#FF1C1F26","weight":"medium","maxLines":1}]},
             {"type":"spacer","size":6,"show":{"minHeight":100}},
             {"type":"row","verticalAlign":"center","children":[
              {"type":"text","text":"{health.heart.last}","size":32,"color":"#FF1C1F26","weight":"bold","maxLines":1},
              {"type":"text","text":" bpm","size":13,"color":"#FF6B7280","maxLines":1},
              {"type":"spacer","size":12,"show":{"minWidth":230}},
              {"type":"column","background":"#33D2542A","cornerRadius":12,"padding":6,"show":{"minWidth":230,"ifSet":"{health.heartMax}"},"children":[
               {"type":"text","text":"Min {health.heartMin} • Max {health.heartMax}","size":12,"color":"#FF1C1F26","maxLines":1}]}]},
             {"type":"text","text":"Min {health.heartMin} • Max {health.heartMax}","size":11,"color":"#FF6B7280","maxLines":1,"show":{"maxWidth":229,"minHeight":100,"ifSet":"{health.heartMax}"}},
             {"type":"text","text":"No heart rate readings yet. Settings → Exercise & health → Check what Health Connect shares.","size":12,"color":"#FF6B7280","maxLines":3,"show":{"minHeight":100,"ifEmpty":"{health.heart.series}"}},
             {"type":"spacer","size":8,"show":{"minHeight":150}},
             {"type":"row","show":{"minHeight":150},"children":[
              {"type":"chart","series":"{health.heart.series}","height":84,"color":"#FFD2542A","grow":true},
              {"type":"spacer","size":8,"show":{"minWidth":250,"minHeight":200}},
              {"type":"column","show":{"minWidth":250,"minHeight":200},"children":[
               {"type":"text","text":"{health.heartMax}","size":11,"color":"#FF6B7280"},
               {"type":"spacer","size":56},
               {"type":"text","text":"{health.heartMin}","size":11,"color":"#FF6B7280"}]}]},
             {"type":"spacer","size":4,"show":{"minHeight":150}},
             {"type":"row","show":{"minHeight":150,"ifSet":"{health.heart.series}"},"children":[
              {"type":"text","text":"{health.heart.start}","size":11,"color":"#FF6B7280","maxLines":1},
              {"type":"spacer","grow":true},
              {"type":"text","text":"{health.heart.mid}","size":11,"color":"#FF6B7280","maxLines":1,"show":{"minWidth":250}},
              {"type":"spacer","grow":true},
              {"type":"text","text":"{health.heart.end}","size":11,"color":"#FF6B7280","maxLines":1}]},
             {"type":"row","show":{"minHeight":360},"children":[
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{health.restingHeart}","size":18,"color":"#FF1C1F26","weight":"bold","maxLines":1},
               {"type":"text","text":"resting bpm","size":11,"color":"#FF6B7280","maxLines":1}]},
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"{health.hrv}","size":18,"color":"#FF1C1F26","weight":"bold","maxLines":1},
               {"type":"text","text":"HRV","size":11,"color":"#FF6B7280","maxLines":1}]}]},
             {"type":"spacer","size":8,"show":{"minHeight":360}},
             {"type":"row","show":{"minHeight":260},"children":[
              {"type":"spacer","grow":true},
              {"type":"text","text":"Updated {updated}","size":11,"color":"#FF6B7280","maxLines":1},
              {"type":"spacer","grow":true}]}]}}""",
        ),
        Triple(
            "NOOP rings", "Charge, Effort and Rest as rings, like NOOP's: NOOP's own scores straight from not-NOOP; with NOOP via Health Connect, estimates (Rest is last night's sleep against 8 hours).",
            """{"name":"NOOP rings","background":"#E6C6D9F0","cornerRadius":28,"padding":14,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[{"type":"column","align":"center","grow":true,"show":{"maxWidth":249},"children":[{"type":"ring","value":"{health.charge}","text":"{health.charge}","size":44,"thickness":5,"textSize":14,"color":"#FFD4A23A","trackColor":"#33506080","textColor":"#FF1C2433"},{"type":"spacer","size":6,"show":{"minHeight":100}},{"type":"text","text":"CHARGE","size":10,"color":"#FF3B4656","weight":"bold","maxLines":1,"show":{"minHeight":100}}]},{"type":"column","align":"center","grow":true,"show":{"maxWidth":249},"children":[{"type":"ring","value":"{health.effort.pct}","text":"{health.effort}","size":44,"thickness":5,"textSize":14,"color":"#FFB0681F","trackColor":"#33506080","textColor":"#FF1C2433"},{"type":"spacer","size":6,"show":{"minHeight":100}},{"type":"text","text":"EFFORT","size":10,"color":"#FF3B4656","weight":"bold","maxLines":1,"show":{"minHeight":100}}]},{"type":"column","align":"center","grow":true,"show":{"maxWidth":249},"children":[{"type":"ring","value":"{health.rest}","text":"{health.rest}","size":44,"thickness":5,"textSize":14,"color":"#FFDDAE45","trackColor":"#33506080","textColor":"#FF1C2433"},{"type":"spacer","size":6,"show":{"minHeight":100}},{"type":"text","text":"REST","size":10,"color":"#FF3B4656","weight":"bold","maxLines":1,"show":{"minHeight":100}}]},{"type":"column","align":"center","grow":true,"show":{"minWidth":250},"children":[{"type":"ring","value":"{health.charge}","text":"{health.charge}","size":76,"thickness":8,"textSize":24,"color":"#FFD4A23A","trackColor":"#33506080","textColor":"#FF1C2433"},{"type":"spacer","size":6,"show":{"minHeight":100}},{"type":"text","text":"CHARGE","size":12,"color":"#FF3B4656","weight":"bold","maxLines":1,"show":{"minHeight":100}}]},{"type":"column","align":"center","grow":true,"show":{"minWidth":250},"children":[{"type":"ring","value":"{health.effort.pct}","text":"{health.effort}","size":76,"thickness":8,"textSize":24,"color":"#FFB0681F","trackColor":"#33506080","textColor":"#FF1C2433"},{"type":"spacer","size":6,"show":{"minHeight":100}},{"type":"text","text":"EFFORT","size":12,"color":"#FF3B4656","weight":"bold","maxLines":1,"show":{"minHeight":100}}]},{"type":"column","align":"center","grow":true,"show":{"minWidth":250},"children":[{"type":"ring","value":"{health.rest}","text":"{health.rest}","size":76,"thickness":8,"textSize":24,"color":"#FFDDAE45","trackColor":"#33506080","textColor":"#FF1C2433"},{"type":"spacer","size":6,"show":{"minHeight":100}},{"type":"text","text":"REST","size":12,"color":"#FF3B4656","weight":"bold","maxLines":1,"show":{"minHeight":100}}]}]},
             {"type":"spacer","size":10,"show":{"minHeight":200}},
             {"type":"row","show":{"minHeight":200},"children":[
              {"type":"spacer","grow":true},
              {"type":"text","text":"{health.charge.label} · Slept {health.sleep}","size":12,"color":"#FF3B4656","maxLines":1},
              {"type":"spacer","grow":true}]}]}}""",
        ),
        Triple(
            "Stress", "Today's stress from not-NOOP on NOOP's 0-3 scale, with the day's curve, peak and average when there's room.",
            """{"name":"Stress","background":"#FFF4F0E6","cornerRadius":28,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"text","text":"Stress","size":13,"color":"#FF1C2433","weight":"medium","grow":true,"maxLines":1},
              {"type":"text","text":"{noop.stress.label}","size":12,"color":"#FF5A6472","maxLines":1,"show":{"ifSet":"{noop.stress}"},"levelOf":"{noop.stress.pct}","levels":[{"from":0,"color":"#FF3FA37A"},{"from":34,"color":"#FFD99A2B"},{"from":67,"color":"#FFD9534F"}]}]},
             {"type":"row","verticalAlign":"bottom","show":{"ifSet":"{noop.stress}"},"children":[
              {"type":"text","text":"{noop.stress}","size":34,"color":"#FF1C2433","weight":"bold","maxLines":1},
              {"type":"text","text":" of 3","size":13,"color":"#FF5A6472","maxLines":1,"grow":true},
              {"type":"text","text":"Peak {noop.stress.peak} · {noop.stress.peakAt}","size":12,"color":"#FF5A6472","maxLines":1,"show":{"minWidth":250}}]},
             {"type":"text","text":"Shows once not-NOOP has scored an hour today.","size":12,"color":"#FF5A6472","maxLines":3,"show":{"ifEmpty":"{noop.stress}"}},
             {"type":"spacer","size":8,"show":{"minHeight":130,"ifSet":"{noop.stress.series}"}},
             {"type":"chart","series":"{noop.stress.series}","height":48,"min":0,"max":300,"color":"#FF3FA37A","show":{"minHeight":130,"maxHeight":249,"ifSet":"{noop.stress.series}"}},
             {"type":"chart","series":"{noop.stress.series}","height":120,"min":0,"max":300,"color":"#FF3FA37A","show":{"minHeight":250,"ifSet":"{noop.stress.series}"}},
             {"type":"row","show":{"minHeight":160,"ifSet":"{noop.stress.series}"},"children":[
              {"type":"text","text":"{noop.stress.start}","size":11,"color":"#FF5A6472","grow":true,"maxLines":1},
              {"type":"text","text":"{noop.stress.end}","size":11,"color":"#FF5A6472","maxLines":1}]},
             {"type":"text","text":"Avg {noop.stress.avg} · peak {noop.stress.peak} at {noop.stress.peakAt}","size":12,"color":"#FF5A6472","maxLines":1,"show":{"minHeight":200,"maxWidth":249,"ifSet":"{noop.stress}"}},
             {"type":"text","text":"Average {noop.stress.avg} today","size":12,"color":"#FF5A6472","maxLines":1,"show":{"minHeight":200,"minWidth":250,"ifSet":"{noop.stress}"}}]}}""",
        ),
        Triple(
            "Coach brief", "not-NOOP's daily Coach brief: the first point on a small widget, the whole brief on a big one. Turn the brief on in not-NOOP.",
            """{"name":"Coach brief","background":"#F20D1626","cornerRadius":28,"padding":16,"root":
            {"type":"column","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"star","size":14,"color":"#FFE8B64C"},
              {"type":"text","text":" Coach brief","size":14,"color":"#FFE8B64C","weight":"bold","grow":true,"maxLines":1},
              {"type":"text","text":"{noop.coach.time}","size":12,"color":"#99FFFFFF","maxLines":1,"show":{"minWidth":200}}]},
             {"type":"spacer","size":8},
             {"type":"text","text":"Turn on the Coach brief in not-NOOP to see it here.","size":13,"color":"#CCFFFFFF","maxLines":3,"show":{"ifEmpty":"{noop.coach}"}},
             {"type":"text","text":"{noop.coach.summary}","size":13,"color":"#FFFFFFFF","maxLines":5,"show":{"maxHeight":249,"ifSet":"{noop.coach}"}},
             {"type":"text","text":"{noop.coach.title}","size":15,"color":"#FFFFFFFF","weight":"bold","maxLines":1,"show":{"minHeight":250,"ifSet":"{noop.coach}"}},
             {"type":"spacer","size":6,"show":{"minHeight":250,"ifSet":"{noop.coach}"}},
             {"type":"text","text":"{noop.coach.body}","size":13,"color":"#E6FFFFFF","maxLines":18,"show":{"minHeight":250,"ifSet":"{noop.coach}"}}]}}""",
        ),
    )
}
