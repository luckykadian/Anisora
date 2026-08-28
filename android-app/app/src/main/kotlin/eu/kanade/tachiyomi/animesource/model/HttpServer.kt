package eu.kanade.tachiyomi.animesource.model

open class HttpServer {
    open fun start() {}
    open fun stop() {}
    open val port: Int = 0
}
