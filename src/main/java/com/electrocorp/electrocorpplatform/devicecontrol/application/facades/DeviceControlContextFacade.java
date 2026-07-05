package com.electrocorp.electrocorpplatform.devicecontrol.application.facades;

public interface DeviceControlContextFacade {
    void ensureDeviceBelongsToUser(Long deviceId, Long userId);

    void updateDeviceRoom(Long deviceId, Long userId, String roomName);
}
