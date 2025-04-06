package com.joelburton.mothlang.interpreter


interface ILoxCallable {
     fun call(interp: Interpreter, arguments: List<Any?>): Any?
     val arity: Int
}

