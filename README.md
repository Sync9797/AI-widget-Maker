# AI Widget Maker

Describe a widget in plain words ("a dark glassy clock with the date and my
battery") and Claude designs it. Preview it at different sizes, ask for changes
("make the clock bigger, purple background"), save it, and add it to your home
screen with one tap.

## Install

Every push to `main` builds the app and publishes it as the
[`latest` release](../../releases/tag/latest).
Download `AIWidgetMaker.apk` on your phone and open it.

The first time, open **Settings** in the app and paste an Anthropic API key
(from console.anthropic.com). It's stored only on the phone.

## Widget style

In **Settings → Widget style**, pick how all your widgets are colored. The
layout always stays as designed:

- **Glass** (default): smoky see-through glass with a bright edge and white
  text. Icons and bars keep their colors.
- **Material 3**: colors from your wallpaper (Android 12+), switching between
  light and dark with your phone.
- **Original colors**: the palette Claude chose for each design.

## Live data

Widgets can show live data, refreshed about every 30 minutes in the
background. Turn each source on in **Settings**, then tap **Save and test** to
check it works:

| Data | From | Setup |
|---|---|---|
| Weather (temperature, high/low, rain chance, wind, a live weather icon) | Open-Meteo | Type your town. No account. |
| Crypto prices and 24 h change, green or red | CoinGecko | List your coins, like `BTC, ETH`. No account. |
| Local news headlines | Google News | Your town, or a topic. No account. |
| Commute time with live traffic | TomTom | Free key from [developer.tomtom.com](https://developer.tomtom.com), plus home and work addresses. |
| Nearby events (concerts, games, shows) | Ticketmaster | Free key ("Consumer Key") from [developer.ticketmaster.com](https://developer.ticketmaster.com). |

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
