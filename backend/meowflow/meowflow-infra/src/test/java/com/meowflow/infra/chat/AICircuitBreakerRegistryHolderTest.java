package com.meowflow.infra.chat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AICircuitBreakerRegistryHolder Tests")
class AICircuitBreakerRegistryHolderTest {

    @Mock
    private CircuitBreakerRegistry registry;

    @Mock
    private CircuitBreaker breaker;

    private AICircuitBreakerRegistryHolder holder;

    @BeforeEach
    void setUp() {
        holder = new AICircuitBreakerRegistryHolder(registry);
    }

    @Test
    @DisplayName("forProvider returns circuit breaker from registry")
    void forProvider_validProvider_returnsBreaker() {
        when(registry.circuitBreaker("openai")).thenReturn(breaker);

        CircuitBreaker result = holder.forProvider("openai");

        assertThat(result).isSameAs(breaker);
    }

    @Test
    @DisplayName("forProvider returns null when registry is null")
    void forProvider_nullRegistry_returnsNull() {
        AICircuitBreakerRegistryHolder holderNoRegistry = new AICircuitBreakerRegistryHolder(null);

        assertThat(holderNoRegistry.forProvider("openai")).isNull();
    }

    @Test
    @DisplayName("getRegistry returns the underlying registry")
    void getRegistry_returnsRegistry() {
        assertThat(holder.getRegistry()).isSameAs(registry);
    }

    @Test
    @DisplayName("getRegistry returns null when registry was null")
    void getRegistry_nullRegistry_returnsNull() {
        AICircuitBreakerRegistryHolder holderNoRegistry = new AICircuitBreakerRegistryHolder(null);
        assertThat(holderNoRegistry.getRegistry()).isNull();
    }
}
