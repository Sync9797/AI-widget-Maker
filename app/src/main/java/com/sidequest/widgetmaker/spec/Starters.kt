package com.sidequest.widgetmaker.spec

/**
 * Ready-made designs that work without any AI: pick one, save it, add it.
 * Each is responsive (more appears as the widget grows) and suits every theme.
 */
object Starters {
    data class Starter(val title: String, val about: String, val design: WidgetDesign)

    val all: List<Starter> by lazy {
        SOURCES.map { (title, about, json) -> Starter(title, about, DesignParser.parse(json)) }
    }

    /** One forecast hour; hours 4 and 5 only fit on wide widgets. */
    private fun hour(n: Int) = """
        {"type":"column","align":"center","grow":true,${if (n > 3) "\"show\":{\"minWidth\":250}," else ""}"children":[
         {"type":"text","text":"{weather.h$n.time}","size":11,"color":"#99FFFFFF","align":"center"},
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
          {"type":"icon","name":"umbrella","size":12,"color":"#99FFFFFF"},
          {"type":"text","text":" {weather.h$n.rain}","size":13,"color":"#B3FFFFFF","grow":true},
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
          {"type":"text","text":"{mq.egg.$n.left}","size":11,"color":"#B3FFFFFF","maxLines":1,"show":{"minWidth":250}},
          {"type":"text","text":"  Hatch","size":12,"color":"#FFFFC857","weight":"bold","tap":"{mq.egg.$n.hatch}","show":{"ifSet":"{mq.egg.$n.hatch}"}}]},
         {"type":"text","text":"{mq.egg.$n.left}","size":11,"color":"#B3FFFFFF","maxLines":1,"show":{"maxWidth":249}},
         {"type":"spacer","size":4},
         {"type":"progress","value":"{mq.egg.$n.progress}","color":"#FFFFC857","trackColor":"#26FFFFFF","height":5}]}"""

    private fun adventure(n: Int, minHeight: Int) = """
        {"type":"column","show":{"minHeight":$minHeight,"ifSet":"{mq.adv.$n}"},"children":[
         {"type":"spacer","size":10},
         {"type":"row","verticalAlign":"center","children":[
          {"type":"text","text":"{mq.adv.$n}","size":14,"color":"#FFFFFFFF","weight":"bold","maxLines":1,"grow":true},
          {"type":"text","text":"Claim","size":12,"color":"#FF8FE3B0","weight":"bold","tap":"{mq.adv.$n.claim}","show":{"ifSet":"{mq.adv.$n.claim}"}}]},
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
          {"type":"text","text":"{mq.party.$n.level}","size":11,"color":"#99FFFFFF"},
          {"type":"text","text":"  {mq.party.$n.hp}","size":11,"color":"#CCFFFFFF","show":{"minWidth":250}}]},
         {"type":"spacer","size":4},
         {"type":"progress","value":"{mq.party.$n.hpPct}","color":"#FF4CD137","trackColor":"#26FFFFFF","height":5}]}"""

    private fun bountyLine(n: Int, minHeight: Int) = """
        {"type":"column","show":{"minHeight":$minHeight,"ifSet":"{mq.bounty.$n}"},"children":[
         {"type":"spacer","size":6},
         {"type":"row","children":[
          {"type":"text","text":"{mq.bounty.$n}","size":12,"color":"#E6FFFFFF","grow":true,"maxLines":1},
          {"type":"text","text":"{mq.bounty.$n.progress}","size":11,"color":"#99FFFFFF","show":{"minWidth":250}}]},
         {"type":"spacer","size":3},
         {"type":"progress","value":"{mq.bounty.$n.pct}","color":"#FFFF9F43","trackColor":"#26FFFFFF","height":4}]}"""

    private fun coin(symbol: String, minHeight: Int) = """
        {"type":"row","show":{"minHeight":$minHeight},"children":[
         {"type":"text","text":"$symbol","size":14,"color":"#CCFFFFFF","weight":"bold","grow":true},
         {"type":"text","text":"{crypto.$symbol.price}","size":14,"color":"#FFFFFFFF","weight":"medium"},
         {"type":"text","text":"  {crypto.$symbol.change}","size":13,"color":"#FFFFFFFF",
          "trendOf":"{crypto.$symbol.change}","show":{"minWidth":250}}]}"""

