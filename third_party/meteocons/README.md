# Meteocons

Weather icons by Bas Milius: https://github.com/basmilius/meteocons (npm `@meteocons/svg` 0.1.0, "fill" style),
used under the MIT License (see `LICENSE` here, also shipped in the app as `res/raw/meteocons_license.txt`
and shown in Settings).

The icons are not modified in design. The animated SVGs were rendered to frames, 128 px at 10 frames a
second over one full loop (from 6 s in, after every staggered start), and saved as WebP in
`app/src/main/res/drawable-nodpi/mc_<icon>_<frame>.webp`, so that home-screen widgets (which can't play SVG
or Lottie) can step through them with a ViewFlipper.

Icons used: clear-day, clear-night, partly-cloudy-day, partly-cloudy-night, overcast-day, overcast-night,
fog-day, fog-night, drizzle, sleet, rain, snow, partly-cloudy-day-rain, partly-cloudy-night-rain,
partly-cloudy-day-snow, partly-cloudy-night-snow, thunderstorms-day-rain, thunderstorms-night-rain,
thunderstorms-hail.
