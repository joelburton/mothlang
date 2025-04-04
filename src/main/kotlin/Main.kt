fun main(args: Array<String>) {
    val lox = Lox()

    if (args.isEmpty())
        lox.runPrompt()
    else
        lox.runFile(args[0])
}
