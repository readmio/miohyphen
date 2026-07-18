package com.readmio.miohyphen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * JVM unit tests for the pure engine (no Android). Dictionaries are read from the module's
 * assets; golden break vectors (from the pyphen reference = desired pure-Liang quality) live
 * in `src/test/resources/golden/`.
 *
 * Run: `./gradlew :hyphenator:test`
 */
class HyphenationEngineTest {

    private fun engine(lang: String): HyphenationEngine =
        HyphenationEngine.fromDic(File("src/main/assets/hyphenation/hyph_$lang.dic").readBytes())

    private fun golden(lang: String): List<Pair<String, List<Int>>> =
        javaClass.getResourceAsStream("/golden/$lang.tsv")!!.bufferedReader().readLines()
            .filter { it.isNotBlank() }
            .map { line ->
                val (w, idx) = line.split('\t', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                w to (if (idx.isBlank()) emptyList() else idx.split(',').map(String::toInt))
            }

    private fun matchRate(lang: String): Pair<Int, Int> {
        val e = engine(lang)
        val g = golden(lang)
        val ok = g.count { (w, expected) -> e.positions(w) == expected }
        return ok to g.size
    }

    @Test fun `slovak matches reference exactly`() {
        val (ok, total) = matchRate("sk_SK")
        assertEquals(total, ok, "sk_SK: $ok/$total")
    }

    @Test fun `czech matches reference exactly`() {
        val (ok, total) = matchRate("cs_CZ")
        assertEquals(total, ok, "cs_CZ: $ok/$total")
    }

    @Test fun `polish matches reference exactly`() {
        val (ok, total) = matchRate("pl_PL")
        assertEquals(total, ok, "pl_PL: $ok/$total")
    }

    @Test fun `english matches reference exactly`() {
        val (ok, total) = matchRate("en_US")
        assertEquals(total, ok, "en_US: $ok/$total")
    }

    @Test fun `german (two-level compound) matches reference exactly`() {
        val (ok, total) = matchRate("de_DE")
        assertEquals(total, ok, "de_DE: $ok/$total")
    }

    @Test fun `spanish matches reference exactly`() {
        val (ok, total) = matchRate("es")
        assertEquals(total, ok, "es: $ok/$total")
    }

    @Test fun `ukrainian (cyrillic) matches reference exactly`() {
        val (ok, total) = matchRate("uk_UA")
        assertEquals(total, ok, "uk_UA: $ok/$total")
    }

    @Test fun `russian (cyrillic) matches reference exactly`() {
        val (ok, total) = matchRate("ru_RU")
        assertEquals(total, ok, "ru_RU: $ok/$total")
    }

    @Test fun `portuguese (european) matches reference exactly`() {
        val (ok, total) = matchRate("pt_PT")
        assertEquals(total, ok, "pt_PT: $ok/$total")
    }

    @Test fun `portuguese (brazilian) matches reference exactly`() {
        val (ok, total) = matchRate("pt_BR")
        assertEquals(total, ok, "pt_BR: $ok/$total")
    }

    @Test fun `italian matches reference exactly`() {
        val (ok, total) = matchRate("it_IT")
        assertEquals(total, ok, "it_IT: $ok/$total")
    }

    @Test fun `cyrillic words hyphenate`() {
        assertEquals("ком·пью·тер", engine("ru_RU").hyphenate("компьютер", "·"))
        // The LibreOffice Ukrainian dictionary is intentionally granular (matches pyphen).
        assertEquals("уні·ве·р·си·тет", engine("uk_UA").hyphenate("університет", "·"))
    }

    @Test fun `german compounds split at compound boundaries`() {
        val e = engine("de_DE")
        val dict = HyphenationDictionary.parse(File("src/main/assets/hyphenation/hyph_de_DE.dic").readBytes())
        assertTrue(dict.hasSecondLevel, "de_DE is expected to declare NEXTLEVEL (compound level)")
        assertEquals("Do·nau·dampf·schiff·fahrt", e.hyphenate("Donaudampfschifffahrt", "·"))
        assertEquals("Fuß·ball·welt·meis·ter·schaft", e.hyphenate("Fußballweltmeisterschaft", "·"))
        assertEquals("Kraft·fahr·zeug·ver·si·che·rung", e.hyphenate("Kraftfahrzeugversicherung", "·"))
    }

    @Test fun `soft hyphen insertion`() {
        val e = engine("sk_SK")
        val shy = HyphenationEngine.SOFT_HYPHEN
        assertEquals("bra${shy}ti${shy}sla${shy}va", e.hyphenate("bratislava"))
        assertEquals(0x00AD.toChar(), shy.single())
        assertEquals("bra·ti·sla·va", e.hyphenate("bratislava", "·"))
    }

    @Test fun `binds single-letter words with space + word joiner (default)`() {
        val e = engine("sk_SK")
        val j = SingleLetterBinding.SPACE_WORD_JOINER.separator!!
        assertEquals("\u0020\u2060", j)   // stretchable space + word joiner
        // chained one-letter words; multi-syllable words still hyphenate (visible separator)
        assertEquals("i${j}k${j}ne·mu", e.hyphenateText("i k nemu", separator = "·"))
        assertEquals("v${j}ško·le", e.hyphenateText("v škole", separator = "·"))
        // only one-letter words bind; "on"/"ona" (2+ letters) keep a normal space
        assertEquals("a${j}on a${j}ona", e.hyphenateText("a on a ona", separator = "·"))
    }

    @Test fun `binding style is configurable (nbsp)`() {
        val e = engine("sk_SK")
        val nb = SingleLetterBinding.NO_BREAK_SPACE.separator!!
        assertEquals("\u00A0", nb)
        assertEquals("i${nb}k${nb}ne·mu",
            e.hyphenateText("i k nemu", SingleLetterBinding.NO_BREAK_SPACE, "·"))
    }

    @Test fun `single-letter binding can be turned off`() {
        val e = engine("sk_SK")
        assertEquals("i k ne·mu",
            e.hyphenateText("i k nemu", SingleLetterBinding.NONE, "·"))
    }

    // --- hyphenateHtml: binding across inline markup boundaries ---------------------------------

    @Test fun `html binds one-letter word across inline markup (reported bug)`() {
        val e = engine("sk_SK")
        val j = SingleLetterBinding.SPACE_WORD_JOINER.separator!!
        assertEquals(
            "Oľ·ko a${j}<strong>za·tlies·kal od ra·dos·ti",
            e.hyphenateHtml("Oľko a<strong>zatlieskal od radosti", separator = "·"),
        )
        // previously-working trailing-space case must not regress (one binder, no doubled space)
        assertEquals(
            "vy·has·lo a${j}<strong>ener·gia vy·pr·cha·la",
            e.hyphenateHtml("vyhaslo a <strong>energia vyprchala", separator = "·"),
        )
    }

    @Test fun `html binding handles all space-placement variants`() {
        val e = engine("sk_SK")
        val expected = "a${SingleLetterBinding.SPACE_WORD_JOINER.separator}<strong>dom"
        assertEquals(expected, e.hyphenateHtml("a <strong>dom", separator = "·"))
        assertEquals(expected, e.hyphenateHtml("a<strong> dom", separator = "·"))
        assertEquals(expected, e.hyphenateHtml("a<strong>dom", separator = "·"))
        // also across a custom element
        assertEquals(
            "oba·liť a${SingleLetterBinding.SPACE_WORD_JOINER.separator}<sound>nie·co",
            e.hyphenateHtml("obaliť a <sound> nieco", separator = "·"),
        )
    }

    @Test fun `html honours the chosen binding style and off`() {
        val e = engine("sk_SK")
        val nb = SingleLetterBinding.NO_BREAK_SPACE.separator!!
        assertEquals("a${nb}<strong>dom",
            e.hyphenateHtml("a <strong>dom", SingleLetterBinding.NO_BREAK_SPACE, "·"))
        // NONE: no binder, original space kept
        assertEquals("a <strong>dom",
            e.hyphenateHtml("a <strong>dom", SingleLetterBinding.NONE, "·"))
    }

    @Test fun `html does not bind across block boundaries`() {
        val e = engine("sk_SK")
        assertEquals("dom a</p>", e.hyphenateHtml("dom a</p>", separator = "·"))
        assertEquals("dom a </p>", e.hyphenateHtml("dom a </p>", separator = "·"))
    }

    @Test fun `html leaves script, style and custom tags verbatim`() {
        val e = engine("sk_SK")
        assertEquals(
            "to·to je <script>var a = bratislava;</script> mes·to",
            e.hyphenateHtml("toto je <script>var a = bratislava;</script> mesto", separator = "·"),
        )
        assertEquals(
            "zvuk <sound file=\"x.mp3\"/> tu",
            e.hyphenateHtml("zvuk <sound file=\"x.mp3\"/> tu", separator = "·"),
        )
    }

    @Test fun `html transform is idempotent and never throws`() {
        val e = engine("sk_SK")
        val once = e.hyphenateHtml("Oľko a<strong>zatlieskal a i k nemu")
        assertEquals(once, e.hyphenateHtml(once))
        // unterminated / odd markup must not throw
        e.hyphenateHtml("<strong>unterminated a")
        e.hyphenateHtml("a <!-- comment")
        e.hyphenateHtml("cena a < b eur")
    }

    @Test fun `too-short words are not hyphenated`() {
        val e = engine("sk_SK")
        assertEquals(emptyList<Int>(), e.positions("aha"))
        assertEquals("aha", e.hyphenate("aha"))
    }
}
