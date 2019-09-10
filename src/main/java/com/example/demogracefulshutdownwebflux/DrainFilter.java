package com.example.demogracefulshutdownwebflux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DrainFilter implements WebFilter {

    private final Logger log = LoggerFactory.getLogger(DrainFilter.class);

    private final AtomicInteger inFlight = new AtomicInteger(0);

    private final AtomicBoolean inDraining = new AtomicBoolean(false);

    private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory(true);

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
        if (this.inDraining.get()) {
            final ServerHttpResponse response = serverWebExchange.getResponse();
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
            return response.writeWith(Mono.just(this.bufferFactory.wrap("Now Draining...".getBytes())));
        }
        this.inFlight.incrementAndGet();
        return webFilterChain.filter(serverWebExchange)
            .doOnTerminate(inFlight::decrementAndGet);
    }

    @PreDestroy
    void preDestroy() {
        log.info("Start draining. ({} in-flight requests)", this.inFlight);
        this.inDraining.set(true);
        for (int i = 0; i < 5000; i++) {
            int current = this.inFlight.get();
            if (current <= 0) {
                break;
            }
            try {
                log.info("Draining... ({} in-flight requests)", current);
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("Good bye. ({} in-flight requests)", this.inFlight);
    }
}
