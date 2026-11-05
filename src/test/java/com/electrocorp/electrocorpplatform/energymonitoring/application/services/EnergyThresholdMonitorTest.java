package com.electrocorp.electrocorpplatform.energymonitoring.application.services;

import com.electrocorp.electrocorpplatform.energymonitoring.domain.factories.EnergyReadingFactory;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.events.EnergyThresholdExceededEvent;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.specifications.HighConsumptionSpecification;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * T-35: una lectura sobre el limite configurado publica el evento una sola vez con el usuario y el dispositivo.
 */
class EnergyThresholdMonitorTest {

    private final HighConsumptionSpecification specification = new HighConsumptionSpecification(new BigDecimal("1000"));
    private final EnergyReadingFactory factory = new EnergyReadingFactory(specification);
    private final DomainEventPublisher publisher = mock(DomainEventPublisher.class);
    private final EnergyThresholdMonitor monitor = new EnergyThresholdMonitor(publisher, specification);

    @Test
    void readingAboveTheConfiguredLimitPublishesTheEventOnceWithUserAndDevice() {
        EnergyReading reading = reading(new BigDecimal("1250"));

        boolean published = monitor.publishIfExceeded(reading);

        ArgumentCaptor<EnergyThresholdExceededEvent> captor = ArgumentCaptor.forClass(EnergyThresholdExceededEvent.class);
        verify(publisher, times(1)).publish(captor.capture());
        EnergyThresholdExceededEvent event = captor.getValue();
        assertThat(published).isTrue();
        assertThat(event.userId()).isEqualTo(7L);
        assertThat(event.deviceId()).isEqualTo(3L);
        assertThat(event.deviceName()).isEqualTo("Aire acondicionado");
        assertThat(event.watts()).isEqualByComparingTo("1250");
        assertThat(event.thresholdWatts()).isEqualByComparingTo("1000");
    }

    @Test
    void readingBelowTheConfiguredLimitDoesNotPublishTheEvent() {
        boolean published = monitor.publishIfExceeded(reading(new BigDecimal("640")));

        assertThat(published).isFalse();
        verify(publisher, never()).publish(any());
    }

    private EnergyReading reading(BigDecimal watts) {
        return factory.create(7L, 3L, "Aire acondicionado", watts, BigDecimal.ZERO, BigDecimal.ZERO, 0, LocalDateTime.now());
    }
}
