import com.readmio.miohyphen.HyphenationEngine
import java.io.File

/** Usage: Demo <assetsDir> <lang> <word> [<word> ...] */
fun main(args: Array<String>) {
    val assets = args[0]
    val lang = args[1]
    val engine = HyphenationEngine.fromDic(File(assets, "hyph_$lang.dic").readBytes())
    for (w in args.drop(2)) println("  $w -> ${engine.hyphenate(w, "·")}")
}
