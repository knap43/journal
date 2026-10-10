# Journal

A quiet, private journal for Android — dated entries, photos, search, and a full export.

Kotlin · Jetpack Compose · Material 3 · Room · Coil

## Features

- **Dated entries** on a timeline grouped by month; any entry can be moved to another day.
- **Photos** chosen with the system photo picker (no storage permission) and copied into
  private app storage, so deleting them from the gallery never breaks an entry.
- **Autosave** — there is no save button. Blank pages are discarded on the way out.
- **Search** across titles and bodies.
- **Export** to a single zip:
  - `journal.json` — every entry, machine-readable
  - `entries/YYYY-MM-DD.md` — Markdown with YAML front matter
  - `attachments/` — the original images, linked from both
- Kept out of cloud backup and device-to-device transfer.

## Build

Requires JDK 17 and the Android SDK (platform 36).

```fish
sudo pacman -S jdk17-openjdk
set -Ux ANDROID_HOME ~/Android/Sdk   # or wherever your SDK lives
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release builds (`./gradlew assembleRelease`) are minified and, for now, signed with the
debug key.

## Layout

```
app/src/main/java/com/knap43/journal/
├── data/        Room entities and DAO, repository (attachment files), exporter
├── ui/timeline  month-grouped list, search, export
├── ui/editor    the page itself: date, title, photos, body
└── ui/theme     dark palette, #e1a34f accent, serif for prose
```
