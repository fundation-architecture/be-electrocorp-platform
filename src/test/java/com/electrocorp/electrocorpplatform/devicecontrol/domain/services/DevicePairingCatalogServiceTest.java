package com.electrocorp.electrocorpplatform.devicecontrol.domain.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevicePairingCatalogServiceTest {
    private final DevicePairingCatalogService service = new DevicePairingCatalogService();

    @Test
    void resolveShouldNormalizePairingCode() {
        var template = service.resolve(" ec-plug-100 ");

        assertThat(template.code()).isEqualTo("EC-PLUG-100");
        assertThat(template.type()).isEqualTo("PLUG");
        assertThat(template.powerWatts()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void resolveShouldRejectUnknownPairingCode() {
        assertThatThrownBy(() -> service.resolve("EC-UNKNOWN-999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Device pairing code is not registered.");
    }
}
