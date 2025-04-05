fun main(args: Array<String>) =
    Lox().run {
        if (args.isEmpty()) runPrompt() else runFile(args[0])
    }
