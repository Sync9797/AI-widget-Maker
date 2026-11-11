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

    private fun hour(n: Int) = """
        {"type":"column","align":"center","grow":true,"children":[
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
        {"type":"column","show":{"minHeight":$minHeight},"children":[
         {"type":"spacer","size":8},
         {"type":"text","text":"{news.$n}","size":13,"color":"#FFFFFFFF","weight":"medium","maxLines":2},
         {"type":"text","text":"{news.$n.source}","size":11,"color":"#80FFFFFF","maxLines":1}]}"""

    private fun event(n: Int, minHeight: Int) = """
        {"type":"column","show":{"minHeight":$minHeight},"children":[
         {"type":"spacer","size":8},
         {"type":"text","text":"{events.$n}","size":13,"color":"#FFFFFFFF","weight":"medium","maxLines":1},
         {"type":"text","text":"{events.$n.when} · {events.$n.venue}","size":11,"color":"#99FFFFFF","maxLines":1}]}"""

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
             {"type":"clock","format":"h:mm","size":56,"color":"#FFFFFFFF","weight":"light"},
             {"type":"clock","format":"EEEE, MMMM d","size":14,"color":"#CCFFFFFF","weight":"normal","show":{"minHeight":120}},
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
            {"type":"column","children":[
             {"type":"row","children":[
              {"type":"icon","name":"ticket","size":16,"color":"#FFFF7AC6"},
              {"type":"text","text":"  What's on","size":13,"color":"#FFFF7AC6","weight":"bold","grow":true}]},
             {"type":"spacer","size":6},
             {"type":"text","text":"{events.1}","size":16,"color":"#FFFFFFFF","weight":"bold","maxLines":2},
             {"type":"text","text":"{events.1.when} · {events.1.venue}","size":12,"color":"#B3FFFFFF","maxLines":1},
             ${event(2, 180)},
             ${event(3, 240)},
             ${event(4, 300)}]}}""",
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
    )
}
