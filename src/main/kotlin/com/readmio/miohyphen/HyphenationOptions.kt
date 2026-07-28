package com.readmio.miohyphen

/**
 * Immutable hyphenation policy for [MioHyphen] — set once (usually at construction) and reused.
 *
 * Build it with [Builder] (fluent, Java-friendly):
 *
 * ```
 * val options = HyphenationOptions.Builder()
 *     .binding(SingleLetterBinding.SPACE_WORD_JOINER)
 *     .minimumLastLineLetters(5)
 *     .build()
 * val mioHyphen = MioHyphen(context, options)
 * ```
 *
 * Use [HyphenationOptions.DEFAULT] for the library defaults, or [newBuilder] to tweak an existing
 * instance.
 */
class HyphenationOptions private constructor(
    /** How one-letter words are kept with the next word (default [SingleLetterBinding.SPACE_WORD_JOINER]). */
    val binding: SingleLetterBinding,
    /** The string inserted at each in-word break point (default [HyphenationEngine.SOFT_HYPHEN]). */
    val separator: String,
    /** Whether to avoid a short last-line fragment (runt) at a paragraph's end (default true). */
    val avoidHyphenatingLastWord: Boolean,
    /** Shortest last fragment allowed when [avoidHyphenatingLastWord] (default 4). */
    val minimumLastLineLetters: Int,
) {
    /** A [Builder] pre-populated with this instance's values, for tweaking one field. */
    fun newBuilder(): Builder = Builder()
        .binding(binding)
        .separator(separator)
        .avoidHyphenatingLastWord(avoidHyphenatingLastWord)
        .minimumLastLineLetters(minimumLastLineLetters)

    /** Fluent builder for [HyphenationOptions]. Unset fields keep the library defaults. */
    class Builder {
        private var binding: SingleLetterBinding = SingleLetterBinding.SPACE_WORD_JOINER
        private var separator: String = HyphenationEngine.SOFT_HYPHEN
        private var avoidHyphenatingLastWord: Boolean = true
        private var minimumLastLineLetters: Int = 4

        /** @see HyphenationOptions.binding */
        fun binding(binding: SingleLetterBinding) = apply { this.binding = binding }

        /** @see HyphenationOptions.separator */
        fun separator(separator: String) = apply { this.separator = separator }

        /** @see HyphenationOptions.avoidHyphenatingLastWord */
        fun avoidHyphenatingLastWord(enabled: Boolean) = apply { this.avoidHyphenatingLastWord = enabled }

        /** @see HyphenationOptions.minimumLastLineLetters */
        fun minimumLastLineLetters(letters: Int) = apply { this.minimumLastLineLetters = letters }

        fun build(): HyphenationOptions =
            HyphenationOptions(binding, separator, avoidHyphenatingLastWord, minimumLastLineLetters)
    }

    companion object {
        /** The library defaults (space + word joiner binding, soft-hyphen separator, runt prevention on, min 4). */
        val DEFAULT: HyphenationOptions = Builder().build()
    }
}
