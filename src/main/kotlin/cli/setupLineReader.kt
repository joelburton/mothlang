package com.joelburton.mothlang.cli

import com.joelburton.mothlang.Lox.Companion.interpreter
import org.jline.reader.LineReader
import org.jline.reader.LineReader.*
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.DefaultParser.Bracket
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle.DEFAULT
import org.jline.utils.AttributedStyle.GREEN
import org.jline.widget.AutosuggestionWidgets

fun setupLineReader() : Pair<LineReader, String>  {
    var home = System.getProperty("user.home")
    val parser = DefaultParser()
    val terminal: Terminal = TerminalBuilder.builder().dumb(true).build()

    parser.setEofOnUnclosedBracket(Bracket.CURLY, Bracket.ROUND)
    parser.lineCommentDelims(arrayOf("//"))

    val prompt = AttributedStringBuilder()
        .style(DEFAULT.foreground(GREEN))
        .append("moth> ")
        .style(DEFAULT)
        .toAnsi(terminal)

    val prompt2 = AttributedStringBuilder()
        .style(DEFAULT.foreground(GREEN))
        .append("..... ")
        .style(DEFAULT)
        .toAnsi(terminal)

    val reader = LineReaderBuilder.builder()
        .completer(LoxCompleter(interpreter.globals.values.keys))
        .highlighter(LoxHighlighter(interpreter.globals.values.keys))
        .parser(parser)
        .variable(HISTORY_FILE, "$home/.mothlang_history")
        .variable(SECONDARY_PROMPT_PATTERN, prompt2)
        .variable(INDENTATION, 4)
        .option(Option.INSERT_BRACKET, true)
        .terminal(terminal)
        .build()
    AutosuggestionWidgets(reader).enable()

    return Pair(reader, prompt)
}