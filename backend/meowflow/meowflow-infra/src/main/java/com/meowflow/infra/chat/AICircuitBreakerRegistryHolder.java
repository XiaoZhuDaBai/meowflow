package com.meowflow.infra.chat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Holder that exposes the AI circuit breaker registry and provides
 * a per-provider circuit breaker instance. Allows {@link OpenAIChatClient}
 * and {@link ClaudeChatClient} to participate in the same circuit breaker
 * without a hard dependency on a specific bean name.
 *
 * <p>The {@code aiCircuitBreakerRegistry} bean is now defined in
 * {@code meowflow-common}'s {@code UnifiedCircuitBreakerConfig},
 * unifying circuit breaker configuration across modules.</p>
 */
@Slf4j
@Component
public class AICircuitBreakerRegistryHolder {

    private final CircuitBreakerRegistry registry;

    @Autowired
    public AICircuitBreakerRegistryHolder(@Autowired(required = false) @Qualifier("aiCircuitBreakerRegistry") CircuitBreakerRegistry registry) {
        this.registry = registry;
        if (registry != null) {
            log.info("AICircuitBreakerRegistryHolder initialized with registry");
        } else {
            log.warn("AICircuitBreakerRegistryHolder initialized without registry - circuit breaker disabled");
        }
    }

    public CircuitBreaker forProvider(String provider) {
        if (registry == null) {
            return null;
        }
        return registry.circuitBreaker(provider);
    }

    public CircuitBreakerRegistry getRegistry() {
        return registry;
    }
}