    private val SOURCES = listOf(
        Triple(
            "Big clock", "The time, large. Adds the date, then a greeting and battery as it grows.",
            """{"name":"Big clock","background":"#E6141B2D","cornerRadius":28,"padding":16,"root":
            {"type":"column","align":"center","verticalAlign":"center","children":[
             {"type":"text","text":"{greeting}","size":14,"color":"#B3FFFFFF","weight":"medium","show":{"minHeight":200}},
             {"type":"clock","format":"h:mm","size":40,"color":"#FFFFFFFF","weight":"light","show":{"maxHeight":119}},
             {"type":"clock","format":"h:mm","size":44,"color":"#FFFFFFFF","weight":"light","show":{"minHeight":120,"maxWidth":249}},
             {"type":"clock","format":"h:mm","size":56,"color":"#FFFFFFFF","weight":"light","show":{"minHeight":120,"minWidth":250}},
             {"type":"clock","format":"EEE, MMM d","size":14,"color":"#CCFFFFFF","weight":"normal","show":{"minHeight":120,"maxWidth":249}},
             {"type":"clock","format":"EEEE, MMMM d","size":14,"color":"#CCFFFFFF","weight":"normal","show":{"minHeight":120,"minWidth":250}},
             {"type":"spacer","size":12,"show":{"minHeight":200}},
             {"type":"row","show":{"minHeight":200},"children":[
              {"type":"icon","name":"battery","size":14,"color":"#FF7CF29C"},
              {"type":"text","text":" {battery}%  {charging}","size":13,"color":"#B3FFFFFF"}]}]}}""",
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
             {"type":"row","show":{"minHeight":200},"children":[${hour(1)},${hour(2)},${hour(3)},${hour(4)}]}]}}""",
        ),
        Triple(
            "Weather forecast", "Now, the next hours, and the next 3 days on a big widget.",
            """{"name":"Weather forecast","background":"#E6123A5C","cornerRadius":26,"padding":16,"root":
            {"type":"column","children":[
             {"type":"row","children":[
              {"type":"icon","name":"weather","size":36,"color":"#FFFFD27A"},
              {"type":"column","grow":true,"padding":8,"children":[
               {"type":"text","text":"{weather.temp}","size":30,"color":"#FFFFFFFF","weight":"light","maxLines":1},
               {"type":"text","text":"{weather.condition}","size":12,"color":"#CCFFFFFF","maxLines":1,"show":{"minHeight":110}}]},
              {"type":"column","align":"end","show":{"minWidth":230},"children":[
               {"type":"text","text":"{weather.place}","size":13,"color":"#FFFFFFFF","weight":"medium","align":"end","maxLines":1},
               {"type":"text","text":"H {weather.high}  L {weather.low}","size":12,"color":"#B3FFFFFF","align":"end"},
               {"type":"text","text":"Rain {weather.rain}","size":12,"color":"#B3FFFFFF","align":"end"}]}]},
             {"type":"spacer","size":12,"show":{"minHeight":150}},
             {"type":"row","show":{"minHeight":150},"children":[${hour(1)},${hour(2)},${hour(3)},${hour(4)},${hour(5)}]},
             {"type":"spacer","size":12,"show":{"minHeight":260}},
             {"type":"column","show":{"minHeight":260},"children":[${day(1)},{"type":"spacer","size":4},${day(2)},{"type":"spacer","size":4},${day(3)}]}]}}""",
        ),
        Triple(
            "Crypto ticker", "Bitcoin's price and change; more coins as it grows taller. Set your coins in Settings.",
            """{"name":"Crypto ticker","background":"#E6111418","cornerRadius":22,"padding":14,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","children":[
              {"type":"icon","name":"coin","size":18,"color":"#FFF7B731"},
              {"type":"text","text":" Crypto","size":13,"color":"#B3FFFFFF","weight":"medium","grow":true},
              {"type":"text","text":"{updated}","size":11,"color":"#66FFFFFF","show":{"minWidth":250}}]},
             {"type":"spacer","size":6},
             {"type":"text","text":"{crypto.BTC.price}","size":26,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
             {"type":"text","text":"BTC {crypto.BTC.change} today","size":13,"color":"#FFFFFFFF","trendOf":"{crypto.BTC.change}"},
             {"type":"spacer","size":10,"show":{"minHeight":170}},
             ${coin("ETH", 170)},
             {"type":"spacer","size":6,"show":{"minHeight":210}},
             ${coin("SOL", 210)},
             {"type":"spacer","size":6,"show":{"minHeight":250}},
             ${coin("XRP", 250)}]}}""",
        ),
        Triple(
            "Commute", "How long the drive takes with traffic. Set up Traffic in Settings.",
            """{"name":"Commute","background":"#E61B2A22","cornerRadius":24,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","children":[
              {"type":"icon","name":"car","size":20,"color":"#FF7CF29C"},
              {"type":"text","text":"  To {traffic.to}","size":13,"color":"#B3FFFFFF","weight":"medium","grow":true}]},
             {"type":"text","text":"{traffic.commute}","size":32,"color":"#FFFFFFFF","weight":"bold","maxLines":1},
             {"type":"text","text":"{traffic.delay} · {traffic.distance}","size":13,"color":"#CCFFFFFF","show":{"minHeight":120}},
             {"type":"spacer","size":12,"show":{"minHeight":200}},
             {"type":"row","show":{"minHeight":200},"children":[
              {"type":"icon","name":"weather","size":16,"color":"#FFFFD27A"},
              {"type":"text","text":" {weather.temp} {weather.condition}","size":13,"color":"#B3FFFFFF","maxLines":1}]}]}}""",
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
              {"type":"text","text":"{calendar.today}","size":11,"color":"#99FFFFFF","show":{"minWidth":250}}]},
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
            {"type":"row","verticalAlign":"center","children":[
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
              {"type":"text","text":"{calendar.3.when}","size":12,"color":"#B3FFFFFF","maxLines":1,"show":{"minHeight":200}}]}]}}""",
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
            {"type":"row","children":[
             {"type":"column","grow":true,"children":[
              {"type":"row","show":{"minHeight":150},"children":[
               {"type":"spacer","grow":true},
               {"type":"icon","name":"weather","size":52,"color":"#FFC9DDE6"}]},
              {"type":"spacer","grow":true},
              {"type":"text","text":"{weather.temp}","size":30,"color":"#FFFFFFFF","weight":"medium","maxLines":1},
              {"type":"text","text":"{weather.outlook}","size":13,"color":"#E6FFFFFF","maxLines":1,"show":{"minHeight":100}},
              {"type":"row","show":{"minHeight":120},"children":[
               {"type":"icon","name":"pin","size":13,"color":"#B3FFFFFF"},
               {"type":"text","text":" {weather.place}","size":13,"color":"#B3FFFFFF","maxLines":1}]}]},
             {"type":"column","grow":true,"show":{"minWidth":250},"verticalAlign":"center","children":[
              ${hourRow(1, 0, clock24 = true)},${hourRow(2, 0, clock24 = true)},${hourRow(3, 140, clock24 = true)},${hourRow(4, 140, divider = false, clock24 = true)}]}]}}""",
        ),
        Triple(
            "Battery pill", "An oval battery meter, like One UI's: the percentage and a bar.",
            """{"name":"Battery pill","background":"#B3504A48","cornerRadius":40,"padding":12,"shape":"pill","root":
            {"type":"row","verticalAlign":"center","children":[
             {"type":"text","text":"{battery}%","size":18,"color":"#FFFFFFFF","weight":"bold"},
             {"type":"spacer","size":14},
             {"type":"progress","value":"{battery}","color":"#FFFFFFFF","trackColor":"#40FFFFFF","height":12,"grow":true},
             {"type":"text","text":"  {charging}","size":12,"color":"#CCFFFFFF","show":{"minWidth":260}}]}}""",
        ),
        Triple(
            "Agenda card", "Monster Quest's date card: what's next at the top, today's date big at the bottom.",
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
             {"type":"clock","format":"EEEE, MMM","size":14,"color":"#FFFFFFFF","weight":"bold","show":{"minHeight":140}}]}}""",
        ),
        Triple(
            "Steps pill", "Today's steps and a bar to your goal, as an oval like One UI's. Turn on Exercise & health in Settings.",
            """{"name":"Steps pill","background":"#E62A1B12","cornerRadius":40,"padding":12,"shape":"pill","root":
            {"type":"column","align":"center","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"bottom","children":[
              {"type":"text","text":"{health.steps}","size":22,"color":"#FFFFFFFF","weight":"bold"},
              {"type":"text","text":" Steps","size":13,"color":"#CCFFFFFF","weight":"medium"},
              {"type":"text","text":" of {health.stepGoal}","size":13,"color":"#99FFFFFF","show":{"minWidth":250}}]},
             {"type":"spacer","size":6},
             {"type":"progress","value":"{health.stepsPct}","color":"#FF4CD137","trackColor":"#40FFFFFF","height":10}]}}""",
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
               {"type":"progress","value":"{health.stepsPct}","color":"#FF4CD137","trackColor":"#26FFFFFF","height":4,"show":{"minHeight":100}}]},
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
              {"type":"text","text":"deep {health.sleep.deep}","size":12,"color":"#99FFFFFF","show":{"minWidth":250}}]}]}}""",
        ),
        Triple(
            "Monster Quest eggs", "Your eggs and how far to walk until they hatch; more eggs the taller it is.",
            """{"name":"Monster Quest eggs","background":"#E6241B33","cornerRadius":26,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"egg","size":18,"color":"#FFFFC857"},
              {"type":"text","text":"  {mq.eggs} eggs","size":15,"color":"#FFFFFFFF","weight":"bold","grow":true},
              {"type":"text","text":"{mq.eggs.ready} ready","size":12,"color":"#FFFFC857","show":{"minWidth":200}}]},
             ${egg(1, 100)},
             ${egg(2, 170)},
             ${egg(3, 230)},
             ${egg(4, 290)}]}}""",
        ),
        Triple(
            "Monster Quest adventures", "Monsters out on adventures: where, who, and how long until they're back.",
            """{"name":"Monster Quest adventures","background":"#E6122A24","cornerRadius":26,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"paw","size":18,"color":"#FF8FE3B0"},
              {"type":"text","text":"  {mq.adventures} away","size":15,"color":"#FFFFFFFF","weight":"bold","grow":true,"maxLines":1}]},
             ${adventure(1, 100)},
             ${adventure(2, 190)},
             ${adventure(3, 260)}]}}""",
        ),
        Triple(
            "Monster Quest day", "Your hero, today's steps and torches, and last night's sleep.",
            """{"name":"Monster Quest day","background":"#E61E1A2E","cornerRadius":26,"padding":16,"root":
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
               {"type":"text","text":"eggs","size":11,"color":"#99FFFFFF"}]}]}]}}""",
        ),
        Triple(
            "Monster Quest quests", "Your next quests with a check button: tap it to complete the quest in the game.",
            """{"name":"Monster Quest quests","background":"#E6182230","cornerRadius":26,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","show":{"minHeight":120},"children":[
              {"type":"text","text":"Quests","size":15,"color":"#FFFFFFFF","weight":"bold","grow":true},
              {"type":"text","text":"{mq.quests} open","size":12,"color":"#99FFFFFF"}]},
             ${quest(1, 0)},
             ${quest(2, 150)},
             ${quest(3, 200)},
             ${quest(4, 250)},
             ${quest(5, 300)}]}}""",
        ),
        Triple(
            "Monster Quest progress", "Steps to your next torch, the weekly bounty, and a phone-free session's time left.",
            """{"name":"Monster Quest progress","background":"#E6261A14","cornerRadius":26,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"flame","size":16,"color":"#FFFF9F43"},
              {"type":"text","text":"  {mq.torches} torches","size":14,"color":"#FFFFFFFF","weight":"bold","grow":true},
              {"type":"text","text":"{mq.torch.next}","size":11,"color":"#99FFFFFF","show":{"minWidth":250}}]},
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
              {"type":"text","text":"{mq.bounty.ready}","size":11,"color":"#FFFFC857","show":{"ifSet":"{mq.bounty.ready}"}}]},
             ${bountyLine(1, 150)},
             ${bountyLine(2, 190)},
             ${bountyLine(3, 230)}]}}""",
        ),
        Triple(
            "Monster Quest party", "Your party's monsters with their level and health.",
            """{"name":"Monster Quest party","background":"#E6142618","cornerRadius":26,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"icon","name":"paw","size":16,"color":"#FF4CD137"},
              {"type":"text","text":"  Party","size":14,"color":"#FFFFFFFF","weight":"bold"}]},
             ${member(1, 0)},
             ${member(2, 140)},
             ${member(3, 190)},
             ${member(4, 240)},
             ${member(5, 290)}]}}""",
        ),
        Triple(
            "Battery", "Your battery level with a bar; the time and date when it's bigger.",
            """{"name":"Battery","background":"#E6162016","cornerRadius":24,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","children":[
              {"type":"icon","name":"bolt","size":22,"color":"#FF7CF29C"},
              {"type":"text","text":" {battery}%","size":30,"color":"#FFFFFFFF","weight":"bold","grow":true},
              {"type":"text","text":"{charging}","size":12,"color":"#FF7CF29C","show":{"minWidth":220}}]},
             {"type":"spacer","size":10},
             {"type":"progress","value":"{battery}","color":"#FF7CF29C","trackColor":"#26FFFFFF","height":8},
             {"type":"spacer","size":14,"show":{"minHeight":180}},
             {"type":"row","show":{"minHeight":180},"children":[
              {"type":"clock","format":"h:mm","size":22,"color":"#FFFFFFFF","weight":"light","grow":true},
              {"type":"clock","format":"EEE d MMM","size":13,"color":"#B3FFFFFF"}]}]}}""",
        ),
        Triple(
            "NOOP recovery", "Charge (estimated from HRV and resting heart rate) and effort from NOOP; HRV, sleep, SpO2 and more as it grows. Turn on Health Connect in NOOP.",
            """{"name":"NOOP recovery","background":"#F20B0F14","cornerRadius":28,"padding":16,"root":
            {"type":"column","verticalAlign":"center","children":[
             {"type":"row","verticalAlign":"center","children":[
              {"type":"column","grow":true,"children":[
               {"type":"text","text":"Charge","size":12,"color":"#99FFFFFF","show":{"minHeight":100}},
               {"type":"row","verticalAlign":"bottom","children":[
                {"type":"text","text":"{health.charge}%","size":28,"color":"#FF2BE38A","weight":"bold","maxLines":1},
                {"type":"text","text":" {health.charge.label}","size":13,"color":"#CCFFFFFF","maxLines":1}]}]},
              {"type":"column","align":"end","show":{"minWidth":250},"children":[
               {"type":"text","text":"Effort","size":12,"color":"#99FFFFFF","show":{"minHeight":100}},
               {"type":"text","text":"{health.effort}","size":24,"color":"#FF4FA3FF","weight":"bold"}]}]},
             {"type":"spacer","size":6,"show":{"minHeight":100}},
             {"type":"progress","value":"{health.charge}","color":"#FF2BE38A","trackColor":"#26FFFFFF","height":6,"show":{"minHeight":100}},
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
               {"type":"text","text":"VO2 max","size":11,"color":"#99FFFFFF","maxLines":1}]}]}]}}""",
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
               {"type":"row","verticalAlign":"center","children":[
                {"type":"icon","name":"previous","size":24,"color":"#E6FFFFFF","tap":"music:prev"},
                {"type":"spacer","size":16},
                {"type":"icon","name":"play","live":"{music.playIcon}","size":30,"color":"#FFFFFFFF","tap":"music:toggle"},
                {"type":"spacer","size":16},
                {"type":"icon","name":"next","size":24,"color":"#E6FFFFFF","tap":"music:next"},
                {"type":"spacer","grow":true},
                {"type":"text","text":"{music.app}","size":11,"color":"#80FFFFFF","maxLines":1,"show":{"minWidth":300}}]}]}]}]}}""",
        ),
        Triple(
            "Music pill", "What's playing in an oval, with play/pause (and next when wider).",
            """{"name":"Music pill","background":"#E6202124","cornerRadius":40,"padding":10,"shape":"pill","root":
            {"type":"row","verticalAlign":"center","children":[
             {"type":"art","size":36,"cornerRadius":18,"show":{"minWidth":200}},
             {"type":"spacer","size":10,"show":{"minWidth":200}},
             {"type":"text","text":"{music.line}","size":13,"color":"#FFFFFFFF","weight":"medium","maxLines":1,"grow":true},
             {"type":"icon","name":"play","live":"{music.playIcon}","size":26,"color":"#FFFFFFFF","tap":"music:toggle"},
             {"type":"spacer","size":10,"show":{"minWidth":240}},
             {"type":"icon","name":"next","size":22,"color":"#CCFFFFFF","tap":"music:next","show":{"minWidth":240}}]}}""",
        ),
    )
}
