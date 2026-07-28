package com.readmio.miohyphen

/**
 * Hyphenation engine for a single language.
 *
 * Runs the Liang/TeX algorithm over a [HyphenationDictionary] of LibreOffice patterns — the same
 * patterns the hunspell `libhyphen` engine uses. The implementation is a self-contained, pure
 * Kotlin/JVM port of the `pyphen` reference (no Android, no third-party dependencies), so its
 * output matches pyphen, including merged two-level/compound dictionaries such as German.
 *
 * Obtain one via [MioHyphen.forLanguage] (Android) or [fromDic] (any JVM). Instances are
 * immutable and thread-safe; build one per language and reuse it.
 */
class HyphenationEngine internal constructor(private val dict: HyphenationDictionary) {

    /** Minimum number of characters that must precede the first break (from the dictionary header). */
    val leftMin: Int get() = dict.leftMin

    /** Minimum number of characters that must follow the last break (from the dictionary header). */
    val rightMin: Int get() = dict.rightMin

    /**
     * Computes the break positions of [word]: indices *before which* a hyphen may be inserted.
     *
     * For example `positions("bratislava")` returns `[3, 5, 8]`, i.e. `bra|ti|sla|va`. Matching is
     * case-insensitive; the returned indices refer to the original [word]. Positions closer than
     * [leftMin] to the start or [rightMin] to the end are excluded.
     *
     * @param word a single word (no surrounding whitespace).
     * @return break indices in ascending order, or an empty list if [word] cannot be split.
     */
    fun positions(word: String): List<Int> {
        val w = word.lowercase()
        val n = w.length
        val rightBound = n - dict.rightMin
        if (rightBound < dict.leftMin) return emptyList()

        val pointed = ".$w."
        val len = pointed.length
        val refs = IntArray(len + 1)
        val maxLen = dict.maxKeyLength

        for (i in 0 until len - 1) {
            val jMax = minOf(i + maxLen, len)
            var j = i + 1
            while (j <= jMax) {
                val pat = dict.patterns[pointed.substring(i, j)]
                if (pat != null) {
                    val values = pat.values
                    val base = i + pat.start
                    var k = 0
                    while (k < values.size) {
                        val idx = base + k
                        if (idx < refs.size && values[k] > refs[idx]) refs[idx] = values[k]
                        k++
                    }
                }
                j++
            }
        }

        val out = ArrayList<Int>()
        for (idx in refs.indices) {
            if (refs[idx] and 1 == 1) {
                val pos = idx - 1   // shift back over the leading '.'
                if (pos in dict.leftMin..rightBound) out.add(pos)
            }
        }
        return out
    }

    /**
     * Inserts [separator] at every valid break point of [word].
     *
     * With the default separator this produces a soft-hyphenated string ready for a `TextView`;
     * pass a visible separator (e.g. `"·"` or `"-"`) to inspect the breaks.
     *
     * ```
     * engine.hyphenate("bratislava")        // "bra­ti­sla­va"  (invisible U+00AD)
     * engine.hyphenate("bratislava", "·")   // "bra·ti·sla·va"
     * ```
     *
     * @param word a single word (no surrounding whitespace); case is preserved.
     * @param separator the string inserted at each break point (default [SOFT_HYPHEN]).
     * @return [word] with separators inserted, or [word] unchanged if it has no break points.
     */
    fun hyphenate(word: String, separator: String = SOFT_HYPHEN): String {
        val pos = positions(word)
        if (pos.isEmpty()) return word
        val sb = StringBuilder(word.length + pos.size * separator.length)
        var prev = 0
        for (p in pos) {
            sb.append(word, prev, p).append(separator)
            prev = p
        }
        sb.append(word, prev, word.length)
        return sb.toString()
    }

