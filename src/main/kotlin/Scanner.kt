import TokenType.*

private val keywords = mapOf(
    "and" to AND,
    "class" to CLASS,
    "else" to ELSE,
    "false" to FALSE,
    "for" to FOR,
    "fun" to FUN,
    "if" to IF,
    "nil" to NIL,
    "or" to OR,
    "print" to PRINT,
    "return" to RETURN,
    "super" to SUPER,
    "this" to THIS,
    "true" to TRUE,
    "var" to VAR,
    "while" to WHILE,
)

class Scanner(private val source: String) {
    private val tokens: MutableList<Token> = mutableListOf()
    private var start = 0
    private var current = 0
    private var line = 1

    private val isAtEnd get() = current >= source.length

    private fun advance() = source[current++]

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = source.slice(start until current)
        tokens.add(Token(type, text, literal, line))
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd) return false
        if (source[current] != expected) return false
        current += 1
        return true
    }

    private fun peek() = if (isAtEnd) '\u0000' else source[current]

    private fun peekNext() =
        if (current + 1 >= source.length) '\u0000' else source[current + 1]

    private fun isDigit(c: Char) = c in '0'..'9'

    private fun isAlpha(c: Char) =
        c in 'a'..'z' || c in 'A'..'Z' || c == '_'

    private fun isAlphaNumeric(c: Char) = isAlpha(c) || isDigit(c)

    private fun handleString() {
        while (peek() != '"' && !isAtEnd) {
            if (peek() == '\n') line += 1
            advance()
        }

        if (isAtEnd) return error("Unterminated string")

        advance()
        val value = source.slice(start + 1 until current - 1)
        addToken(STRING, value)
    }

    private fun handleNumber() {
        while (peek().isDigit()) advance()
        if (peek() == '.' && peekNext().isDigit()) {
            advance()
            while (peek().isDigit()) advance()
        }
        addToken(NUMBER, source.slice(start until current).toDouble())
    }

    private fun handleIdentifier() {
        while (isAlphaNumeric(peek())) advance()

        val text = source.slice(start until current)
        val type = keywords[text] ?: IDENTIFIER
        addToken(type)
    }

    private fun scanToken() {
        val c = advance()
        when (c) {
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ',' -> addToken(COMMA)
            '.' -> addToken(DOT)
            '-' -> addToken(MINUS)
            '+' -> addToken(PLUS)
            ';' -> addToken(SEMICOLON)
            '*' -> addToken(STAR)
            '!' -> addToken(if (match('=')) BANG_EQUAL else BANG)
            '=' -> addToken(if (match('=')) EQUAL_EQUAL else EQUAL)
            '<' -> addToken(if (match('=')) LESS_EQUAL else LESS)
            '>' -> addToken(if (match('=')) GREATER_EQUAL else GREATER)
            '/' -> {
                if (match('/')) while (peek() != '\n' && !isAtEnd) advance()
                else addToken(SLASH)
            }
            '"' -> handleString()
            ' ', '\r', '\t' -> {}
            '\n' -> line += 1

            else -> {
                if (isDigit(c)) handleNumber()
                else if (isAlpha(c)) handleIdentifier()
                else Lox.loxError(line, "Unexpected character: $c")
            }
        }
    }

    fun scanTokens(): MutableList<Token> {
        while (!isAtEnd) {
            start = current
            scanToken()
        }

        tokens.add(Token(EOF, "", null, line))
        return tokens
    }
}