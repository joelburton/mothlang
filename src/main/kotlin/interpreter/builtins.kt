package com.joelburton.mothlang.interpreter

object ClockFn: ILoxCallable {
    override val arity: Int = 0
    override fun call(interp: Interpreter, arguments: List<Any?>): Any? {
        return System.currentTimeMillis() / 1000.0
    }
    override fun toString(): String = "<clock native fn>"
}