    /**
     * Hyphenates every alphabetic run in [text], leaving whitespace, punctuation and digits
     * untouched — suitable for feeding a `TextView`/Compose `Text`.
     *
     * [binding] controls one-letter-word binding — the Slovak/Czech rule that a one-letter
     * preposition/conjunction (`v`, `s`, `a`, `i`, …) must not end a line. The separating space is
     * replaced with the binding's separator (default [SingleLetterBinding.SPACE_WORD_JOINER]),
     * chaining across consecutive one-letter words; use [SingleLetterBinding.NONE] to disable.
     *
     * Runt prevention (on by default): [avoidHyphenatingLastWord] drops trailing separators from the
     * last word while the tail after them would be shorter than [minimumLastLineLetters] letters, so
     * a hyphenated last word can't strand a tiny fragment (a "runt") on the last line.
     *
     * @param text arbitrary text (a sentence, paragraph, …).
     * @param binding how one-letter words are bound (default [SingleLetterBinding.SPACE_WORD_JOINER]).
     * @param separator the string inserted at each in-word break point (default [SOFT_HYPHEN]).
     * @param avoidHyphenatingLastWord avoid a short last-line fragment (runt) at the end.
     * @param minimumLastLineLetters shortest allowed last fragment when [avoidHyphenatingLastWord].
     * @return [text] with soft hyphens inside words and one-letter words bound per [binding].
     */
    fun hyphenateText(
        text: String,
        binding: SingleLetterBinding = SingleLetterBinding.SPACE_WORD_JOINER,
        separator: String = SOFT_HYPHEN,
        avoidHyphenatingLastWord: Boolean = true,
        minimumLastLineLetters: Int = 4,
    ): String {
        val bindSep = binding.separator
        val sb = StringBuilder(text.length + text.length / 8)
        val n = text.length
        var i = 0
        var lastWordStart = -1
        var lastWordEnd = -1
        while (i < n) {
            if (text[i].isLetter()) {
                val start = i
                while (i < n && text[i].isLetter()) i++
                // A one-letter word directly followed by a plain space: bind it to the next word.
                if (bindSep != null && i - start == 1 && i < n && text[i] == ' ') {
                    lastWordStart = sb.length
                    sb.append(text[start])
                    lastWordEnd = sb.length
                    sb.append(bindSep)
                    i++   // consume the original space (replaced by the binder)
                    if (i < n && text[i] == '\u2060') i++   // absorb an existing word joiner (idempotency)
                } else {
                    lastWordStart = sb.length
                    sb.append(hyphenate(text.substring(start, i), separator))
                    lastWordEnd = sb.length
                }
            } else {
                sb.append(text[i])
                i++
            }
        }
        if (avoidHyphenatingLastWord && lastWordStart >= 0) {
            deStubLastWord(sb, lastWordStart, lastWordEnd, separator, minimumLastLineLetters)
        }
        return sb.toString()
    }

    /**
     * Hyphenates the **text content of an HTML fragment**, leaving every tag, attribute and custom
     * element (e.g. `<sound>`) untouched, and skipping the contents of `<script>`/`<style>`.
     *
     * Unlike calling [hyphenateText] per DOM text node, this sees across inline markup: a one-letter
     * word is bound to the following word **even when a `<strong>`, `<b>`, `<em>`, `<a>`, … or a
     * custom element sits between them**, regardless of where the separating space sits —
     * `a <strong>x`, `a<strong> x` and `a<strong>x` all become `a`+sep+`<strong>`+`x` (one binder,
     * no leftover space). The binder is [binding]'s separator (default a space + WORD JOINER, which
     * stays non-breaking yet still justifies). Binding never crosses a block boundary (`</p>`,
     * `<br>`, list items, `<script>`/`<style>`); a one-letter word that ends a block is left alone.
     *
     * Malformed input is returned unchanged rather than throwing, and the transform is idempotent.
     * Runt prevention ([avoidHyphenatingLastWord], on by default) is applied per paragraph — the last
     * word before each block boundary won't leave fewer than [minimumLastLineLetters] letters on the
     * last line.
     *
     * @param html an HTML fragment.
     * @param binding how one-letter words are bound (default [SingleLetterBinding.SPACE_WORD_JOINER]).
     * @param separator the string inserted at each in-word break point (default [SOFT_HYPHEN]).
     * @param avoidHyphenatingLastWord avoid a short last-line fragment (runt) at each paragraph end.
     * @param minimumLastLineLetters shortest allowed last fragment when [avoidHyphenatingLastWord].
     * @return [html] with soft hyphens inside text words and one-letter words bound per [binding].
     */
    fun hyphenateHtml(
        html: String,
        binding: SingleLetterBinding = SingleLetterBinding.SPACE_WORD_JOINER,
        separator: String = SOFT_HYPHEN,
        avoidHyphenatingLastWord: Boolean = true,
        minimumLastLineLetters: Int = 4,
    ): String = try {
        renderHtml(
            tokenizeHtml(html), binding.separator, separator,
            avoidHyphenatingLastWord, minimumLastLineLetters,
        )
    } catch (t: Throwable) {
        html   // never throw on malformed markup — hyphenation is a best-effort enhancement
    }

