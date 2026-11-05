package com.electrocorp.electrocorpplatform.energymonitoring.infrastructure.cache;

import com.electrocorp.electrocorpplatform.energymonitoring.application.ports.ConsumptionSummaryCache;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.EnergyDashboardSummaryResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Adaptador Redis del resumen de consumo. Si Redis no responde, registra una advertencia,
 * deja de consultarlo durante un TTL y el endpoint sigue funcionando sin cache.
 */
@Component
@ConditionalOnProperty(name = "electrocorp.energy.summary-cache.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConsumptionSummaryCache implements ConsumptionSummaryCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConsumptionSummaryCache.class);

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final Duration ttl;
    private volatile Instant unavailableUntil = Instant.MIN;

    public RedisConsumptionSummaryCache(
            StringRedisTemplate redisTemplate,
            @Value("${electrocorp.energy.summary-cache.ttl-seconds:30}") long ttlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public Optional<EnergyDashboardSummaryResource> get(Long userId) {
        if (isTemporarilyUnavailable()) {
            return Optional.empty();
        }

        try {
            String json = redisTemplate.opsForValue().get(ConsumptionSummaryCache.keyFor(userId));
            return json == null
                    ? Optional.empty()
                    : Optional.of(jsonMapper.readValue(json, EnergyDashboardSummaryResource.class));
        } catch (RuntimeException exception) {
            markUnavailable("read", exception);
            return Optional.empty();
        }
    }

    @Override
    public void put(Long userId, EnergyDashboardSummaryResource summary) {
        if (isTemporarilyUnavailable()) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(
                    ConsumptionSummaryCache.keyFor(userId),
                    jsonMapper.writeValueAsString(summary),
                    ttl
            );
        } catch (RuntimeException exception) {
            markUnavailable("write", exception);
        }
    }

    @Override
    public void evict(Long userId) {
        if (isTemporarilyUnavailable()) {
            return;
        }

        try {
            redisTemplate.delete(ConsumptionSummaryCache.keyFor(userId));
        } catch (RuntimeException exception) {
            markUnavailable("evict", exception);
        }
    }

    private boolean isTemporarilyUnavailable() {
        return Instant.now().isBefore(unavailableUntil);
    }

    private void markUnavailable(String operation, RuntimeException exception) {
        unavailableUntil = Instant.now().plus(ttl);
        LOGGER.warn("Redis did not respond ({}). The consumption summary will be served without cache for {} s: {}",
                operation, ttl.toSeconds(), exception.getMessage());
    }
}
