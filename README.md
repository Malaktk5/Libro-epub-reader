# Libro – offline EPUB shelf & reader (Android)

Kotlin + Jetpack Compose. No permissions, no internet, no analytics.

## Build
1. Open this folder in **Android Studio** (Ladybug or newer) and let Gradle sync.
2. Run on a phone/emulator (Android 8.0+), or `Build > Build APK(s)`.
   Command line: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/`.

## What it does
- First launch: choose **language** (English / Français / العربية / system) and **theme** (system / light / dark / sepia).
- **Add books** picks EPUB files with the system file picker. Each is copied into app storage, so it still works if the original is moved.
- Shelf shows **covers** (placeholder when a book has none), a **progress bar and %**, search, sort; long-press a book to mark finished/unread or remove.
- Reader: tap the middle to show controls, left/right edges (or swipe) to turn pages.
  Customise **page theme** (6), **pages or scrolling**, **font** (4 bundled + system serif/mono + the book's own), **size**, **line spacing**, **margins**, **alignment**, hyphenation, brightness, keep-screen-on.
  Table of contents, progress slider, position remembered per book. RTL books (Arabic/Hebrew) page right-to-left.

## Layout
- `epub/EpubBook.kt` – zip/OPF/NAV/NCX parsing, cover detection, progress math
- `data/` – settings (`Prefs`), shelf storage (`LibraryRepository`)
- `ui/reader/ReaderController.kt` – WebView served from the zip on `https://epub.local` (network blocked)
- `assets/reader.js` – pagination (CSS columns), scrolling, theming, taps/swipes
- Fonts in `assets/fonts` (SIL OFL, see `LICENSES.txt`).

## License
MIT, see `LICENSE`. Third-party notices are in `THIRD_PARTY_NOTICES.md`.
