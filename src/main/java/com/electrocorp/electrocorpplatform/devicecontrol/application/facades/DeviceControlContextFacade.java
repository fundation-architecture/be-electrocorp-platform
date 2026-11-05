package com.electrocorp.electrocorpplatform.devicecontrol.application.facades;

import java.util.List;
import java.util.Optional;

public interface DeviceControlContextFacade {
    void ensureDeviceBelongsToUser(Long deviceId, Long userId);

    void updateDeviceRoom(Long deviceId, Long userId, String roomName);

    List<DeviceSnapshot> findDevicesByUserId(Long userId);

    Optional<DeviceSnapshot> findDeviceOfUser(Long deviceId, Long userId);

    List<DeviceSnapshot> findActiveDevices();
}