    private fun renderHtml(
        tokens: List<HtmlToken>,
        bindSep: String?,
        sep: String,
        avoidLastWord: Boolean,
        minLastLetters: Int,
    ): String {
        val bind = bindSep != null
        val shy = '\u00AD'
        val out = StringBuilder()
        var bindPending = false          // last emitted word was a one-letter word awaiting a follower
        val gapRaw = StringBuilder()     // exact gap content (spaces + inline tags) — emitted if no bind
        val gapTags = StringBuilder()    // only the inline tags — emitted (after NBSP) if we bind

        var lastWordStart = -1           // output range of the last word of the current paragraph
        var lastWordEnd = -1

        for (tok in tokens) {
            when (tok.kind) {
                T_INLINE -> if (bind && bindPending) {
                    gapRaw.append(tok.text); gapTags.append(tok.text)
                } else {
                    out.append(tok.text)
                }
                T_BARRIER -> {   // block boundary ends the paragraph
                    if (bind && bindPending) {
                        out.append(gapRaw); gapRaw.setLength(0); gapTags.setLength(0); bindPending = false
                    }
                    if (avoidLastWord && lastWordStart >= 0) {
                        deStubLastWord(out, lastWordStart, lastWordEnd, sep, minLastLetters)
                    }
                    out.append(tok.text)
                    lastWordStart = -1; lastWordEnd = -1
                }
                else -> {   // T_TEXT
                    val s = tok.text
                    val n = s.length
                    var i = 0
                    while (i < n) {
                        val c = s[i]
                        if (c.isLetter()) {
                            val start = i
                            while (i < n && (s[i].isLetter() || s[i] == shy)) i++
                            val run = s.substring(start, i)
                            val clean = if (run.indexOf(shy) >= 0) run.replace(SOFT_HYPHEN, "") else run
                            if (bind && bindPending) {   // this word closes a pending one-letter word
                                out.append(bindSep).append(gapTags)
                                gapRaw.setLength(0); gapTags.setLength(0); bindPending = false
                            }
                            lastWordStart = out.length
                            if (clean.length == 1) {
                                out.append(clean)
                                if (bind) { bindPending = true; gapRaw.setLength(0); gapTags.setLength(0) }
                            } else {
                                out.append(hyphenate(clean, sep))
                                bindPending = false
                            }
                            lastWordEnd = out.length
                        } else if (bind && bindPending && isHtmlSpace(c)) {
                            gapRaw.append(c); i++
                        } else {
                            if (bind && bindPending) {   // punctuation etc. — not a bindable follower
                                out.append(gapRaw); gapRaw.setLength(0); gapTags.setLength(0); bindPending = false
                            }
                            out.append(c); i++
                        }
                    }
                }
            }
        }
        if (bind && bindPending) out.append(gapRaw)   // one-letter word at the very end: nothing to bind
        if (avoidLastWord && lastWordStart >= 0) {
            deStubLastWord(out, lastWordStart, lastWordEnd, sep, minLastLetters)
        }
        return out.toString()
    }

    /** A tokenized piece of HTML: transformable text, a transparent inline tag, or an opaque barrier. */
    private class HtmlToken(val text: String, val kind: Int)

