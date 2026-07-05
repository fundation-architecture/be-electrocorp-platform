package com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateDeviceResource(
        @NotBlank String name,
        String room,
        @NotBlank String type,
        @NotNull @Positive BigDecimal powerWatts
) {
}
