package com.joelburton.mothlang.cli

import com.joelburton.mothlang.Lox

fun main(args: Array<String>) =
    Lox().run {
        if (args.isEmpty()) runPrompt() else runFile(args[0])
    }
