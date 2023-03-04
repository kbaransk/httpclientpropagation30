package com.example.httpclientpropagation30;

import io.micrometer.tracing.Baggage;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
class MyController {

    private final WebClient webClient;
    private final Baggage valueToPropagate;

    MyController(WebClient webClient, Baggage valueToPropagate) {
        this.webClient = webClient;
        this.valueToPropagate = valueToPropagate;
    }

    @RequestMapping("/first/{value}")
    Mono<String> first(@PathVariable String value) {
        valueToPropagate.set(value);
        return webClient.get().uri("http://localhost:8080/remoteSystem").exchangeToMono( this::handleResponse);
    }

    private Mono<String> handleResponse(ClientResponse response) {
        if (response.statusCode().is2xxSuccessful())
            return response.bodyToMono(String.class);
        else
            return Mono.error(new RuntimeException());
    }

    @RequestMapping("/remoteSystem")
    Mono<String> remoteSystem() {
        return Mono.just("Some response");
    }
}
