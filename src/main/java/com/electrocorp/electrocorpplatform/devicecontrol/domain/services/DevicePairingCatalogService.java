package com.electrocorp.electrocorpplatform.devicecontrol.domain.services;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

@Service
public class DevicePairingCatalogService {
    private static final Map<String, DevicePairingTemplate> TEMPLATES = Map.of(
            "EC-PLUG-100", new DevicePairingTemplate("EC-PLUG-100", "Smart Plug ElectroCorp", "PLUG", new BigDecimal("150.00")),
            "EC-LIGHT-200", new DevicePairingTemplate("EC-LIGHT-200", "Luz inteligente ElectroCorp", "LIGHT", new BigDecimal("12.00")),
            "EC-SENSOR-300", new DevicePairingTemplate("EC-SENSOR-300", "Sensor de movimiento ElectroCorp", "SENSOR", new BigDecimal("5.00")),
            "EC-SWITCH-400", new DevicePairingTemplate("EC-SWITCH-400", "Interruptor inteligente ElectroCorp", "SWITCH", new BigDecimal("60.00"))
    );

    public DevicePairingTemplate resolve(String pairingCode) {
        String normalizedCode = normalize(pairingCode);

        if (normalizedCode.isBlank()) {
            throw new IllegalArgumentException("Device pairing code is required.");
        }

        DevicePairingTemplate template = TEMPLATES.get(normalizedCode);

        if (template == null) {
            throw new IllegalArgumentException("Device pairing code is not registered.");
        }

        return template;
    }

    public String normalize(String pairingCode) {
        return pairingCode == null
                ? ""
                : pairingCode.trim().toUpperCase(Locale.ROOT);
    }

    public record DevicePairingTemplate(
            String code,
            String defaultName,
            String type,
            BigDecimal powerWatts
    ) {
    }
}
