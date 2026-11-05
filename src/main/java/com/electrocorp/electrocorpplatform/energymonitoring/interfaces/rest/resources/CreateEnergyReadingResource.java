package com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateEnergyReadingResource(
        @NotNull Long deviceId,
        @NotBlank String deviceName,
        @NotNull BigDecimal watts,
        @Min(0) @Max(3600) Integer sampleSeconds
) {
}
