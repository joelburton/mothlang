package com.joelburton.mothlang.cli

import com.joelburton.mothlang.Lox
import com.joelburton.mothlang.scanner.keywordsToTokens
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle.BLACK
import org.jline.utils.AttributedStyle.BLUE
import org.jline.utils.AttributedStyle.DEFAULT as D
import org.jline.utils.AttributedStyle.GREEN
import org.jline.utils.AttributedStyle.YELLOW
import java.util.regex.Pattern

internal class LoxHighlighter(val syms: Set<String>) : Highlighter {
    var str = "(\".*?\")"
    val num = "([-+]?[0-9]*\\.?[0-9]+)"
    val pr = "(true|false|nil)"
    val com = "(//.*)"
    val kw = "(${keywordsToTokens.keys.joinToString("|")})"
    val sym = "(\\w+)"
    val ws = "(\\s+)"
    val rest = "(\\S+)"
    val reAll = "$str|$num|$pr|$com|$kw|$sym|$ws|$rest".toRegex()

    override fun highlight(
        reader: LineReader, buffer: String
    ): AttributedString {
        val sb = AttributedStringBuilder()
        for (m in reAll.findAll(buffer)) {
            val (str, num, pr, com, kw, sym, ws, rest) = m.destructured

            if (str.isNotEmpty()) sb.append(str, D.foreground(GREEN))
            else if (num.isNotEmpty()) sb.append(num, D.foreground(BLUE))
            else if (pr.isNotEmpty()) sb.append(pr, D.foreground(BLUE).bold())
            else if (com.isNotEmpty()) sb.append(com, D.foreground(BLACK))
            else if (kw.isNotEmpty()) sb.append(kw, D.foreground(YELLOW))
            else if (sym.isNotEmpty()) sb.append(
                sym, if (sym in syms) D.bold() else D.underline())
            else if (ws.isNotEmpty()) sb.append(ws)
            else if (rest.isNotEmpty()) sb.append(rest)
        }
        return sb.toAttributedString()
    }

    override fun refresh(reader: LineReader?) {
        super.refresh(reader)
    }

    override fun setErrorPattern(p0: Pattern?) {
        throw Exception("Not yet implemented")
    }

    override fun setErrorIndex(p0: Int) {
        throw Exception("Not yet implemented")
    }
}
