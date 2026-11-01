# Libro – offline EPUB shelf & reader (Android)

Kotlin + Jetpack Compose. No permissions, no internet, no analytics.

## What it does
- First launch: choose **language** (English / Français / العربية / system) and **theme** (system / light / dark / sepia).
- **Add books** picks EPUB files with the system file picker. Each is copied into app storage, so it still works if the original is moved.
- Shelf shows **covers** (placeholder when a book has none), a **progress bar and %**, search, sort; long-press a book to mark finished/unread or remove.
- Reader: tap the middle to show controls, left/right edges (or swipe) to turn pages.
  Customise **page theme** (6), **pages or scrolling**, **font** (4 bundled + system serif/mono + the book's own), **size**, **line spacing**, **margins**, **alignment**, hyphenation, brightness, keep-screen-on.
  Reading settings: tap the middle of the page, then the "Aa" icon in the top bar. Table of contents, progress slider, position remembered per book. RTL books (Arabic/Hebrew) page right-to-left.

## Layout
- `epub/EpubBook.kt` – zip/OPF/NAV/NCX parsing, cover detection, progress math
- `data/` – settings (`Prefs`), shelf storage (`LibraryRepository`)
- `ui/reader/ReaderController.kt` – WebView served from the zip on `https://epub.local` (network blocked)
- `assets/reader.js` – pagination (CSS columns), scrolling, theming, taps/swipes
- Fonts in `assets/fonts` (SIL OFL, see `LICENSES.txt`).

## Themes
App themes: system, light, dark, sepia, amoled, midnight, nord, gruvbox, forest, solarized, rose, lavender.
Page themes (reader): same as app, light, sepia, gray, dark, black, solar light/dark, nord, gruvbox, midnight, forest, rose, lavender.

## Custom CSS
Settings > Custom CSS (or Reading settings > Custom CSS inside a book). The text is injected last into every chapter,
so it can override anything (use `body` + `!important` to win over the reader's own rules).
CSS cannot style native Android widgets, so colour variables are also read to recolour the app itself:

    :root { --app-bg:#111; --app-fg:#eee; --app-surface:#1c1c1c; --app-outline:#555;
            --app-primary:#e07a5f; --app-on-primary:#000; --app-secondary:#81b29a;
            --page-bg:#1c1b22; --page-fg:#e6e1d6; --page-link:#f2cc8f; --page-selection:#f2cc8f55; }

Inside the book you can also use the read-only `--libro-bg`, `--libro-fg`, `--libro-link`, `--libro-size`, `--libro-margin`.
Colours: #RGB, #RGBA, #RRGGBB, #RRGGBBAA, rgb(), rgba(). "Clear" removes the stylesheet if something breaks the layout.
