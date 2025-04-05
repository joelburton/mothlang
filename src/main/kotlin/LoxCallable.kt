 interface ILoxCallable {
     fun call(interp: Interpreter, arguments: List<Any?>): Any?
     val arity: Int
}

