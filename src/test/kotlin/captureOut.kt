import java.io.ByteArrayOutputStream
import java.io.PrintStream

fun captureStdout(block: () -> Unit): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val originalOut = System.out
    System.setOut(printStream)
    try {
        block()
    } finally {
        System.setOut(originalOut)
        printStream.flush()
        printStream.close()
    }
    return byteArrayOutputStream.toString()
}
fun captureStderr(block: () -> Unit): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val originalErr = System.err
    System.setErr(printStream)
    try {
        block()
    } finally {
        System.setErr(originalErr)
        printStream.flush()
        printStream.close()
    }
    return byteArrayOutputStream.toString()
}
