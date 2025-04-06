package com.joelburton.mothlang.interpreter

import com.joelburton.mothlang.scanner.Token

/** Interface for both built-in and user-defined functions.
 *
 * Function calling will have a list of arguments, but the list may be empty.
 * Items in the arguments may be null or non-null, and can be of any type.
 * (Technically, there are only three types in the language: nil (null),
 * strings, and doubles. Since Kotlin doesn't have union types, it isn't
 * easy to make the arguments check that they're of those types --- but it
 * wouldn't be possible for end-user-code to produce any other types, so this
 * loose checking only really affects Kotlin code.
 */
interface ILoxCallable {
     fun call(interp: Interpreter, arguments: List<Any?>, name: Token): Any?
     val arity: Int
}
