package com.example.httpclientpropagation30;

import brave.baggage.BaggageField;
import brave.internal.baggage.ExtraBaggageContext;
import io.netty.channel.Channel;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.brave.bridge.BraveTraceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.ConnectionObserver;
import reactor.netty.NettyPipeline;
import reactor.netty.http.client.HttpClient;

import java.net.SocketAddress;
import java.util.*;
import java.util.function.Supplier;

@SpringBootApplication
class HttpClientPropagation30Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(HttpClientPropagation30Application.class).run(args);
    }
}

@Configuration
class HttpClientPropagation30Configuration {

    @Bean
    BaggageField valueToPropagate() {
        return BaggageField.create("some_field");
    }

    @Bean
    WebClient webClient(WebClient.Builder builder, HttpClient customHttpClient) {
        return builder.clientConnector(new ReactorClientHttpConnector(customHttpClient)).build();
    }

    @Bean
    HttpClient customHttpClient(BaggageField valueToPropagate) {
        return HttpClient.create()
                         .doOnChannelInit(
                                 (ConnectionObserver observer, Channel channel, SocketAddress remoteAddress) -> {
// -----> works here, but it's only called on first request
                                     System.out.println("baggage: " + valueToPropagate.getValue());

                                     var pipeline = channel.pipeline();
                                     if (pipeline.get(NettyPipeline.HttpCodec) != null) {
                                         Supplier<Map<String, String>> additionalContext = () -> createAdditionalContext(observer);

                                         var httpClientLoggingHandler = new MyNettyChannelHandler(additionalContext);
                                         pipeline.addAfter(NettyPipeline.HttpCodec, "MyHandler",
                                                           httpClientLoggingHandler);
                                     }
                                 });
    }

    private static Map<String, String> createAdditionalContext(ConnectionObserver observer) {
        var context = observer.currentContext();
        return context.<TraceContext>getOrEmpty(TraceContext.class)
                      .filter(BraveTraceContext.class::isInstance)
                      .map(traceContext -> ExtraBaggageContext.getAllValues(BraveTraceContext.toBrave(traceContext)))
                      .map(Map::copyOf)
                      .orElse(Collections.emptyMap());
    }
}