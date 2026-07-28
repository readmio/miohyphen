# MioHyphen

Offline, **dependency-free** word hyphenation for Android (and any JVM), using the
LibreOffice / hunspell **`libhyphen`** TeX patterns — the same pattern data behind
[ushuaia.pl](https://www.ushuaia.pl/hyphen/), LibreOffice and OpenOffice.

- 🔌 **No runtime dependencies, no NDK, no native code** — a single self-contained Kotlin engine.
- 📴 **Fully offline & on-device** — no network, patterns bundled as assets.
- 🌍 **12 languages bundled**, incl. German **compound** splitting and **Cyrillic**.
- ✅ Output verified 100% against the `pyphen` reference (see [Verified quality](#verified-quality)).

```kotlin
val mioHyphen = MioHyphen(context)
mioHyphen.hyphenate("bratislava", "sk_SK")            // "bra­ti­sla­va"  (U+00AD soft hyphens)
mioHyphen.hyphenate("Donaudampfschifffahrt", "de_DE") // "Do­nau­dampf­schiff­fahrt"
```

## Install (JitPack)

**1.** Add the JitPack repository. In `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**2.** Add the dependency (use a released tag, e.g. `0.1.0`, a commit hash, or `main-SNAPSHOT`):

```kotlin
dependencies {
    implementation("com.github.readmio:miohyphen:0.1.0")
}
```

To publish a version: push the repo, create a git tag (`git tag 0.1.0 && git push --tags`), then the
coordinate `com.github.readmio:miohyphen:0.1.0` builds on first request at
`https://jitpack.io/#readmio/miohyphen`.

## Usage

```kotlin
val mioHyphen = MioHyphen(context)                     // keep as a singleton

mioHyphen.hyphenate("bratislava", "sk_SK")             // "bra­ti­sla­va"  (soft hyphens U+00AD)
mioHyphen.positions("bratislava", "sk_SK")             // [3, 5, 8]
mioHyphen.hyphenateText("Ako sa máš dnes?", "sk_SK")   // hyphenates each word, keeps spaces/punctuation
mioHyphen.availableLanguages()                         // ["cs_CZ", "de_DE", "en_US", … , "uk_UA"]

// One-letter words are kept with the next word, and a paragraph-end runt is avoided — both on by
// default. Tune them via HyphenationOptions (see Configuration below).
mioHyphen.hyphenateText("i k nemu", "sk_SK")   // binds one-letter "i"/"k" to the next word (defaults)

// custom separator instead of the soft hyphen:
mioHyphen.forLanguage("de_DE").hyphenate("Fußballweltmeisterschaft", "·")
// "Fuß·ball·welt·meis·ter·schaft"
```

Feed the soft-hyphenated string to a `TextView`/Compose `Text` — the renderer breaks lines at the
soft hyphens when needed and shows nothing otherwise.

### Hyphenating HTML

For HTML content, use `hyphenateHtml` instead of walking the DOM and calling `hyphenateText` per
text node. It transforms only text (tags, attributes and custom elements like `<sound>` are kept
verbatim; `<script>`/`<style>` are skipped) and — crucially — it binds a one-letter word to the
next word **even across inline markup**, which a per-node call cannot see:

```kotlin
mioHyphen.hyphenateHtml("Oľko a<strong>zatlieskal</strong>", "sk_SK")
// "Oľ­ko a <strong>za­tlies­kal</strong>"   — the "a" stays with the next word
```

Binding never crosses block boundaries (`</p>`, `<br>`, list items); a one-letter word that ends a
block is left alone. The call is idempotent and returns the input unchanged on malformed markup.

## Configuration (`HyphenationOptions`)

Policy — one-letter binding, break separator and runt prevention — lives in `HyphenationOptions`,
built with a fluent `Builder` and usually set once on the instance (per-call override optional):

```kotlin
val mioHyphen = MioHyphen(
    context,
    HyphenationOptions.Builder()
        .binding(SingleLetterBinding.SPACE_WORD_JOINER)   // default
        .avoidHyphenatingLastWord(true)                   // default
        .minimumLastLineLetters(4)                        // default
        .build(),
)

// override for a single call:
mioHyphen.hyphenateHtml(storyHtml, "sk_SK",
    HyphenationOptions.Builder().minimumLastLineLetters(6).build())
```

**One-letter binding** (`binding`) — how a one-letter word is kept with the next word:

| value | inserted after a one-letter word | notes |
|---|---|---|
| `SPACE_WORD_JOINER` | space `U+0020` + word joiner `U+2060` | **default** — non-breaking **and** stretches under justification |
| `NO_BREAK_SPACE` | `U+00A0` | non-breaking, fixed width (won't stretch when justified) |
| `NONE` | *(nothing — the plain space is kept)* | binding off |

**Runt prevention** (`avoidHyphenatingLastWord` default on, `minimumLastLineLetters` default 4) — a
*runt* is a tiny fragment stranded on a paragraph's last line. When on, the paragraph's last word
drops trailing hyphens while the fragment after them would be shorter than `minimumLastLineLetters`
letters (so `matematika` stays whole rather than breaking to a 2-letter last line). In HTML it
applies per paragraph (the last word before each block boundary); only the last word is affected.

## Supported languages

`sk_SK, cs_CZ, pl_PL, en_US, en_GB, de_DE, es, ru_RU, uk_UA, pt_PT, pt_BR, it_IT`

Add more by dropping `hyph_<lang>.dic` (from the
[LibreOffice dictionaries repo](https://git.libreoffice.org/dictionaries)) into
`src/main/assets/hyphenation/` — no code change. Remove one to drop its size and licensing obligations.

## Verified quality

Checked against golden vectors from the `pyphen` reference (`./gradlew test`):

| language | match | note |
|---|---|---|
| Slovak `sk_SK` | **100%** (40/40) | single-level |
| Czech `cs_CZ` | **100%** (28/28) | single-level |
| Polish `pl_PL` | **100%** (16/16) | single-level |
| English `en_US` | **100%** (20/20) | single-level |
| German `de_DE` | **100%** (26/26) | **two-level / compound** |
| Spanish `es` | **100%** (16/16) | single-level |
| Russian `ru_RU` | **100%** (12/12) | Cyrillic |
| Ukrainian `uk_UA` | **100%** (12/12) | Cyrillic |
| Portuguese `pt_PT` / `pt_BR` | **100%** | single-level |
| Italian `it_IT` | **100%** (14/14) | single-level |

German compounds split correctly (`Donaudampfschifffahrt → Do·nau·dampf·schiff·fahrt`); Cyrillic
works (`компьютер → ком·пью·тер`). The LibreOffice **Ukrainian** patterns are intentionally granular
(`університет → уні·ве·р·си·тет`) — every marked point is a valid break.

## How it works

- Patterns bundled as assets (`hyph_<lang>.dic`).
- `HyphenationDictionary` parses the `.dic` (charset, `LEFTHYPHENMIN`/`RIGHTHYPHENMIN`, patterns);
  `HyphenationEngine` runs Liang's algorithm; `MioHyphen` is the Android façade (loads from assets,
  caches one engine per language). The engine has **no Android imports** and is unit-tested on a plain JVM.
- The algorithm is a faithful port of `pyphen`. **Two-level (compound) dictionaries** — e.g. German
  (`NEXTLEVEL`) — are handled by merging both pattern levels (later patterns win), which is what makes
  German compounds split.

### Known limitation

Non-standard (Németh) patterns — a few cases where a letter changes at the break (e.g. old German
`bak-ken`) — are skipped; they need the literal C `libhyphen`. There are **zero** such patterns in
the bundled languages.

## Building

```bash
./gradlew test               # JVM unit tests
./gradlew assembleRelease    # build the .aar (build/outputs/aar/MioHyphen-release.aar)
```

Toolchain (aligned with the Readmio app): **AGP 9.3.1, Gradle 9.5.1, Kotlin 2.4.0, JDK 17+**.
CI builds on every push (`.github/workflows/ci.yml`).

## API documentation

The full public API (`MioHyphen`, `HyphenationEngine`, `HyphenationDictionary`) is documented with
KDoc — parameters, return values, thrown exceptions and examples. Browse it in your IDE (Dokka HTML
generation can be re-added with a Kotlin 2.4-compatible Dokka version).

## Next steps

### Kotlin Multiplatform (iOS)

The engine is already **KMP-ready**: `HyphenationEngine` and `HyphenationDictionary` use only the
common Kotlin stdlib (no `java.*`, no Android) and have been verified to compile for Kotlin/Native
`iosArm64` + `iosSimulatorArm64`. Only the thin `MioHyphen` façade is Android-specific.

To turn this into a shared Android + iOS library:

1. **Gradle → multiplatform.** Switch the module to `kotlin("multiplatform")` with `androidTarget()`
   and the iOS targets (`iosArm64`, `iosSimulatorArm64`, `iosX64`) alongside `com.android.library`.
2. **Engine → `commonMain`.** Move `HyphenationEngine` + `HyphenationDictionary` as-is — no code
   changes needed (they are already common-safe).
3. **Façade per platform.** Keep the current `MioHyphen` (Context/AssetManager) in `androidMain`;
   add an `iosMain` variant that loads `hyph_<lang>.dic` from the app bundle (`NSBundle`). A shared
   `expect`/`actual` resource loader keeps the public API identical on both platforms.
4. **Bundle the dictionaries** as multiplatform resources (e.g. Compose Multiplatform resources or
   `moko-resources`) so they land in the iOS app bundle.
5. **Distribution.** For a **KMP / Compose Multiplatform** app: just add a `commonMain` dependency.
   For a **pure Swift/SwiftUI** app: export an XCFramework and publish via **Swift Package Manager**
   or **CocoaPods**.

The heavy lifting (the platform-agnostic engine) is done; what remains is the façade + packaging.

### Attribution in the consuming app

An app that ships MioHyphen also bundles the LibreOffice/hunspell pattern dictionaries, so **the app
redistributes those files and must carry their notices**. Put them on your in-app **"Open source
licenses"** page (e.g. the one opened from the About menu) — that is the standard, accepted place.

Surface **both** components directly on that page — don't rely only on an *app → library repo →
dictionaries* link chain (fragile if the repo moves or goes private). List MioHyphen itself, and the
bundled dictionaries with their license, a source link, and where the per-language credits live.
Ready-to-paste block:

```html
<h3>MioHyphen</h3>
<p>Copyright © Readmio. Licensed under the Apache License 2.0.
   <a href="https://github.com/readmio/miohyphen">github.com/readmio/miohyphen</a></p>

<h3>Hyphenation dictionaries (LibreOffice / hunspell patterns)</h3>
<p>This app bundles TeX/Liang hyphenation pattern files (<code>hyph_*.dic</code>) from the
   LibreOffice dictionaries project, used under the
   <a href="https://www.mozilla.org/MPL/1.1/">Mozilla Public License 1.1</a>
   (tri-licensed GPL 2.0 / LGPL 2.1 / MPL 1.1). Source and per-language credits:
   <a href="https://git.libreoffice.org/dictionaries">git.libreoffice.org/dictionaries</a>.
   Per-language attribution is in the <code>README_hyph_*.txt</code> files shipped with the patterns.</p>
```

Keep the bundled `README_hyph_*.txt` files (each language's attribution) in the AAR/APK — do not
strip them. Choose **LGPL-2.1 or MPL-1.1** from the dictionaries' tri-license (not GPL) so your app
stays closed-source; see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md). Not legal advice — for a
commercial release have the per-language terms reviewed.

## License

Code: **Apache-2.0** (`LICENSE`).

The bundled hyphenation dictionaries are third-party (LibreOffice, tri-licensed GPL/LGPL/MPL, with
per-language terms). **Read [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) before redistributing.**
