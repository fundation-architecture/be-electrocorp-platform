package com.electrocorp.electrocorpplatform.devicecontrol.application.internal.facades;

import com.electrocorp.electrocorpplatform.devicecontrol.application.facades.DeviceControlContextFacade;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceControlContextFacadeImpl implements DeviceControlContextFacade {

    private final DeviceRepository deviceRepository;

    @Override
    @Transactional(readOnly = true)
    public void ensureDeviceBelongsToUser(Long deviceId, Long userId) {
        deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found."));
    }

    @Override
    @Transactional
    public void updateDeviceRoom(Long deviceId, Long userId, String roomName) {
        deviceRepository.findByIdAndUserId(deviceId, userId)
                .ifPresent(device -> {
                    device.setRoom(roomName == null ? "" : roomName.trim());
                    deviceRepository.save(device);
                });
    }
}
