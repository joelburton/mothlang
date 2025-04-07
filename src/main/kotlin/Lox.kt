package com.joelburton.mothlang

import com.joelburton.mothlang.Lox.Companion.hadError
import com.joelburton.mothlang.Lox.Companion.hadRuntimeError
import com.joelburton.mothlang.ast.AstExprPrinter
import com.joelburton.mothlang.ast.AstStmtPrinter
import com.joelburton.mothlang.cli.setupLineReader
import com.joelburton.mothlang.interpreter.Interpreter
import com.joelburton.mothlang.parser.Parser
import com.joelburton.mothlang.resolver.Resolver
import com.joelburton.mothlang.scanner.Scanner
import com.joelburton.mothlang.scanner.Token
import com.joelburton.mothlang.scanner.TokenType
import org.jline.reader.EndOfFileException
import java.io.File
import kotlin.system.exitProcess


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
        val (reader, prompt) = setupLineReader()

        while (true) {
            val line = try {
                reader.readLine(prompt)
            } catch (_: EndOfFileException) {
                break
            }
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
