package com.joelburton.mothlang

import com.joelburton.mothlang.Lox.Companion.hadError
import com.joelburton.mothlang.Lox.Companion.hadRuntimeError
import com.joelburton.mothlang.ast.AstExprPrinter
import com.joelburton.mothlang.ast.AstStmtPrinter
import com.joelburton.mothlang.cli.LoxHighlighter
import com.joelburton.mothlang.interpreter.Interpreter
import com.joelburton.mothlang.parser.Parser
import com.joelburton.mothlang.resolver.Resolver
import com.joelburton.mothlang.scanner.Scanner
import com.joelburton.mothlang.scanner.Token
import com.joelburton.mothlang.scanner.TokenType
import com.joelburton.mothlang.scanner.keywordsToTokens
import org.jline.reader.*
import org.jline.reader.LineReader.*
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.DefaultParser.Bracket
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle.DEFAULT
import org.jline.utils.AttributedStyle.GREEN
import org.jline.widget.AutosuggestionWidgets
import java.io.File
import kotlin.system.exitProcess

internal class LoxCompleter(val words: Set<String>) : Completer {
    override fun complete(
        reader: LineReader,
        line: ParsedLine,
        candidates: MutableList<Candidate>
    ) {
        for (word in words) candidates.add(Candidate(word))
        for (keyword in keywordsToTokens.keys) candidates.add(Candidate(keyword))
    }
}


/** Core class for the language.
 *
 * Included static ("companion object") methods for the different components
 * reporting errors (this might want to get moved to a singleton
 * ErrorReporter object or such).
 *
 * Includes methods that run a file or run the REPL.
 */

class Lox(
    val showTokens: Boolean = false,
    val showParse: Boolean = false,
    val dryRun: Boolean = false,
) {

    /** Run REPL for language. */
    fun runPrompt() {
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
            .highlighter(LoxHighlighter())
            .parser(parser)
            .variable(HISTORY_FILE, "$home/.mothlang_history")
            .variable(SECONDARY_PROMPT_PATTERN, prompt2)
            .variable(INDENTATION, 4)
            .option(Option.INSERT_BRACKET, true)
            .terminal(terminal)
            .build()
        AutosuggestionWidgets(reader).enable()

        while (true) {
            val line = reader.readLine(prompt) ?: break
            run(line)
            hadError = false
            hadRuntimeError = false
        }
    }

    /** Run a single file. */
    fun runFile(path: String) {
        val file = File(path)
        val contents = file.readText()
        run(contents)
        if (hadError) exitProcess(65)
        if (hadRuntimeError) exitProcess(70)
    }

    /** Run a single expression. */
    fun runExpr(expr: String) {
        val tokens = Scanner(expr).scanTokens()
        if (showTokens) {
            for (t in tokens) {
                println(t)
            }
        }

        val parser = Parser(tokens)
        val result = parser.parseExpression()
        if (showParse) {
            val printer = AstExprPrinter()
            println(printer.print(result))
        }

        println(interpreter.evalSingle(result))
    }

    /** Run a string. */
    private fun run(code: String) {
        val tokens = Scanner(code).scanTokens()
        if (showTokens) {
            for (t in tokens) {
                println(t)
            }
        }

        val parser = Parser(tokens)
        val statements = parser.parse()
        if (showParse) {
            val printer = AstStmtPrinter()
            for (statement in statements) {
                println(printer.print(statement))
            }
        }
        if (hadError) return

        val resolver = Resolver(interpreter)
        resolver.resolve(statements)
        if (hadError) return

        if (dryRun) return
        interpreter.interpret(statements)
    }

    /** Static stuff. */
    companion object {

        /** Singleton interpreter.
         *
         * For now, this is having all users share an interpreter.
         * However, this isn't safe --- the interpreter has state, like globals
         * and locals. So, multiple callers shouldn't exist for this.
         *
         * This is a bit of a hack for now; the functions above should
         * create their own interpreters. This will require a different way
         * to handle [hadError] and [hadRuntimeError], though, since these are
         * also currently static.
         */
        val interpreter = Interpreter()
        var hadError = false
        var hadRuntimeError = false

        /** Report a non-fatal error. */
        fun report(line: Int, where: String, message: String) {
            System.err.println("[line ${line}] Error $where: $message")
            hadError = true
        }

        /** Report a non-fatal error in scanning or parsing. */
        fun loxError(line: Int, message: String) = report(line, "", message)

        /** Report a non-fatal error in scanning or parsing. */
        fun loxError(token: Token, message: String) {
            if (token.type == TokenType.EOF) {
                report(token.line, "at end", message)
            } else {
                report(token.line, "at '${token.lexeme}'", message)
            }
        }

        /** Runtime language error: prints a message and stop interp.
         *
         * This is [Throwable] and sometimes is thrown by clients; sometimes
         * it isn't, and is used for the side effect of printing a message
         * and setting a flag to stop the interpreter.
         */
        fun runtimeError(error: Interpreter.RuntimeError) {
            System.err.println("${error.message}\n[line ${error.token.line}]")
            hadRuntimeError = true
        }
    }
}
