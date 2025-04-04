import java.io.File
import kotlin.system.exitProcess

class Lox() {

    fun runPrompt() {
        while (true) {
            print("> ")
            val line = readLine() ?: break
            run(line)
            hadError = false
            hadRuntimeError = false
        }
    }

    fun runFile(path: String) {
        val file = File(path)
        val contents = file.readText()
        run(contents)
        if (hadError) exitProcess(65)
        if (hadRuntimeError) exitProcess(70)
        println("[Done]")
    }

    fun run(code: String) {
        val parser = Parser(Scanner(code).scanTokens())
        val statements = parser.parse()
        if (hadError) return
        interpreter.interpret(statements)
    }

    companion object {
        val interpreter = Interpreter()
        var hadError = false
        var hadRuntimeError = false

        fun report(line: Int, where: String, message: String) {
            System.err.println("[line ${line}] Error $where: $message")
            hadError = true
        }

        fun loxError(line: Int, message: String) = report(line, "", message)

        fun loxError(token: Token, message: String) {
            if (token.type == TokenType.EOF) {
                report(token.line, "at end", message)
            } else {
                report(token.line, "at '${token.lexeme}'", message)
            }
        }

        fun runtimeError(error: Interpreter.RuntimeError) {
            System.err.println("${error.message}\n[line ${error.token.line}]")
            hadRuntimeError = true
        }
    }
}

