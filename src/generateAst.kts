import java.io.File


defineAst(
    "/tmp", "Expr", listOf(
        "Binary       : Expr left, Token operator, Expr right",
        "Grouping     : Expr expression",
        "Literal      : Any? value",
        "Unary        : Token operator, Expr right",
//    "Variable     : Token name",
//    "Assign       : Token name, Expr value",
//    "Logical      : Expr left, Token operator, Expr right",
//    "Call         : Expr callee, Token paren, List<Expr> arguments",
//    "Get          : Expr obj, Token name",
//    "Set          : Expr obj, Token name, Expr value",
    )
)

fun defineAst(
    outputDir: String,
    baseName: String,
    types: List<String>,
) {
    val s = buildString {
        append("abstract class $baseName {\n")
        defineVisitor(this, baseName, types);
        for (type in types) {
            val className = type.substringBefore(':')
            val fields = type.substringAfter(':').trim()
            defineType(this, baseName, className, fields)
        }
        this.append(" abstract <R> fun accept(visitor: Visitor<R>): R\n")
        append("}")
    }
    print(s)
    File("$outputDir/$baseName.kt").writeText(s)
}

fun defineType(
    sb: StringBuilder,
    baseName: String,
    className: String,
    fields: String,
) {
    sb.append("    class $className(")
    for (f in fields.split(",")) {
        val (typ, nam) = f.trim().split(' ')
        sb.append("val $nam: $typ, ")
    }
    sb.append(")  : $baseName {\n")

    sb.append("    override fun <R> accept(visitor: Visitor<R>) = visitor.visit$className(this)\n")

    sb.append("    }\n\n")
}

fun defineVisitor(sb: StringBuilder, baseName: String, types: List<String>) {
    sb.append("interface Visitor<R> {\n")
    for (typ in types) {
        val className = typ.substringBefore(':').trim()
        sb.append("    fun <R> visit$className(node: $className): R\n")
    }
    sb.append("}\n\n")
}