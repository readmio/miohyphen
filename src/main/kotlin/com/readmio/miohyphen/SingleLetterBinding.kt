package com.readmio.miohyphen

/**
 * How a one-letter word (Slovak/Czech prepositions/conjunctions like `v`, `s`, `a`, `i`, …) is kept
 * attached to the following word so it never dangles at a line end.
 *
 * The [separator] replaces the ordinary space between the one-letter word and the next word.
 */
enum class SingleLetterBinding(
    /** The string that replaces the separating space, or `null` to disable binding. */
    val separator: String?,
) {
    /**
     * **Default.** A normal space (U+0020) followed by a WORD JOINER (U+2060). The space still
     * stretches under text justification, while the word joiner forbids a line break there
     * (UAX #14). Recommended — a plain no-break space would leave the gap unstretched on a
     * justified line.
     */
    SPACE_WORD_JOINER("\u0020\u2060"),

    /**
     * A single NO-BREAK SPACE (U+00A0). Non-breaking, but fixed-width: on a justified line the gap
     * stays tight while the rest of the line stretches.
     */
    NO_BREAK_SPACE("\u00A0"),

    /** Do not bind one-letter words; leave the ordinary space as-is. */
    NONE(null),
}
