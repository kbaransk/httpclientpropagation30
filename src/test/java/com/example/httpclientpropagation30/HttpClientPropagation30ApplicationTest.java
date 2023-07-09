package com.example.httpclientpropagation30;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@ActiveProfiles("test")
@TestInstance(PER_CLASS)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@SpringBootTest(classes = HttpClientPropagation30Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        useMainMethod = SpringBootTest.UseMainMethod.WHEN_AVAILABLE)
@Configuration
@AutoConfigureObservability
class HttpClientPropagation30ApplicationTest {

    @Value("${local.server.port}")
    private Integer port;

    private final WebClient webClient;

    public HttpClientPropagation30ApplicationTest(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Test
    void test() {
        var response = webClient
                 .get()
                 .uri("http://localhost:" + port + "/first/xyz")
                 .exchangeToMono( this::handleResponse);
        StepVerifier.create(response)
                    .expectNext("Remote response: xyz")
                    .verifyComplete();
    }

    private Mono<String> handleResponse(ClientResponse response) {
        if (response.statusCode().is2xxSuccessful())
            return response.bodyToMono(String.class);
        else
            return Mono.error(new RuntimeException());
    }
}