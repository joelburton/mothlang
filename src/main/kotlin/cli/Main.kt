package com.joelburton.mothlang.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.joelburton.mothlang.Lox
import com.joelburton.mothlang.cli.MothCli.Companion.lox

/** Main class: logic for command-line options & proceeds. */

class MothCli : CliktCommand("moth") {
    val tokens by option("-t", "--tokens").flag().help("Show tokens")
    val parse by option("-p", "--parse").flag().help("Show parse results")
    val dryRun by option("-n", "--dry-run").flag().help("Don't run code")

    override fun help(context: Context) =
        "A programming language for moths and cats alike."
    override fun run() {
        lox = Lox(showTokens = tokens, showParse = parse, dryRun = dryRun)
    }

    companion object {
        lateinit var lox: Lox
    }
}

class Run : CliktCommand() {
    val filePath by argument()
    override fun help(context: Context) = "Run .moth file"
    override fun run() = lox.runFile(filePath)
}

class Repl : CliktCommand() {
    override fun help(context: Context) = "Interactive REPL shell"
    override fun run() = lox.runPrompt()
}

class Eval : CliktCommand() {
    val expression by option("-e", "--expression").prompt()
    override fun help(context: Context) = "Execute single Moth expression"
    override fun run() = lox.runExpr(expression)
}

fun main(args: Array<String>) =
    MothCli()
        .subcommands(Run(), Repl(), Eval())
        .versionOption("0.4")
        .main(args)
