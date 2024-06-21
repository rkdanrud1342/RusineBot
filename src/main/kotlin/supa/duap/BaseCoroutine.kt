package supa.duap

import io.ktor.util.logging.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import org.slf4j.LoggerFactory

object BaseCoroutine {
    private val logger = LoggerFactory.getLogger(BaseCoroutine::class.java)
    private val exceptionHandler : CoroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        logger.error(e)
    }

    val ui = Dispatchers.Main + exceptionHandler
    val default = Dispatchers.Default + exceptionHandler
    val io = Dispatchers.IO + exceptionHandler
}

