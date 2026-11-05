package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.application.facades.DeviceControlContextFacade;
import com.electrocorp.electrocorpplatform.devicecontrol.application.facades.DeviceSnapshot;
import com.electrocorp.electrocorpplatform.energymonitoring.application.ports.ConsumptionSummaryCache;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.commands.CreateEnergyReadingCommand;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyReadingRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.CurrentConsumptionService;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.services.EnergyCostCalculator;
import com.electrocorp.electrocorpplatform.energymonitoring.infrastructure.cache.RedisConsumptionSummaryCache;
import com.electrocorp.electrocorpplatform.energymonitoring.infrastructure.tariff.ConfigurableTariffProvider;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.EnergyDashboardSummaryResource;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.AlertRepository;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-26 (QA-02): el resumen se sirve desde la cache dentro del TTL, una lectura nueva invalida
 * la clave consumption:{id}:summary y, si Redis no responde, el resumen se calcula sin cache.
 */
class ConsumptionSummaryCacheTest {

    private static final Long USER_ID = 7L;

    private final EnergyReadingRepository energyReadingRepository = mock(EnergyReadingRepository.class);
    private final DeviceControlContextFacade deviceControlContextFacade = mock(DeviceControlContextFacade.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final InMemoryConsumptionSummaryCache cache = new InMemoryConsumptionSummaryCache();

    @Test
    void secondQueryWithinTtlDoesNotHitTheDatabase() {
        EnergyMonitoringApplicationService queryService = queryServiceWith(cache);

        EnergyDashboardSummaryResource first = queryService.getDashboardSummary(USER_ID);
        EnergyDashboardSummaryResource second = queryService.getDashboardSummary(USER_ID);

        assertThat(second).isEqualTo(first);
        assertThat(cache.store).containsKey("consumption:7:summary");
        verify(deviceControlContextFacade, times(1)).findDevicesByUserId(USER_ID);
        verify(energyReadingRepository, times(1))
                .findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(any(), any(), any());
    }

    @Test
    void registeringANewReadingInvalidatesTheKey() {
        EnergyMonitoringApplicationService queryService = queryServiceWith(cache);
        queryService.getDashboardSummary(USER_ID);
        DeviceSnapshot device = new DeviceSnapshot(1L, USER_ID, "Refrigeradora", "Cocina", "Linea blanca",
                BigDecimal.valueOf(300), true);
        when(deviceControlContextFacade.findDeviceOfUser(1L, USER_ID)).thenReturn(Optional.of(device));
        when(energyReadingRepository.save(any(EnergyReading.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EnergyReadingIngestionService ingestionService = new EnergyReadingIngestionService(
                energyReadingRepository,
                deviceControlContextFacade,
                mock(DomainEventPublisher.class),
                cache,
                new EnergyCostCalculator(new ConfigurableTariffProvider(new BigDecimal("0.75")))
        );

        ingestionService.createReading(new CreateEnergyReadingCommand(USER_ID, 1L, "Refrigeradora", BigDecimal.valueOf(300), 0));

        assertThat(cache.store).doesNotContainKey("consumption:7:summary");
    }

    @Test
    void summaryStillWorksWhenRedisDoesNotRespond() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Redis is down"));
        RedisConsumptionSummaryCache redisCache = new RedisConsumptionSummaryCache(redisTemplate, 30);
        EnergyMonitoringApplicationService queryService = queryServiceWith(redisCache);

        EnergyDashboardSummaryResource summary = queryService.getDashboardSummary(USER_ID);

        assertThat(summary).isNotNull();
        assertThat(summary.currentWatts()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisAdapterStoresTheSummaryAsJsonWithA30SecondTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        Map<String, String> redis = new HashMap<>();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doAnswer(invocation -> redis.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(valueOperations).set(anyString(), anyString(), eq(Duration.ofSeconds(30)));
        when(valueOperations.get(anyString())).thenAnswer(invocation -> redis.get(invocation.<String>getArgument(0)));
        RedisConsumptionSummaryCache redisCache = new RedisConsumptionSummaryCache(redisTemplate, 30);
        EnergyDashboardSummaryResource summary = queryServiceWith(redisCache).getDashboardSummary(USER_ID);

        Optional<EnergyDashboardSummaryResource> cached = redisCache.get(USER_ID);

        assertThat(redis).containsKey("consumption:7:summary");
        assertThat(cached).contains(summary);
    }

    private EnergyMonitoringApplicationService queryServiceWith(ConsumptionSummaryCache summaryCache) {
        when(deviceControlContextFacade.findDevicesByUserId(USER_ID)).thenReturn(List.of());
        return new EnergyMonitoringApplicationService(
                energyReadingRepository,
                deviceControlContextFacade,
                alertRepository,
                new CurrentConsumptionService(),
                summaryCache,
                new EnergyCostCalculator(new ConfigurableTariffProvider(new BigDecimal("0.75")))
        );
    }

    static class InMemoryConsumptionSummaryCache implements ConsumptionSummaryCache {
        final Map<String, EnergyDashboardSummaryResource> store = new HashMap<>();

        @Override
        public Optional<EnergyDashboardSummaryResource> get(Long userId) {
            return Optional.ofNullable(store.get(ConsumptionSummaryCache.keyFor(userId)));
        }

        @Override
        public void put(Long userId, EnergyDashboardSummaryResource summary) {
            store.put(ConsumptionSummaryCache.keyFor(userId), summary);
        }

        @Override
        public void evict(Long userId) {
            store.remove(ConsumptionSummaryCache.keyFor(userId));
        }
    }
}
