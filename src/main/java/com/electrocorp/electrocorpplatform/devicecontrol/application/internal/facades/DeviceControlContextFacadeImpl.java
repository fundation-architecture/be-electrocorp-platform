package com.electrocorp.electrocorpplatform.devicecontrol.application.internal.facades;

import com.electrocorp.electrocorpplatform.devicecontrol.application.facades.DeviceControlContextFacade;
import com.electrocorp.electrocorpplatform.devicecontrol.application.facades.DeviceSnapshot;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.DeviceStatus;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    @Override
    @Transactional(readOnly = true)
    public List<DeviceSnapshot> findDevicesByUserId(Long userId) {
        return deviceRepository.findByUserId(userId).stream()
                .map(DeviceSnapshot::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceSnapshot> findDeviceOfUser(Long deviceId, Long userId) {
        return deviceRepository.findByIdAndUserId(deviceId, userId)
                .map(DeviceSnapshot::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceSnapshot> findActiveDevices() {
        return deviceRepository.findAll().stream()
                .filter(device -> device.getStatus() == DeviceStatus.ON)
                .map(DeviceSnapshot::from)
                .toList();
    }
}
