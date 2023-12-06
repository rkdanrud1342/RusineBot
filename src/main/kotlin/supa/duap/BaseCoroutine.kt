package supa.duap

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers

object BaseCoroutine {
    private val exceptionHandler : CoroutineExceptionHandler = CoroutineExceptionHandler { _, e ->

    }

    val ui = Dispatchers.Main + exceptionHandler
    val default = Dispatchers.Default + exceptionHandler
    val io = Dispatchers.IO + exceptionHandler
}

