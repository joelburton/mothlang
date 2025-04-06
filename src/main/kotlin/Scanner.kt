import TokenType.*

/** Maps language keywords to [TokenType]. */

private val keywordsToTokens = mapOf(
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

/** Scanner that takes string [source] and gathers found tokens in [tokens].
 *
 * The general use is:
 *
 * ```
 *   val scanner = Scanner(myText)
 *   val tokens: List<Token> = scanner.scanTokens()
 * ```
 *
 * If the scanning is interrupted by an unhandled error, you can resume where
 * it left off, since it internally tracks the current pointer in the source.
 * */

class Scanner(private val source: String) {
    /** List of tokens found by the scanner. */
    private val tokens: MutableList<Token> = mutableListOf()

    /** Start in [source] of current scan.
     *
     * For example, while scanning "123" and having found the first 2 digits:
     *                        start-^ ^-current
     */
    private var start = 0

    /** Char position in the [source].
     *
     * This is "one-ahead" of the char this responds to; see how "advance"
     * returns the current pointer position and then advances to the next.
     * */
    private var current = 0

    /** Line number of file currently being read. */
    private var line = 1

    /** Are we and end of source? */
    private val isAtEnd get() = current >= source.length

    /** Return current token and advance to next. */
    private fun advance() = source[current++]

    /** Return current token but don't advance to next. */
    private fun peek() = if (isAtEnd) '\u0000' else source[current]

    /** Return token after the current token, but don't advance. */
    private fun peekNext() =
        if (current + 1 >= source.length) '\u0000' else source[current + 1]

    /** Add token to [tokens].
     *
     * @param type TokenType
     * @param literal Any? TODO
     */
    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = source.slice(start until current)
        tokens.add(Token(type, text, literal, line))
    }

    /** Does the val of current equal [expected]? If true, advance [current]. */
    private fun match(expected: Char): Boolean =
        if (isAtEnd || source[current] != expected) false
        else {
            val c: Char = 'x'
            c.isDigit()
            current += 1; true
        }

    /** Is this a simple digit? (just 0-9, no fancy unicode stuff. */
    private fun isDigit(c: Char) = c in '0'..'9'

    /** Is this a valid identifier start? (A-Z,a-z,_) */
    private fun isIdentStart(c: Char) =
        c in 'a'..'z' || c in 'A'..'Z' || c == '_'

    /** Is this a valid identifier-after-start? A-Z,a-z,_,0-9 */
    private fun isIdent(c: Char) = isIdentStart(c) || isDigit(c)


    /** Consume double-quote-delimited string to [Token].
     *
     * This has no escaping; it is not possible to put " inside a string.
     *
     * Advance line counter is newline found and continue gathering.
     */
    private fun handleString() {
        while (peek() != '"' && !isAtEnd) {
            if (peek() == '\n') line += 1
            advance()
        }

        if (isAtEnd) return error("Unterminated string")

        advance() // skip the ending quote
        val value = source.slice(start + 1 until current - 1)
        addToken(STRING, value)
    }

    /** Consume simple-float and add as [Token].
     *
     * This does not allow for negative floats or stuff like scientific
     * notation.
     *
     * - Allowed: 1 42 42.50
     * - Not allowed: -1 42. 42.50.50 42e9
     */
    private fun handleNumber() {
        while (peek().isDigit()) advance()
        if (peek() == '.' && peekNext().isDigit()) {
            advance()
            while (peek().isDigit()) advance()
        }
        addToken(NUMBER, source.slice(start until current).toDouble())
    }

    /** Consume identifier and add [Token] (keyword or generic IDENTIFIER). */

    private fun handleIdent() {
        while (isIdent(peek())) advance()

        val text = source.slice(start until current)
        val type = keywordsToTokens[text] ?: IDENTIFIER
        addToken(type)
    }

    /** Scan current char and add token.
     *
     * - Many are simple one-char: `+` `-` `*` etc
     * - `/` can be SLASH or start of `//` comment
     *   - comments don't generate a token and are ignored
     * - whitespace is ignored (newlines add to line counter)
     * - well-formed numbers create a token
     * - well-formed identifiers create a token
     * - everything else reports an error
     */
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
                else if (isIdentStart(c)) handleIdent()
                else Lox.loxError(line, "Unexpected character: $c")
            }
        }
    }

    /** Scan tokens, start at [current] until end of file. */
    fun scanTokens(): List<Token> {
        while (!isAtEnd) {
            start = current
            scanToken()
        }

        tokens.add(Token(type = EOF, lexeme = "", literal = null, line = line))
        return tokens
    }
}