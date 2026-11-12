# AI Widget Maker

Describe a widget in plain words ("a dark glassy clock with the date and my
battery") and Claude designs it. Preview it at different sizes, ask for changes
("make the clock bigger, purple background"), save it, and add it to your home
screen with one tap.

## Install

Every push to `main` builds the app and publishes it as the
[`latest` release](../../releases/tag/latest).
Download `AIWidgetMaker.apk` on your phone and open it.

## Three ways to make a widget, two of them free

- **Free, with the Claude app:** describe a widget and tap **Design in Claude
  (free)**. The app opens the Claude app (or claude.ai) with your request; a
  free account works. Send it, copy Claude's reply, come back and tap **Paste
  design**. Changes ("make it purple") work the same way.
- **Free, no AI:** start from one of the ready-made designs: big clock, clock
  and weather, forecast, crypto ticker, commute, local news, nearby events,
  up next (calendar), big date, day cookie, hourly weather, battery pill, agenda card, steps pill, activity, Monster Quest eggs, adventures, day, quests, progress and party, battery. Save it as it is, or change it with Claude.
- **Automatic, paid per use:** add an Anthropic API key (from
  console.anthropic.com) in **Settings** and designs are made in the app, a few
  cents each. The key is stored only on the phone.

## Resize for more

Widgets are responsive: stretch one wider or taller and it shows more. A small
clock gains the battery when it's wider, then the weather, then an hourly
forecast as it grows taller; a news widget shows more headlines the taller it
is. Claude designs every widget this way (each element can say which sizes it
appears at), and the app previews each design at 2×1, 2×2, 2×4, 4×2 and 4×4.

## Monster Quest, both ways

Monster Quest widgets don't just show the game, they can act in it:

- a **✓** next to a quest completes it (and pays its reward),
- **Claim** appears when an adventure is back, **Hatch** when an egg is ready.

The game checks each tap against its rules, applies it right away if it's
open (or the next time it starts), and the widget shows it as done at once.
Tapping an egg, adventure or quest opens that screen in the game. Only this
app can read the game's summary or ask it to act.

## Shapes and look

Widgets come in six shapes: rounded, pill, circle, squircle, arch (round top)
and Material 3 Expressive's scalloped cookie. A design picks one, and each
widget can pick its own in its settings.

**Settings → Widget look** sets, for all widgets: corner radius, edge
thickness, size (how much room around the panel), and **oval 1-row widgets**
with a max height, like One UI's "Adjust height for 2x1 & 4x1".

## Tap to open

Tapping a widget opens the right thing:

- A **headline** opens that article, an **event** its page, a **calendar entry**
  that event in your calendar.
- The **time** opens your Clock app and a **date** your calendar.
- The rest of the widget opens an app for what it shows: weather, crypto, news,
  events, commute (maps), battery.

Clock, calendar, maps and battery use the phone's defaults. For weather, crypto
and news, Android has no default, so the first tap asks which app (fitting apps
first, or the web) with **Always use this app** ticked. Change any of them in
**Settings → When you tap a widget**.

## Widget style

In **Settings → Widget style**, pick how all your widgets are colored. The
layout always stays as designed:

- **Glass** (default): smoky see-through glass with a bright edge and white
  text. Icons and bars keep their colors.
- **Material 3**: your phone's colors (wallpaper or a Samsung One UI color
  palette, Android 12+), switching between light and dark with your phone. The
  launcher looks the colors up itself each time it draws the widget, so they
  match the phone's current palette; Settings shows them as swatches.
- **Original colors**: the palette Claude chose for each design.
- **One UI**: like Samsung One UI 9. The part of your wallpaper behind each
  widget, blurred and frosted, with text that turns dark or light to suit it
  and accents in your color palette. Launchers don't let widgets blur, so the
  app does it: pick your wallpaper picture once in Settings (or allow
  wallpaper access), then tap an empty spot on each widget once so it learns
  where it sits and the blur lines up.

Each widget can also have its own style: long-press it, tap its settings, and
pick one (or keep "App default"). The same screen has **Refresh now**, which
fetches fresh data right away and redraws your widgets.

## Live data

Widgets can show live data, refreshed about every 30 minutes in the
background. Turn each source on in **Settings**, then tap **Save and test** to
check it works:

| Data | From | Setup |
|---|---|---|
| Weather (now, the next 5 hours, the next 3 days, with live weather icons) | Open-Meteo | Type your town. No account. |
| Crypto prices and 24 h change, green or red | CoinGecko | List your coins, like `BTC, ETH`. No account. |
| Local news (up to 5 headlines) | Google News | Your town, or a topic. No account. |
| Commute time with live traffic | TomTom | Free key from [developer.tomtom.com](https://developer.tomtom.com), plus home and work addresses. |
| Nearby events (up to 5) | Local news (Google News), or Ticketmaster | Your town. No account: event-like headlines first, crime and accident news left out, and dates read from headlines ("this weekend", "Sat", "Nov 14"). For exact listings, optionally add a free Ticketmaster key. |
| Your calendar (next 7 days) | The calendars on your phone | Turn on **Your calendar** and allow access. Read on the phone only. |
| Exercise & health: steps, heart rate, kcal, workouts, sleep, effort | Health Connect (Samsung Health, WHOOP, Google Fit, Fitbit, Garmin...) | Turn on **Exercise & health** and allow access. In Samsung Health: Settings → Health Connect. "Effort" is estimated from heart rate (WHOOP keeps its strain score private). |
| Monster Quest: quests, eggs, adventures, party, bounty, torches, phone-free time, steps, sleep | The game itself | Install Monster Quest (3.24 or newer); widgets update whenever the game changes. |

Then just ask for it, for example "my commute time and today's weather" or
"a crypto ticker for my coins".

## Where widgets can go

- **Home screen:** any Android 8+ launcher, including Samsung's. Use "Add to
  home screen" in the app, or long-press the home screen → Widgets → AI Widget
  Maker (you'll be asked which design to show).
- **Lock screen:** the widget is declared for the lock screen too
  (`keyguard`). It shows up wherever the phone's lock screen accepts
  third-party widgets. On Samsung: long-press the lock screen → Widgets. Some
  One UI versions only list Samsung's own widgets there; it then works on the
  home screen only.

## How it works

Android widgets can't run downloaded code, so Claude doesn't write code. It
writes a small JSON design (rows, columns, text, a live clock, icons, progress
bars, live values like `{battery}` and `{greeting}`), and the app draws it with
Jetpack Glance.

Code is in `app/src/main/java/com/sidequest/widgetmaker/`:

- `spec/`: the design format (`WidgetSpec.kt`), the parser that checks and
  clamps what Claude sends (`DesignParser.kt`), and the live values (`Tokens.kt`).
- `ai/`: the prompt that teaches Claude the format, and the API call.
- `feeds/`: the live data sources: fetching (`Feeds.kt`), reading each
  service's response (`FeedParsers.kt`) and the background refresh (`FeedWorker.kt`).
- `widget/`: the Glance widget. Clocks use the system's `TextClock`, so they
  tick every minute without waking the app.
- `ui/`: the app screens and an in-app preview that follows the same layout rules.

Tests: `./gradlew :app:testDebugUnitTest`.

## Before sharing it with other people

The API key lives in the app, which is fine for your own phone. To publish
the app, send the request through a small server of your own that holds the key.

## Ideas for later

- Calendar events and now-playing music as live values.
- Images and gradients (generated as bitmaps).
- Tap actions: open an app, start a timer.
