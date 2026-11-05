package com.electrocorp.electrocorpplatform.energymonitoring.domain.model.events;

import com.electrocorp.electrocorpplatform.shared.domain.model.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una lectura supero el limite de consumo configurado. Lleva el usuario y el dispositivo involucrado (US-20).
 */
public record EnergyThresholdExceededEvent(
        Long userId,
        Long deviceId,
        String deviceName,
        BigDecimal watts,
        BigDecimal thresholdWatts,
        LocalDateTime occurredOn
) implements DomainEvent {
    public EnergyThresholdExceededEvent(Long userId, Long deviceId, String deviceName, BigDecimal watts, BigDecimal thresholdWatts) {
        this(userId, deviceId, deviceName, watts, thresholdWatts, LocalDateTime.now());
    }
}
