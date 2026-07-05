package com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands;

public record PairDeviceCommand(
        Long userId,
        String pairingCode,
        String alias,
        String room
) {
}
