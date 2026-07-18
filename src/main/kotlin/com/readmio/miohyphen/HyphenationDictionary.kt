package com.readmio.miohyphen

/**
 * A parsed LibreOffice / hunspell `hyph_*.dic` hyphenation dictionary: the Liang/TeX patterns
 * plus the left/right minimums. This is the same pattern data the hunspell `libhyphen` engine
 * (behind ushuaia.pl) and LibreOffice use.
 *
 * The parser is a faithful port of the `pyphen` reference. In particular, two-level (compound)
 * dictionaries — e.g. German, which has a `NEXTLEVEL` section — are handled the way pyphen does:
 * the second-level patterns are merged into the same flat pattern set, with later patterns
 * overriding earlier ones on a key collision. That is what makes German compounds split.
 *
 * **KMP-friendly:** uses only the common Kotlin stdlib — no `java.*`, no Android, no third-party
 * dependencies — so this class can live in a Kotlin Multiplatform `commonMain` and run on the JVM,
 * Android and Kotlin/Native (iOS). It decodes the dictionary's declared charset itself, supporting
 * the encodings the LibreOffice patterns use (UTF-8, ISO-8859-1, ISO-8859-2).
 */
class HyphenationDictionary private constructor(
    internal val patterns: Map<String, Pattern>,
    internal val maxKeyLength: Int,
    /** Minimum characters before the first break (`LEFTHYPHENMIN`; defaults to 2). */
    val leftMin: Int,
    /** Minimum characters after the last break (`RIGHTHYPHENMIN`; defaults to 2). */
    val rightMin: Int,
    /** The charset named on the dictionary's first line, e.g. `"UTF-8"` or `"ISO8859-2"`. */
    val charsetName: String,
    /** Number of non-standard (Németh) patterns dropped — these need the literal C libhyphen. */
    val droppedNonStandard: Int,
    /** True if the dictionary declared a second (compound) level; it is merged, not ignored. */
    val hasSecondLevel: Boolean,
) {
    /** A parsed pattern: point [values] to apply starting at [start] letters into the key. */
    internal class Pattern(val start: Int, val values: IntArray)

    companion object {
        private const val DEFAULT_MIN = 2
        private val CARET_HEX = Regex("""\^\^([0-9a-f]{2})""")

        /** Header keywords consumed without producing a pattern. */
        private val IGNORED_PREFIXES = listOf("COMPOUNDLEFTHYPHENMIN", "COMPOUNDRIGHTHYPHENMIN")

        /**
         * Parses a `hyph_*.dic` file into a dictionary.
         *
         * Reads the charset from the first line, honours `LEFTHYPHENMIN`/`RIGHTHYPHENMIN`, merges
         * a `NEXTLEVEL` (compound) section into the same pattern set, and skips comments and
         * non-standard (`/`) patterns. Never throws for malformed content — unknown lines are
         * ignored.
         *
         * @param bytes the full contents of a LibreOffice/hunspell `hyph_*.dic` file.
         * @return the parsed dictionary.
         */
        fun parse(bytes: ByteArray): HyphenationDictionary {
            val nl = bytes.indexOf('\n'.code.toByte())
            val headerEnd = if (nl >= 0) nl else bytes.size
            // The charset name on the first line is ASCII, so UTF-8 decoding reads it fine.
            val charsetName = bytes.copyOfRange(0, headerEnd).decodeToString().trim()
            val text = decodeBytes(bytes, charsetName)

            val patterns = HashMap<String, Pattern>()
            var leftMin = DEFAULT_MIN
            var rightMin = DEFAULT_MIN
            var dropped = 0
            var hasSecondLevel = false
            var maxKey = 0
            var firstLine = true

            for (raw in text.lineSequence()) {
                if (firstLine) { firstLine = false; continue }   // charset line
                var line = raw.trim()
                // Comment conventions: '%' (TeX) and '#' (e.g. the de_DE header block).
                if (line.isEmpty() || line.startsWith("%") || line.startsWith("#")) continue

                val upper = line.uppercase()
                when {
                    upper.startsWith("LEFTHYPHENMIN") -> { leftMin = tailInt(line, leftMin); continue }
                    upper.startsWith("RIGHTHYPHENMIN") -> { rightMin = tailInt(line, rightMin); continue }
                    upper == "NEXTLEVEL" -> { hasSecondLevel = true; continue }   // merge, do NOT stop
                    IGNORED_PREFIXES.any { upper.startsWith(it) } -> continue
                }

                if (line.indexOf('^') >= 0) {
                    line = CARET_HEX.replace(line) { m -> m.groupValues[1].toInt(16).toChar().toString() }
                }
                // Non-standard hyphenation (`pat/change,idx,cut`): unsupported by standard Liang.
                if ('/' in line && '=' in line) { dropped++; continue }

                val parsed = parsePattern(line) ?: continue   // all-zero line -> skip
                if (parsed.key.length > maxKey) maxKey = parsed.key.length
                patterns[parsed.key] = Pattern(parsed.start, parsed.values)   // last wins
            }
            return HyphenationDictionary(
                patterns, maxKey, leftMin, rightMin, charsetName, dropped, hasSecondLevel,
            )
        }

        private class Parsed(val key: String, val start: Int, val values: IntArray)

        /**
         * Split a pattern into its letter key and per-gap point values, mirroring pyphen's
         * `re.findall(r'(\d?)(\D?)', pattern)`: read an optional digit then an optional
         * non-digit, repeatedly, with one trailing empty gap. Returns null if all values are 0.
         */
        private fun parsePattern(p: String): Parsed? {
            val key = StringBuilder(p.length)
            val values = ArrayList<Int>(p.length + 1)
            var i = 0
            while (i < p.length) {
                var digit = 0
                if (p[i].isDigit()) { digit = p[i] - '0'; i++ }
                if (i < p.length && !p[i].isDigit()) { key.append(p[i]); i++ }
                values.add(digit)
            }
            values.add(0)   // trailing gap from the final empty match

            var max = 0
            for (v in values) if (v > max) max = v
            if (max == 0) return null

            var start = 0
            while (values[start] == 0) start++
            var end = values.size
            while (values[end - 1] == 0) end--
            val trimmed = IntArray(end - start) { values[start + it] }
            return Parsed(key.toString(), start, trimmed)
        }

        private fun tailInt(line: String, fallback: Int): Int =
            line.trim().substringAfterLast(' ').toIntOrNull() ?: fallback

        /**
         * Decodes [bytes] using the dictionary's declared [charsetName], with pure-Kotlin
         * single-byte decoders (so no `java.nio.charset` is needed). Supports the charsets the
         * LibreOffice `hyph_*.dic` files use — UTF-8, ISO-8859-1 (Latin-1), ISO-8859-2 (Latin-2);
         * anything else falls back to UTF-8. To add another single-byte charset, add its high-half
         * (0xA0–0xFF) table like [LATIN2_HIGH] and a branch here.
         */
        private fun decodeBytes(bytes: ByteArray, charsetName: String): String {
            val norm = charsetName.uppercase().replace("-", "").replace("_", "")
            return when (norm) {
                "ISO88591", "LATIN1" ->
                    CharArray(bytes.size) { (bytes[it].toInt() and 0xFF).toChar() }.concatToString()
                "ISO88592", "LATIN2" ->
                    CharArray(bytes.size) {
                        val b = bytes[it].toInt() and 0xFF
                        if (b < 0xA0) b.toChar() else LATIN2_HIGH[b - 0xA0]
                    }.concatToString()
                else -> bytes.decodeToString()   // UTF-8 (and best-effort fallback)
            }
        }

        /** ISO-8859-2 (Latin-2) high half: Unicode for bytes 0xA0..0xFF (index = byte - 0xA0). */
        private val LATIN2_HIGH: String =
            "\u00A0\u0104\u02D8\u0141\u00A4\u013D\u015A\u00A7\u00A8\u0160\u015E\u0164\u0179\u00AD\u017D\u017B" +
            "\u00B0\u0105\u02DB\u0142\u00B4\u013E\u015B\u02C7\u00B8\u0161\u015F\u0165\u017A\u02DD\u017E\u017C" +
            "\u0154\u00C1\u00C2\u0102\u00C4\u0139\u0106\u00C7\u010C\u00C9\u0118\u00CB\u011A\u00CD\u00CE\u010E" +
            "\u0110\u0143\u0147\u00D3\u00D4\u0150\u00D6\u00D7\u0158\u016E\u00DA\u0170\u00DC\u00DD\u0162\u00DF" +
            "\u0155\u00E1\u00E2\u0103\u00E4\u013A\u0107\u00E7\u010D\u00E9\u0119\u00EB\u011B\u00ED\u00EE\u010F" +
            "\u0111\u0144\u0148\u00F3\u00F4\u0151\u00F6\u00F7\u0159\u016F\u00FA\u0171\u00FC\u00FD\u0163\u02D9"

        private fun ByteArray.indexOf(b: Byte): Int {
            for (i in indices) if (this[i] == b) return i
            return -1
        }
    }
}
