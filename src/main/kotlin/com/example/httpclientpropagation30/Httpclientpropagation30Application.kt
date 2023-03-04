package com.example.httpclientpropagation30

import io.micrometer.tracing.Baggage
import io.micrometer.tracing.Tracer
import io.netty.channel.*
import io.netty.handler.codec.http.*
import io.netty.util.internal.logging.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.netty.*
import reactor.netty.http.client.HttpClient
import java.net.SocketAddress
import java.util.*
import java.util.function.Supplier

fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()
    runApplication<Httpclientpropagation30Application>(*args)
}

@RestController
class MyController(private val webClient: WebClient, private val valueToPropagate: Baggage) {

    @RequestMapping("/first/{value}")
    fun first(@PathVariable value: String): Mono<String> {
        valueToPropagate.set(value).get()
        return webClient.get().uri("http://localhost:8080/remoteSystem").exchangeToMono { handleResponse(it) }
    }

    private fun handleResponse(response: ClientResponse) = when {
        response.statusCode().is2xxSuccessful -> response.bodyToMono(String::class.java)
        else -> Mono.error(RuntimeException())
    }

    @RequestMapping("/remoteSystem")
    fun remoteSystem(): Mono<String> = Mono.just("Some response")
}

@SpringBootApplication
@Configuration
class Httpclientpropagation30Application {

    @Bean
    fun valueToPropagate(tracer: Tracer) = tracer.createBaggage("some_field")!!

    @Bean
    fun customHttpClient(valueToPropagate: Baggage): HttpClient {
        return HttpClient.create()
                .doOnChannelInit { observer: ConnectionObserver, channel: Channel, remoteAddress: SocketAddress? ->
// -----> works here, but it's only called on first request
                    println("baggage: ${valueToPropagate.makeCurrent().get()}")

                    val pipeline = channel.pipeline()
                    if (pipeline[NettyPipeline.HttpCodec] != null) {
                        val additionalContext = Supplier<Map<String, String?>> {
                            mapOf(valueToPropagate.name() to valueToPropagate.makeCurrent().get())
                        }
                        val httpClientLoggingHandler = MyHandler(additionalContext)
                        pipeline.addAfter(NettyPipeline.HttpCodec, "MyHandler", httpClientLoggingHandler)
                    }
                }
    }

    @Bean
    fun webClient(builder: WebClient.Builder, customHttpClient: HttpClient) =
            builder.clientConnector(ReactorClientHttpConnector(customHttpClient)).build()
}

class MyHandler(private val additionalContext: Supplier<Map<String, String?>>) : ChannelDuplexHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
// -----> here value is null
        println(additionalContext.get().toString())
        ctx.fireChannelRead(msg)
    }

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
// -----> here value is null
        println(additionalContext.get().toString())
        ctx.write(msg, promise)
    }
}
