/** Tokens emitted by the [Scanner].
 *
 * @property type TokenType that represents what kind of token this is.
 * @property lexeme String that is the actual text of a token.
 * @property literal TODO
 * @property line What line of input this token was found on (1-based)
 */

class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val line: Int,
) {
    override fun toString() = "$type '$lexeme' $literal @$line"
}
