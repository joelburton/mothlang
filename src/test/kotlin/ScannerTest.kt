import com.joelburton.mothlang.scanner.Scanner
import com.joelburton.mothlang.scanner.Token
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*

class ScannerTest {
    @Test
    fun `scans simple input`() {
        val source = "1 + 2"
        val tokens = Scanner(source).scanTokens()
        assertEquals(4, tokens.size)

        val text = tokens.joinToString("\n") { it.toString() }
        assertEquals(
            """
NUMBER '1' 1.0 @1
PLUS '+' null @1
NUMBER '2' 2.0 @1
EOF '' null @1
        """.trimIndent(), text
        )
    }

    @Test
    fun `handles line comments`() {
        val source = "1 // nope \n 2 // also nope \n "
        val tokens = Scanner(source).scanTokens()
        assertEquals(3, tokens.size)

        val text = tokens.joinToString("\n") { it.toString() }
        assertEquals(
            """
NUMBER '1' 1.0 @1
NUMBER '2' 2.0 @2
EOF '' null @3
        """.trimIndent(), text
        )
    }

    @Test
    fun `handles complex statements`() {
        val source = """
            for (var i = 0; i < 10; i = i + 1) { print i; }
        """.trimIndent()
        val tokens = Scanner(source).scanTokens()
        assertEquals(23, tokens.size)
        val text = tokens.joinToString("\n") { it.toString() }
        assertEquals(
            """
FOR 'for' null @1
LEFT_PAREN '(' null @1
VAR 'var' null @1
IDENTIFIER 'i' null @1
EQUAL '=' null @1
NUMBER '0' 0.0 @1
SEMICOLON ';' null @1
IDENTIFIER 'i' null @1
LESS '<' null @1
NUMBER '10' 10.0 @1
SEMICOLON ';' null @1
IDENTIFIER 'i' null @1
EQUAL '=' null @1
IDENTIFIER 'i' null @1
PLUS '+' null @1
NUMBER '1' 1.0 @1
RIGHT_PAREN ')' null @1
LEFT_BRACE '{' null @1
PRINT 'print' null @1
IDENTIFIER 'i' null @1
SEMICOLON ';' null @1
RIGHT_BRACE '}' null @1
EOF '' null @1
        """.trimIndent(), text
        )
    }

    @Test
    fun `handles invalid chars`() {
        val source = "1 + #!"
        var tokens: List<Token> = listOf()
        val out = captureStderr {
            tokens = Scanner(source).scanTokens()
        }
        assertEquals(4, tokens.size)

        val text = tokens.joinToString("\n") { it.toString() }
        assertEquals(
            """
NUMBER '1' 1.0 @1
PLUS '+' null @1
BANG '!' null @1
EOF '' null @1
        """.trimIndent(), text
        )
        assertEquals("[line 1] Error : Unexpected character: #\n", out)
    }
}