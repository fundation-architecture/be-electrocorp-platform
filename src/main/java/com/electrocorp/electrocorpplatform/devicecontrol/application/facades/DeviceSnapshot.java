package com.electrocorp.electrocorpplatform.devicecontrol.application.facades;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.DeviceStatus;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Device;

import java.math.BigDecimal;

/**
 * Vista de solo lectura de un dispositivo que Device Control expone a otros bounded contexts
 * mediante {@link DeviceControlContextFacade}. Evita que otros contextos dependan del agregado Device.
 */
public record DeviceSnapshot(
        Long id,
        Long userId,
        String name,
        String room,
        String type,
        BigDecimal powerWatts,
        boolean active
) {
    public static DeviceSnapshot from(Device device) {
        return new DeviceSnapshot(
                device.getId(),
                device.getUserId(),
                device.getName(),
                device.getRoom(),
                device.getType(),
                device.getPowerWatts(),
                device.getStatus() == DeviceStatus.ON
        );
    }
}
