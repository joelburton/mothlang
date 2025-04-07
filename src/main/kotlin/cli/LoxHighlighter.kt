package com.joelburton.mothlang.cli

import com.joelburton.mothlang.Lox
import com.joelburton.mothlang.scanner.keywordsToTokens
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle.BLACK
import org.jline.utils.AttributedStyle.BLUE
import org.jline.utils.AttributedStyle.DEFAULT
import org.jline.utils.AttributedStyle.GREEN
import org.jline.utils.AttributedStyle.YELLOW
import java.util.regex.Pattern

internal class LoxHighlighter : Highlighter {
    var str = "(\".*?\")"
    val num = "([-+]?[0-9]*\\.?[0-9]+)"
    val primitive = "(true|false|nil)"
    val comment = "(//.*)"
    val keywords = "(${keywordsToTokens.keys.joinToString("|")})"
    val symbol = "(\\w+)"
    val ws = "(\\s+)"
    val rest = "(\\S+)"
    val reAll =
        "$str|$num|$primitive|$comment|$keywords|$symbol|$ws|$rest".toRegex()

    override fun highlight(
        reader: LineReader, buffer: String
    ): AttributedString {
        val sb = AttributedStringBuilder()
        for (m in reAll.findAll(buffer)) {
            if (m.groups[1] != null)
                sb.append(m.groups[1]!!.value, DEFAULT.foreground(GREEN))
            else if (m.groups[2] != null)
                sb.append(m.groups[2]!!.value, DEFAULT.foreground(BLUE))
            else if (m.groups[3] != null)
                sb.append(m.groups[3]!!.value, DEFAULT.foreground(BLUE).bold())
            else if (m.groups[4] != null)
                sb.append(m.groups[4]!!.value, DEFAULT.foreground(BLACK))
            else if (m.groups[5] != null) {
                sb.append(m.groups[5]!!.value, DEFAULT.foreground(YELLOW))
            } else if (m.groups[6] != null) {
                val v = m.groups[6]!!.value
                if (v in Lox.interpreter.globals.values.keys) {
                    sb.append(v, DEFAULT.bold())
                } else {
                    sb.append(v, DEFAULT.underline())
                }
            }
            else if (m.groups[7] != null)
                sb.append(m.groups[7]!!.value, DEFAULT.foreground(YELLOW))
            else sb.append(m.groups[8]!!.value)
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