    companion object {
        /** U+00AD SOFT HYPHEN — invisible unless the renderer breaks the line there. */
        const val SOFT_HYPHEN: String = "\u00AD"

        private const val T_TEXT = 0
        private const val T_INLINE = 1
        private const val T_BARRIER = 2

        /** Block-level / boundary tags across which a one-letter word must NOT be bound. */
        private val BLOCK_TAGS = setOf(
            "p", "div", "br", "hr", "li", "ul", "ol", "dl", "dt", "dd",
            "h1", "h2", "h3", "h4", "h5", "h6", "section", "article", "aside", "nav",
            "header", "footer", "main", "figure", "figcaption", "blockquote", "pre",
            "table", "thead", "tbody", "tfoot", "tr", "td", "th", "caption",
            "form", "fieldset", "address", "hgroup", "details", "summary", "dialog",
        )

        private fun isHtmlSpace(c: Char): Boolean =
            c == ' ' || c == '\n' || c == '\t' || c == '\r' || c == '\u000C'

        /** Split an HTML fragment into text / inline-tag / opaque-barrier tokens (see [HtmlToken]). */
        private fun tokenizeHtml(html: String): List<HtmlToken> {
            val tokens = ArrayList<HtmlToken>()
            val text = StringBuilder()
            val n = html.length
            var i = 0
            fun flushText() {
                if (text.isNotEmpty()) { tokens.add(HtmlToken(text.toString(), T_TEXT)); text.setLength(0) }
            }
            while (i < n) {
                val c = html[i]
                if (c != '<') { text.append(c); i++; continue }
                when {
                    html.startsWith("<!--", i) -> {   // comment: preserve verbatim, transparent
                        val end = html.indexOf("-->", i + 4)
                        val stop = if (end < 0) n else end + 3
                        flushText(); tokens.add(HtmlToken(html.substring(i, stop), T_INLINE)); i = stop
                    }
                    i + 1 < n && html[i + 1] == '!' -> {   // doctype / CDATA
                        val end = html.indexOf('>', i)
                        val stop = if (end < 0) n else end + 1
                        flushText(); tokens.add(HtmlToken(html.substring(i, stop), T_INLINE)); i = stop
                    }
                    i + 1 < n && (html[i + 1].isLetter() || html[i + 1] == '/') -> {
                        val tagEnd = findTagEnd(html, i)
                        if (tagEnd < 0) { text.append(c); i++; continue }   // no '>' — treat '<' as text
                        val rawTag = html.substring(i, tagEnd)
                        val name = tagName(rawTag)
                        flushText()
                        if ((name == "script" || name == "style") && !rawTag.trimEnd().endsWith("/>")) {
                            // consume the element whole (tag + raw content + close): never transformed
                            val closeIdx = html.indexOf("</$name", tagEnd, ignoreCase = true)
                            val stop = when {
                                closeIdx < 0 -> n
                                else -> html.indexOf('>', closeIdx).let { if (it < 0) n else it + 1 }
                            }
                            tokens.add(HtmlToken(html.substring(i, stop), T_BARRIER)); i = stop
                        } else {
                            val kind = if (name in BLOCK_TAGS) T_BARRIER else T_INLINE
                            tokens.add(HtmlToken(rawTag, kind)); i = tagEnd
                        }
                    }
                    else -> { text.append(c); i++ }   // a stray '<' (e.g. "a < b")
                }
            }
            flushText()
            return tokens
        }

        /** Index just past the closing `>` of the tag at [start], respecting quotes; -1 if unterminated. */
        private fun findTagEnd(s: String, start: Int): Int {
            var i = start + 1
            var quote = ' '
            while (i < s.length) {
                val c = s[i]
                if (quote != ' ') { if (c == quote) quote = ' ' }
                else if (c == '"' || c == '\'') quote = c
                else if (c == '>') return i + 1
                i++
            }
            return -1
        }

        /** Lower-cased tag name of a raw tag like `<strong>`, `</p>`, `<a href=…>`, `<br/>`. */
        private fun tagName(raw: String): String {
            var i = 1
            if (i < raw.length && raw[i] == '/') i++
            val start = i
            while (i < raw.length && raw[i].isLetterOrDigit()) i++
            return raw.substring(start, i).lowercase()
        }

        /**
         * Removes trailing [sep] separators from the paragraph's last word (range [start, end) in
         * [sb]) while the tail after the last remaining separator has fewer than [minLast] letters —
         * so a hyphenated last word can't strand a tiny fragment on the paragraph's last line.
         *
         * Uses only common Kotlin stdlib (`substring`, `lastIndexOf`, `deleteRange`) — KMP-safe.
         */
        private fun deStubLastWord(sb: StringBuilder, start: Int, end: Int, sep: String, minLast: Int) {
            if (sep.isEmpty() || minLast <= 0 || end <= start) return
            val word = sb.substring(start, end)
            val drops = ArrayList<Int>()   // separator start indices within `word`, descending
            var searchFrom = word.length
            while (true) {
                val idx = word.lastIndexOf(sep, searchFrom - 1)
                if (idx < 0) break
                // letters remaining after this separator, once the separators after it are dropped
                val tailLetters = (word.length - (idx + sep.length)) - drops.size * sep.length
                if (tailLetters >= minLast) break
                drops.add(idx)
                searchFrom = idx
            }
            for (rel in drops) sb.deleteRange(start + rel, start + rel + sep.length)
        }

        /**
         * Builds an engine from the raw bytes of a `hyph_*.dic` file.
         *
         * Use this on a plain JVM (or when loading dictionaries yourself); Android callers normally
         * use [MioHyphen] instead, which loads bundled dictionaries from assets.
         *
         * @param bytes the full contents of a LibreOffice/hunspell `hyph_*.dic` file.
         * @return an engine backed by the parsed dictionary.
         */
        fun fromDic(bytes: ByteArray): HyphenationEngine =
            HyphenationEngine(HyphenationDictionary.parse(bytes))
    }
}
