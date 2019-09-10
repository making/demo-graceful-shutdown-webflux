package com.example.demogracefulshutdownwebflux;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
public class HelloController {

    @GetMapping("/")
    Mono<String> hello() {
        return Mono.just("Hello!").delayElement(Duration.ofSeconds(5));
    }
}
