import com.readmio.miohyphen.HyphenationDictionary
import com.readmio.miohyphen.HyphenationEngine
import java.io.File

/**
 * Standalone JVM verifier for the SHIPPED engine code (HyphenationDictionary + HyphenationEngine).
 * Loads each bundled .dic exactly as the Android facade would, hyphenates the golden words, and
 * diffs against the pyphen-derived golden vectors (= the desired pure-Liang quality).
 *
 * Usage: Runner <assetsDir> <goldenDir> <lang> [<lang> ...]
 */
fun main(args: Array<String>) {
    val assetsDir = args[0]
    val goldenDir = args[1]
    val langs = args.drop(2)

    var grandOk = 0
    var grandTotal = 0
    var failed = false

    for (lang in langs) {
        val dicBytes = File(assetsDir, "hyph_$lang.dic").readBytes()
        val dict = HyphenationDictionary.parse(dicBytes)
        val engine = HyphenationEngine.fromDic(dicBytes)
        println(
            "--- $lang: charset=${dict.charsetName}, " +
                "non-standard dropped=${dict.droppedNonStandard}, secondLevel=${dict.hasSecondLevel}"
        )

        var ok = 0
        var total = 0
        val diffs = ArrayList<String>()
        File(goldenDir, "$lang.tsv").forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val (word, idxPart) = line.split('\t', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            val expected = if (idxPart.isBlank()) emptyList() else idxPart.split(',').map { it.toInt() }
            val actual = engine.positions(word)
            total++
            if (actual == expected) ok++ else diffs.add("  DIFF $word: expected=$expected actual=$actual")
        }
        grandOk += ok; grandTotal += total
        val pct = if (total == 0) 0 else ok * 100 / total
        println("=== $lang (lmin=${engine.leftMin}, rmin=${engine.rightMin}): $ok/$total ($pct%) ===")
        diffs.forEach(::println)
        if (diffs.isEmpty()) println("  all identical")
        if (diffs.isNotEmpty()) failed = true
        // show a few rendered samples
        listOf("bratislava", "vykreslenie", "computer", "hyphenation").forEach { w ->
            runCatching { println("    sample $w -> ${engine.hyphenate(w, "·")}") }
        }
    }

    println("\nTOTAL: $grandOk/$grandTotal (${if (grandTotal==0) 0 else grandOk*100/grandTotal}%)")
    if (failed) { System.err.println("FAILURES PRESENT"); kotlin.system.exitProcess(1) }
    println("ALL GOLDEN VECTORS MATCH — shipped Kotlin engine reproduces the reference output.")
}
