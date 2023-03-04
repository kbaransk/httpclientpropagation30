package com.example.httpclientpropagation30;

import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.Tracer;
import io.netty.channel.Channel;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import reactor.netty.ConnectionObserver;
import reactor.netty.NettyPipeline;
import reactor.netty.http.client.HttpClient;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@SpringBootApplication
class HttpClientPropagation30Application {

    public static void main(String[] args) {
        Hooks.enableAutomaticContextPropagation();
        new SpringApplicationBuilder(HttpClientPropagation30Application.class).run(args);
    }
}

@Configuration
class HttpClientPropagation30Configuration {

    private static Map<String, String> createAdditionalContext(Baggage valueToPropagate) {
        var contextMap = new HashMap<String, String>();
        contextMap.put(valueToPropagate.name(), valueToPropagate.makeCurrent().get());
        return contextMap;
    }

    @Bean
    Baggage valueToPropagate(Tracer tracer) {
        return tracer.createBaggage("some_field");
    }

    @Bean
    WebClient webClient(WebClient.Builder builder, HttpClient customHttpClient) {
        return builder.clientConnector(new ReactorClientHttpConnector(customHttpClient)).build();
    }

    @Bean
    HttpClient customHttpClient(Baggage valueToPropagate) {
        return HttpClient.create()
                         .doOnChannelInit(
                                 (ConnectionObserver observer, Channel channel, SocketAddress remoteAddress) -> {
// -----> works here, but it's only called on first request
                                     System.out.println("baggage: " + valueToPropagate.makeCurrent().get());

                                     var pipeline = channel.pipeline();
                                     if (pipeline.get(NettyPipeline.HttpCodec) != null) {
                                         Supplier<Map<String, String>> additionalContext = () -> createAdditionalContext(
                                                 valueToPropagate);

                                         var httpClientLoggingHandler = new MyNettyChannelHandler(additionalContext);
                                         pipeline.addAfter(NettyPipeline.HttpCodec, "MyHandler",
                                                           httpClientLoggingHandler);
                                     }
                                 });
    }
}