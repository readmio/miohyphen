package com.readmio.miohyphen

import android.content.Context
import android.content.res.AssetManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Offline word hyphenation for Android.
 *
 * `MioHyphen` is the entry point of the library. It loads `hyph_<lang>.dic` hyphenation
 * dictionaries bundled in the app's `assets/<assetDir>/` folder on first use, caches one
 * [HyphenationEngine] per language, and computes soft-hyphen break points entirely on-device —
 * no network, no native code, no runtime dependencies.
 *
 * The dictionaries are the LibreOffice / hunspell `libhyphen` TeX patterns; hyphenation quality
 * matches the `pyphen` reference. Language codes are the bundled file names without the
 * `hyph_`/`.dic` affixes, e.g. `"sk_SK"`, `"de_DE"`, `"en_US"` (see [availableLanguages]).
 *
 * Typical usage — create once (it is cheap and thread-safe) and keep it for the app lifetime:
 *
 * ```
 * val mioHyphen = MioHyphen(context)
 *
 * mioHyphen.hyphenate("bratislava", "sk_SK")            // "bra­ti­sla­va"
 * mioHyphen.positions("bratislava", "sk_SK")            // [3, 5, 8]
 * mioHyphen.hyphenateText("Ako sa máš?", "sk_SK")       // whole sentence, punctuation preserved
 * textView.text = mioHyphen.hyphenateText(article, "de_DE")
 * ```
 *
 * The hyphenation policy (binding style, separator, runt prevention) is set once via
 * [HyphenationOptions] — build it with [HyphenationOptions.Builder] and pass it here; individual
 * calls may still override it. All methods are thread-safe.
 *
 * ```
 * val mioHyphen = MioHyphen(
 *     context,
 *     HyphenationOptions.Builder().minimumLastLineLetters(5).build(),
 * )
 * ```
 *
 * @constructor Creates an instance reading dictionaries through [assets].
 * @property assets the [AssetManager] the dictionaries are read from.
 * @property assetDir folder inside `assets/` holding the `hyph_*.dic` files (default `"hyphenation"`).
 * @property options the default hyphenation policy for this instance (default [HyphenationOptions.DEFAULT]).
 */
class MioHyphen @JvmOverloads constructor(
    private val assets: AssetManager,
    private val assetDir: String = "hyphenation",
    private val options: HyphenationOptions = HyphenationOptions.DEFAULT,
) {
    /**
     * Convenience constructor that reads dictionaries from the app's assets.
     *
     * @param context any [Context]; only its [AssetManager] is retained.
     * @param options the default hyphenation policy (default [HyphenationOptions.DEFAULT]).
     */
    @JvmOverloads
    constructor(context: Context, options: HyphenationOptions = HyphenationOptions.DEFAULT) :
        this(context.assets, options = options)

    private val engines = ConcurrentHashMap<String, HyphenationEngine>()

    /**
     * Returns the [HyphenationEngine] for [lang], loading and caching it on first use.
     *
     * Use this directly when you want to hyphenate many words in the same language, or need a
     * custom separator (see [HyphenationEngine.hyphenate]).
     *
     * @param lang language code matching a bundled `hyph_<lang>.dic`, e.g. `"sk_SK"`.
     * @return the cached engine for [lang]; the same instance is returned on later calls.
     * @throws java.io.IOException if no dictionary is bundled for [lang].
     */
    fun forLanguage(lang: String): HyphenationEngine = engines.getOrPut(lang) {
        assets.open("$assetDir/hyph_$lang.dic").use { HyphenationEngine.fromDic(it.readBytes()) }
    }

    /**
     * Hyphenates a single [word], inserting a soft hyphen (U+00AD) at every valid break point.
     *
     * @param word a single word (no surrounding whitespace); case is preserved.
     * @param lang language code, e.g. `"de_DE"`.
     * @return [word] with soft hyphens inserted, or [word] unchanged if it has no break points.
     * @throws java.io.IOException if no dictionary is bundled for [lang].
     */
    fun hyphenate(word: String, lang: String): String = forLanguage(lang).hyphenate(word)

    /**
     * Returns the break positions of a single [word]: indices *before which* a hyphen may be
     * inserted. For example `positions("bratislava", "sk_SK")` returns `[3, 5, 8]`
     * (`bra|ti|sla|va`).
     *
     * @param word a single word (no surrounding whitespace).
     * @param lang language code, e.g. `"sk_SK"`.
     * @return break indices in ascending order, or an empty list if the word cannot be split.
     * @throws java.io.IOException if no dictionary is bundled for [lang].
     */
    fun positions(word: String, lang: String): List<Int> = forLanguage(lang).positions(word)

    /**
     * Hyphenates every alphabetic run in [text], leaving whitespace, punctuation and digits
     * untouched. This is what you usually feed to a `TextView`/Compose `Text`.
     *
     * Applies this instance's policy; pass [options] to override it for a single call (build one
     * with [HyphenationOptions.Builder] or `instanceOptions.newBuilder()`).
     *
     * @param text arbitrary text (a sentence, paragraph, …).
     * @param lang language code, e.g. `"sk_SK"`.
     * @param options hyphenation policy for this call (default: the instance's options).
     * @return [text] hyphenated per [options].
     * @throws java.io.IOException if no dictionary is bundled for [lang].
     */
    @JvmOverloads
    fun hyphenateText(text: String, lang: String, options: HyphenationOptions = this.options): String =
        forLanguage(lang).hyphenateText(
            text, options.binding, options.separator,
            options.avoidHyphenatingLastWord, options.minimumLastLineLetters,
        )

    /**
     * HTML-aware hyphenation: transforms only text content of [html] (soft hyphens + one-letter
     * binding + runt prevention), preserving all tags/attributes/custom elements and skipping
     * `<script>`/`<style>`.
     *
     * Prefer this over calling [hyphenateText] per DOM text node: it binds a one-letter word to the
     * following word **even across inline markup** (`<strong>`, `<em>`, `<a>`, custom elements, …),
     * which a per-node string call cannot do. Binding and runt prevention never cross block
     * boundaries. Malformed input is returned unchanged; the transform is idempotent.
     *
     * Applies this instance's policy; pass [options] to override it for a single call.
     *
     * @param html an HTML fragment.
     * @param lang language code, e.g. `"sk_SK"`.
     * @param options hyphenation policy for this call (default: the instance's options).
     * @return [html] with only its text content hyphenated per [options].
     * @throws java.io.IOException if no dictionary is bundled for [lang].
     */
    @JvmOverloads
    fun hyphenateHtml(html: String, lang: String, options: HyphenationOptions = this.options): String =
        forLanguage(lang).hyphenateHtml(
            html, options.binding, options.separator,
            options.avoidHyphenatingLastWord, options.minimumLastLineLetters,
        )

    /**
     * Lists the language codes for which a dictionary is bundled (derived from the `hyph_*.dic`
     * files in [assetDir]).
     *
     * @return language codes such as `["cs_CZ", "de_DE", "en_US", …, "uk_UA"]`, sorted.
     */
    fun availableLanguages(): List<String> =
        (assets.list(assetDir) ?: emptyArray())
            .filter { it.startsWith("hyph_") && it.endsWith(".dic") }
            .map { it.removePrefix("hyph_").removeSuffix(".dic") }
            .sorted()
}
