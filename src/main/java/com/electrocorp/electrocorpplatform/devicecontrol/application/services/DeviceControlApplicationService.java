package com.electrocorp.electrocorpplatform.devicecontrol.application.services;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.*;
import com.electrocorp.electrocorpplatform.devicecontrol.application.results.DeviceGroupDetails;
import com.electrocorp.electrocorpplatform.devicecontrol.application.results.OperationModeActivationResult;
import com.electrocorp.electrocorpplatform.devicecontrol.application.results.OperationModePreviewResult;
import com.electrocorp.electrocorpplatform.devicecontrol.application.results.RoutineDetails;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Device;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.DeviceGroup;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.OperationMode;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.entities.OperationModeRoutine;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.entities.DeviceGroupDevice;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.DeviceStatus;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.OperationModeStatus;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineAction;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineRepeatType;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Routine;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.events.DeviceCreatedEvent;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.events.OperationModeActivatedEvent;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.DeviceGroupDeviceRepository;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.DeviceGroupRepository;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.DeviceRepository;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.OperationModeRepository;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.RoutineRepository;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineTargetType;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.services.DevicePairingCatalogService;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.services.DevicePolicyService;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.services.RoutineSchedulePolicyService;
import com.electrocorp.electrocorpplatform.energymonitoring.application.services.EnergyReadingRecorderService;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.AlertRuleProfileRepository;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.NotificationPreferenceRepository;
import com.electrocorp.electrocorpplatform.reporting.domain.repositories.EnergyGoalRepository;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.DeviceAssignment;
import com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.Location;
import com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.Room;
import com.electrocorp.electrocorpplatform.workplace.domain.repositories.DeviceAssignmentRepository;
import com.electrocorp.electrocorpplatform.workplace.domain.repositories.LocationRepository;
import com.electrocorp.electrocorpplatform.workplace.domain.repositories.RoomRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeviceControlApplicationService {

    private final EnergyReadingRecorderService energyReadingRecorderService;
    private final DeviceRepository deviceRepository;
    private final RoutineRepository routineRepository;
    private final OperationModeRepository operationModeRepository;
    private final DeviceGroupRepository deviceGroupRepository;
    private final DeviceGroupDeviceRepository deviceGroupDeviceRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final LocationRepository locationRepository;
    private final RoomRepository roomRepository;
    private final EnergyGoalRepository energyGoalRepository;
    private final AlertRuleProfileRepository alertRuleProfileRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final DevicePairingCatalogService devicePairingCatalogService;
    private final DevicePolicyService devicePolicyService;
    private final RoutineSchedulePolicyService routineSchedulePolicyService;

    @Transactional(readOnly = true)
    public List<Device> getDevices(Long userId) {
        return deviceRepository.findByUserId(userId)
                .stream()
                .filter(device -> !device.isRemoved())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OperationMode> getOperationModes(Long userId) {
        return operationModeRepository.findByUserIdAndStatusNot(userId, OperationModeStatus.ARCHIVED);
    }

    @Transactional
    public OperationMode createOperationMode(CreateOperationModeCommand command) {
        if (command.userId() == null) {
            throw new IllegalArgumentException("User id is required.");
        }
        if (command.locationId() == null) {
            throw new IllegalArgumentException("Operation mode location is required.");
        }
        if (command.name() == null || command.name().trim().isEmpty()) {
            throw new IllegalArgumentException("Operation mode name is required.");
        }

        ensureLocationBelongsToUser(command.locationId(), command.userId());
        Long scopedRoomId = resolveModeRoomScope(command.locationId(), command.roomIds());
        validateGroupIds(command.userId(), command.groupIds());
        validateDeviceIds(command.userId(), mergeIds(command.deviceIds(), command.turnOnDeviceIds(), command.turnOffDeviceIds(), command.keepOnDeviceIds()));
        validateRoutineIds(command.userId(), mergeIds(command.routineIds(), command.routinesToEnableIds(), command.routinesToDisableIds()));
        validateGoalIds(command.userId(), command.goalIds());
        validateRuleProfile(command.userId(), command.ruleProfileId());
        validatePreference(command.userId(), command.preferenceId());
        validateDeviceIdsInLocation(command.userId(), command.locationId(), mergeIds(command.deviceIds(), command.turnOnDeviceIds(), command.turnOffDeviceIds(), command.keepOnDeviceIds()));
        validateGroupIdsInLocation(command.userId(), command.locationId(), command.groupIds());
        validateRoutineIdsInLocation(command.userId(), command.locationId(), mergeIds(command.routineIds(), command.routinesToEnableIds(), command.routinesToDisableIds()));
        validateGoalIdsInLocation(command.userId(), command.locationId(), command.goalIds());
        validateModeRoomScope(
                command.userId(),
                command.locationId(),
                scopedRoomId,
                command.groupIds(),
                mergeIds(command.deviceIds(), command.turnOnDeviceIds(), command.turnOffDeviceIds(), command.keepOnDeviceIds()),
                mergeIds(command.routineIds(), command.routinesToEnableIds(), command.routinesToDisableIds()),
                command.goalIds()
        );
        validateModeInternalRoutines(command.userId(), command.locationId(), scopedRoomId, command.internalRoutines());

        OperationMode mode = new OperationMode();
        mode.setUserId(command.userId());
        mode.setLocationId(command.locationId());
        mode.setName(command.name().trim());
        mode.setDescription(command.description() == null ? "" : command.description().trim());
        mode.setStatus(OperationModeStatus.DRAFT);
        mode.setRoomIds(toCsv(command.roomIds()));
        mode.setGroupIds(toCsv(command.groupIds()));
        mode.setDeviceIds(toCsv(command.deviceIds()));
        mode.setTurnOnDeviceIds(toCsv(command.turnOnDeviceIds()));
        mode.setTurnOffDeviceIds(toCsv(command.turnOffDeviceIds()));
        mode.setKeepOnDeviceIds(toCsv(command.keepOnDeviceIds()));
        mode.setRoutineIds(toCsv(command.routineIds()));
        mode.setRoutinesToEnableIds(toCsv(command.routinesToEnableIds()));
        mode.setRoutinesToDisableIds(toCsv(command.routinesToDisableIds()));
        mode.setGoalIds(toCsv(command.goalIds()));
        mode.setInternalRoutines(toModeRoutines(command.internalRoutines()));
        mode.setAllDay(command.allDay() == null || command.allDay());
        mode.setStartsAt(Boolean.FALSE.equals(mode.getAllDay()) ? normalizeModeTime(command.startsAt(), "00:00") : "00:00");
        mode.setEndsAt(Boolean.FALSE.equals(mode.getAllDay()) ? normalizeModeTime(command.endsAt(), "23:59") : "23:59");
        mode.setRuleProfileId(command.ruleProfileId());
        mode.setPreferenceId(command.preferenceId());
        mode.setApplyRuleProfile(command.applyRuleProfile() == null || command.applyRuleProfile());
        mode.setApplyNotificationPreference(command.applyNotificationPreference() == null || command.applyNotificationPreference());
        mode.setApplyRoutines(command.applyRoutines() == null || command.applyRoutines());
        mode.setPreserveCriticalSound(command.preserveCriticalSound() == null || command.preserveCriticalSound());
        return operationModeRepository.save(mode);
    }

    @Transactional(readOnly = true)
    public OperationModePreviewResult previewOperationMode(Long userId, Long modeId) {
        OperationMode mode = getModeForUser(userId, modeId);
        return buildOperationModePreview(mode);
    }

    @Transactional
    public OperationModeActivationResult activateOperationMode(Long userId, Long modeId) {
        OperationMode mode = getModeForUser(userId, modeId);
        if (mode.isArchived()) {
            throw new IllegalArgumentException("Archived operation modes cannot be activated.");
        }

        lockModesInLocation(userId, mode.getLocationId());

        OperationModePreviewResult preview = buildOperationModePreview(mode);
        List<Long> turnedOn = new java.util.ArrayList<>();
        List<Long> turnedOff = new java.util.ArrayList<>();

        applyModeInternalRoutines(mode, turnedOn, turnedOff);

        for (Long deviceId : parseIds(mode.getTurnOnDeviceIds())) {
            deviceRepository.findByIdAndUserId(deviceId, userId).ifPresent(device -> {
                if (device.canReceiveOperationalChanges()) {
                    DeviceStatus previousStatus = device.getStatus();
                    device.turnOn();
                    Device savedDevice = deviceRepository.save(device);
                    energyReadingRecorderService.recordStatusTransition(savedDevice, previousStatus);
                    turnedOn.add(savedDevice.getId());
                }
            });
        }

        for (Long deviceId : parseIds(mode.getTurnOffDeviceIds())) {
            deviceRepository.findByIdAndUserId(deviceId, userId).ifPresent(device -> {
                if (device.canReceiveOperationalChanges()) {
                    DeviceStatus previousStatus = device.getStatus();
                    device.turnOff();
                    Device savedDevice = deviceRepository.save(device);
                    energyReadingRecorderService.recordStatusTransition(savedDevice, previousStatus);
                    turnedOff.add(savedDevice.getId());
                }
            });
        }

        List<Long> enabledRoutines = new java.util.ArrayList<>();
        List<Long> disabledRoutines = new java.util.ArrayList<>();
        if (Boolean.TRUE.equals(mode.getApplyRoutines())) {
            for (Long routineId : parseIds(mode.getRoutinesToEnableIds())) {
                routineRepository.findByIdAndUserId(routineId, userId).ifPresent(routine -> {
                    routine.setEnabled(true);
                    routineRepository.save(routine);
                    enabledRoutines.add(routine.getId());
                });
            }
            for (Long routineId : parseIds(mode.getRoutinesToDisableIds())) {
                routineRepository.findByIdAndUserId(routineId, userId).ifPresent(routine -> {
                    routine.setEnabled(false);
                    routineRepository.save(routine);
                    disabledRoutines.add(routine.getId());
                });
            }
        }

        deactivateActiveModesInLocation(userId, mode);
        mode.activate(LocalDateTime.now());
        OperationMode savedMode = operationModeRepository.save(mode);

        String evidence = "Encendidos: %d. Apagados: %d. Rutinas internas: %d. Ignorados REMOVED: %d. Ignorados MAINTENANCE: %d."
                .formatted(turnedOn.size(), turnedOff.size(), mode.getInternalRoutines().size(), preview.ignoredRemovedDeviceIds().size(), preview.ignoredMaintenanceDeviceIds().size());

        domainEventPublisher.publish(new OperationModeActivatedEvent(
                userId,
                savedMode.getId(),
                savedMode.getName(),
                evidence,
                preview.explanation(),
                preview.recommendedAction()
        ));

        return new OperationModeActivationResult(
                savedMode,
                turnedOn,
                turnedOff,
                preview.ignoredRemovedDeviceIds(),
                preview.ignoredMaintenanceDeviceIds(),
                enabledRoutines,
                disabledRoutines,
                evidence,
                preview.explanation(),
                preview.recommendedAction()
        );
    }

    @Transactional
    public void archiveOperationMode(Long userId, Long modeId) {
        OperationMode mode = getModeForUser(userId, modeId);
        mode.archive();
        operationModeRepository.save(mode);
    }

    @Transactional
    public Device createDevice(CreateDeviceCommand command) {
        return registerDevice(
                command.userId(),
                command.name(),
                command.room(),
                command.type(),
                command.powerWatts()
        );
    }

    @Transactional
    public Device pairDevice(PairDeviceCommand command) {
        if (command.userId() == null) {
            throw new IllegalArgumentException("User id is required.");
        }

        var template = devicePairingCatalogService.resolve(command.pairingCode());
        String alias = command.alias() == null ? "" : command.alias().trim();
        String deviceName = alias.isBlank() ? template.defaultName() : alias;

        return registerDevice(
                command.userId(),
                deviceName,
                command.room(),
                template.type(),
                template.powerWatts()
        );
    }

    private Device registerDevice(
            Long userId,
            String name,
            String room,
            String type,
            BigDecimal powerWatts
    ) {
        devicePolicyService.validateDeviceData(name, type, powerWatts);

        Device device = new Device();
        device.setUserId(userId);
        device.setName(name.trim());
        device.setRoom(room != null ? room.trim() : "");
        device.setType(type.trim());
        device.setPowerWatts(powerWatts);
        device.setStatus(DeviceStatus.OFF);

        Device savedDevice = deviceRepository.save(device);
        domainEventPublisher.publish(new DeviceCreatedEvent(
                userId,
                savedDevice.getId(),
                savedDevice.getName()
        ));

        return savedDevice;
    }

    @Transactional
    public Device updateDevice(UpdateDeviceCommand command) {
        devicePolicyService.validateDeviceData(command.name(), command.type(), command.powerWatts());

        Device device = deviceRepository.findByIdAndUserId(command.deviceId(), command.userId())
                .orElseThrow(() -> new IllegalArgumentException("Device not found."));

        if (device.isRemoved()) {
            throw new IllegalArgumentException("Removed device cannot be updated.");
        }

        device.setName(command.name().trim());
        device.setRoom(command.room() != null ? command.room().trim() : "");
        device.setType(command.type().trim());
        device.setPowerWatts(command.powerWatts());

        return deviceRepository.save(device);
    }

    @Transactional
    public Device toggleDevice(Long userId, Long deviceId) {
        Device device = deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found."));

        DeviceStatus previousStatus = device.getStatus();
        device.toggle();

        Device savedDevice = deviceRepository.save(device);

        energyReadingRecorderService.recordStatusTransition(savedDevice, previousStatus);

        return savedDevice;
    }

    @Transactional
    public void deleteDevice(Long userId, Long deviceId) {
        Device device = deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found."));

        if (device.getStatus() == DeviceStatus.ON) {
            energyReadingRecorderService.closeUsage(device);
        }

        device.markAsRemoved();
        Device savedDevice = deviceRepository.save(device);

        deviceGroupDeviceRepository.deleteByDeviceId(savedDevice.getId());
        deleteDirectDeviceRoutines(userId, savedDevice.getId());
    }

    @Transactional
    public Device updateDeviceStatus(
            Long userId,
            Long deviceId,
            UpdateDeviceStatusCommand command
    ) {
        Device device = deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found."));

        DeviceStatus previousStatus = device.getStatus();

        switch (command.status()) {
            case ON -> device.turnOn();
            case OFF -> device.turnOff();
            case MAINTENANCE -> device.placeInMaintenance();
            case REMOVED -> device.markAsRemoved();
        }

        Device savedDevice = deviceRepository.save(device);

        energyReadingRecorderService.recordStatusTransition(savedDevice, previousStatus);

        if (savedDevice.isRemoved()) {
            deviceGroupDeviceRepository.deleteByDeviceId(savedDevice.getId());
            deleteDirectDeviceRoutines(userId, savedDevice.getId());
        }

        return savedDevice;
    }

    @Transactional(readOnly = true)
    public List<RoutineDetails> getRoutines(Long userId) {
        return routineRepository.findByUserId(userId)
                .stream()
                .map(this::toRoutineDetails)
                .toList();
    }

    @Transactional
    public RoutineDetails createRoutine(CreateRoutineCommand command) {
        if (command.userId() == null) {
            throw new IllegalArgumentException("User id is required.");
        }

        if (command.name() == null || command.name().trim().isEmpty()) {
            throw new IllegalArgumentException("Routine name is required.");
        }

        if (command.action() == null) {
            throw new IllegalArgumentException("Routine action is required.");
        }

        String routineTime = command.time() != null ? command.time().trim() : "";
        RoutineRepeatType repeatType = command.repeatType() != null ? command.repeatType() : RoutineRepeatType.DAILY;

        routineSchedulePolicyService.validateSchedule(
                routineTime,
                repeatType,
                command.daysOfWeek(),
                command.intervalDays(),
                command.startsOn()
        );

        RoutineTargetType targetType = command.targetType() != null
                ? command.targetType()
                : RoutineTargetType.DEVICE;

        Long targetId = command.targetId() != null
                ? command.targetId()
                : targetType == RoutineTargetType.GROUP
                  ? command.groupId()
                  : command.deviceId();

        if (targetId == null) {
            throw new IllegalArgumentException("Routine target is required.");
        }

        validateRoutineTarget(command.userId(), targetType, targetId);

        Routine routine = new Routine();
        routine.setUserId(command.userId());
        routine.setTargetType(targetType);
        routine.setTargetId(targetId);
        routine.setDeviceId(targetType == RoutineTargetType.DEVICE ? targetId : null);
        routine.setGroupId(targetType == RoutineTargetType.GROUP ? targetId : null);
        routine.setName(command.name().trim());
        routine.setAction(command.action());
        routine.setTime(routineTime);
        routine.setRepeatType(repeatType);
        routine.setDaysOfWeek(repeatType == RoutineRepeatType.WEEKLY && command.daysOfWeek() != null
                ? command.daysOfWeek().trim()
                : "");
        routine.setIntervalDays(repeatType == RoutineRepeatType.CUSTOM_INTERVAL
                ? command.intervalDays()
                : 1);
        routine.setStartsOn(command.startsOn() != null ? command.startsOn().trim() : null);
        routine.setEnabled(true);

        Routine savedRoutine = routineRepository.save(routine);

        return toRoutineDetails(savedRoutine);
    }

    @Transactional
    public RoutineDetails updateRoutineStatus(Long userId, Long routineId, UpdateRoutineStatusCommand command) {
        Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Routine not found."));

        routine.setEnabled(command.enabled());

        Routine savedRoutine = routineRepository.save(routine);

        return toRoutineDetails(savedRoutine);
    }

    @Transactional
    public void deleteRoutine(Long userId, Long routineId) {
        Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Routine not found."));

        routineRepository.delete(routine);
    }

    @Transactional
    public RoutineDetails executeRoutine(Long userId, Long routineId) {
        return executeRoutineInternal(userId, routineId, LocalDateTime.now());
    }

    @Transactional
    public RoutineDetails executeScheduledRoutine(Long userId, Long routineId, LocalDateTime executedAt) {
        return executeRoutineInternal(userId, routineId, executedAt != null ? executedAt : LocalDateTime.now());
    }

    private RoutineDetails executeRoutineInternal(Long userId, Long routineId, LocalDateTime executedAt) {
        Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Routine not found."));

        if (Boolean.FALSE.equals(routine.getEnabled())) {
            throw new IllegalArgumentException("Inactive routines cannot be executed.");
        }

        List<Device> applicableDevices = getRoutineScopedDevices(routine)
                .stream()
                .filter(Device::canReceiveOperationalChanges)
                .toList();

        if (applicableDevices.isEmpty()) {
            throw new IllegalArgumentException("Routine has no applicable devices.");
        }

        applicableDevices.forEach(device -> {
            DeviceStatus previousStatus = device.getStatus();

            if (routine.getAction() == com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineAction.TURN_ON) {
                device.turnOn();
            } else {
                device.turnOff();
            }

            Device savedDevice = deviceRepository.save(device);
            energyReadingRecorderService.recordStatusTransition(savedDevice, previousStatus);
        });

        routine.markExecutedAt(executedAt);
        Routine savedRoutine = routineRepository.save(routine);

        return toRoutineDetails(savedRoutine);
    }

    @Transactional
    public RoutineDetails toggleRoutine(Long userId, Long routineId) {
        Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Routine not found."));

        routine.toggle();

        Routine savedRoutine = routineRepository.save(routine);
        return toRoutineDetails(savedRoutine);
    }

    @Transactional(readOnly = true)
    public List<DeviceGroupDetails> getDeviceGroups(Long userId) {
        return deviceGroupRepository.findByUserId(userId)
                .stream()
                .map(group -> new DeviceGroupDetails(group, getDeviceIdsByGroupId(group.getId())))
                .toList();
    }

    @Transactional
    public DeviceGroupDetails createDeviceGroup(CreateDeviceGroupCommand command) {
        if (command.userId() == null) {
            throw new IllegalArgumentException("User id is required.");
        }

        if (command.name() == null || command.name().trim().isEmpty()) {
            throw new IllegalArgumentException("Group name is required.");
        }

        if (command.deviceIds() == null || command.deviceIds().isEmpty()) {
            throw new IllegalArgumentException("At least one device is required.");
        }

        validateDeviceIdsCanFormGroup(command.userId(), command.deviceIds());

        DeviceGroup group = new DeviceGroup();
        group.setUserId(command.userId());
        group.setName(command.name().trim());
        group.setDescription(command.description() != null ? command.description().trim() : "");

        DeviceGroup savedGroup = deviceGroupRepository.save(group);

        command.deviceIds()
                .stream()
                .distinct()
                .forEach(deviceId -> {
                    Device device = deviceRepository.findByIdAndUserId(deviceId, command.userId())
                            .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

                    DeviceGroupDevice relation = new DeviceGroupDevice();
                    relation.setDeviceGroupId(savedGroup.getId());
                    relation.setDeviceId(device.getId());

                    deviceGroupDeviceRepository.save(relation);
                });

        return new DeviceGroupDetails(savedGroup, getDeviceIdsByGroupId(savedGroup.getId()));
    }

    @Transactional
    public void deleteDeviceGroup(Long userId, Long groupId) {
        DeviceGroup group = getDeviceGroupForUser(userId, groupId);

        deviceGroupDeviceRepository.deleteByDeviceGroupId(group.getId());
        deviceGroupRepository.delete(group);
    }

    private List<Long> getDeviceIdsByGroupId(Long groupId) {
        return deviceGroupDeviceRepository.findByDeviceGroupId(groupId)
                .stream()
                .map(DeviceGroupDevice::getDeviceId)
                .toList();
    }

    @Transactional
    public void executeGroupAction(Long userId, Long groupId, ExecuteGroupActionCommand command) {
        DeviceGroup group = getDeviceGroupForUser(userId, groupId);

        List<Long> deviceIds = deviceGroupDeviceRepository.findByDeviceGroupId(group.getId())
                .stream()
                .map(DeviceGroupDevice::getDeviceId)
                .toList();

        if (deviceIds.isEmpty()) {
            throw new IllegalArgumentException("Device group has no devices.");
        }

        deviceIds.forEach(deviceId -> {
            Device device = deviceRepository.findByIdAndUserId(deviceId, group.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

            if (device.isRemoved() || device.isInMaintenance()) {
                return;
            }

            DeviceStatus previousStatus = device.getStatus();
            if (command.status() == DeviceStatus.ON) {
                device.turnOn();
            } else if (command.status() == DeviceStatus.OFF) {
                device.turnOff();
            } else if (command.status() == DeviceStatus.MAINTENANCE) {
                device.placeInMaintenance();
            } else {
                device.markAsRemoved();
            }
            Device savedDevice = deviceRepository.save(device);
            energyReadingRecorderService.recordStatusTransition(savedDevice, previousStatus);
            if (savedDevice.isRemoved()) {
                deviceGroupDeviceRepository.deleteByDeviceId(savedDevice.getId());
                deleteDirectDeviceRoutines(savedDevice.getUserId(), savedDevice.getId());
            }
        });
    }

    @Transactional
    public DeviceGroupDetails updateDeviceGroup(Long userId, Long groupId, UpdateDeviceGroupCommand command) {
        DeviceGroup group = getDeviceGroupForUser(userId, groupId);

        if (command.name() != null && !command.name().trim().isEmpty()) {
            group.setName(command.name().trim());
        }

        group.setDescription(command.description() != null ? command.description().trim() : "");

        if (command.deviceIds() != null) {
            validateDeviceIdsCanFormGroup(userId, command.deviceIds());
        }

        DeviceGroup savedGroup = deviceGroupRepository.save(group);

        if (command.deviceIds() != null) {
            deviceGroupDeviceRepository.deleteByDeviceGroupId(savedGroup.getId());

            command.deviceIds()
                    .stream()
                    .distinct()
                    .forEach(deviceId -> {
                        Device device = deviceRepository.findByIdAndUserId(deviceId, userId)
                                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

                        if (device.isRemoved() || device.isInMaintenance()) {
                            throw new IllegalArgumentException("Device is not available for groups: " + device.getName());
                        }

                        DeviceGroupDevice relation = new DeviceGroupDevice();
                        relation.setDeviceGroupId(savedGroup.getId());
                        relation.setDeviceId(device.getId());

                        deviceGroupDeviceRepository.save(relation);
                    });
        }

        List<Long> deviceIds = deviceGroupDeviceRepository.findByDeviceGroupId(savedGroup.getId())
                .stream()
                .map(DeviceGroupDevice::getDeviceId)
                .toList();

        return new DeviceGroupDetails(savedGroup, deviceIds);
    }

    private List<Long> getOperationalDeviceIdsByGroupId(Long groupId, Long userId) {
        return getDeviceIdsByGroupId(groupId)
                .stream()
                .filter(deviceId -> deviceRepository.findByIdAndUserId(deviceId, userId)
                        .map(device -> !device.isRemoved() && !device.isInMaintenance())
                        .orElse(false))
                .toList();
    }

    private DeviceGroup getDeviceGroupForUser(Long userId, Long groupId) {
        return deviceGroupRepository.findById(groupId)
                .filter(group -> group.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Device group not found."));
    }

    private RoutineDetails toRoutineDetails(Routine routine) {
        List<Device> scopedDevices = getRoutineScopedDevices(routine);
        int applicableDeviceCount = (int) scopedDevices.stream()
                .filter(Device::canReceiveOperationalChanges)
                .count();
        int blockedDeviceCount = scopedDevices.size() - applicableDeviceCount;

        return new RoutineDetails(
                routine,
                resolveRoutineTargetName(routine),
                applicableDeviceCount,
                blockedDeviceCount
        );
    }

    private String resolveRoutineTargetName(Routine routine) {
        return switch (routine.getTargetType()) {
            case GROUP -> deviceGroupRepository.findById(routine.getTargetId())
                    .map(DeviceGroup::getName)
                    .orElse("Grupo no encontrado");
            case ROOM -> roomRepository.findById(routine.getTargetId())
                    .map(Room::getName)
                    .orElse("Habitacion no encontrada");
            case WORKPLACE -> locationRepository.findById(routine.getTargetId())
                    .map(Location::getName)
                    .orElse("Sede no encontrada");
            case DEVICE -> deviceRepository.findById(routine.getTargetId())
                    .map(Device::getName)
                    .orElse("Dispositivo no encontrado");
        };
    }

    private void validateRoutineTarget(Long userId, RoutineTargetType targetType, Long targetId) {
        switch (targetType) {
            case DEVICE -> {
                Device device = deviceRepository.findByIdAndUserId(targetId, userId)
                        .orElseThrow(() -> new IllegalArgumentException("Device not found."));

                if (!device.canReceiveOperationalChanges()) {
                    throw new IllegalArgumentException("Device cannot receive normal routines in its current status.");
                }
            }
            case GROUP -> {
                DeviceGroup group = deviceGroupRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("Device group not found."));

                if (!group.getUserId().equals(userId)) {
                    throw new IllegalArgumentException("Device group does not belong to current user.");
                }
            }
            case ROOM -> {
                Room room = roomRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("Room not found."));
                ensureLocationBelongsToUser(room.getLocationId(), userId);
            }
            case WORKPLACE -> ensureLocationBelongsToUser(targetId, userId);
        }
    }

    private void ensureLocationBelongsToUser(Long locationId, Long userId) {
        locationRepository.findById(locationId)
                .filter(location -> location.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Location does not belong to current user."));
    }

    private List<Device> getRoutineScopedDevices(Routine routine) {
        Set<Long> deviceIds = switch (routine.getTargetType()) {
            case DEVICE -> routine.getTargetId() != null
                    ? Set.of(routine.getTargetId())
                    : Set.of();
            case GROUP -> new LinkedHashSet<>(getDeviceIdsByGroupId(routine.getTargetId()));
            case ROOM -> deviceAssignmentRepository.findByRoomId(routine.getTargetId())
                    .stream()
                    .map(DeviceAssignment::getDeviceId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            case WORKPLACE -> deviceAssignmentRepository.findByLocationId(routine.getTargetId())
                    .stream()
                    .map(DeviceAssignment::getDeviceId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        };

        return deviceIds.stream()
                .map(deviceRepository::findById)
                .flatMap(java.util.Optional::stream)
                .filter(device -> device.getUserId().equals(routine.getUserId()))
                .toList();
    }

    private void deleteDirectDeviceRoutines(Long userId, Long deviceId) {
        routineRepository.findByUserId(userId)
                .stream()
                .filter(routine -> routine.getTargetType() == RoutineTargetType.DEVICE)
                .filter(routine -> deviceId.equals(routine.getDeviceId()) || deviceId.equals(routine.getTargetId()))
                .forEach(routineRepository::delete);
    }

    private void validateModeInternalRoutines(
            Long userId,
            Long locationId,
            Long roomId,
            List<OperationModeRoutineCommand> routines
    ) {
        List<OperationModeRoutineCommand> safeRoutines = routines == null ? List.of() : routines;
        if (safeRoutines.isEmpty()) {
            throw new IllegalArgumentException("Operation mode must include at least one internal routine.");
        }

        for (OperationModeRoutineCommand routine : safeRoutines) {
            if (routine.name() == null || routine.name().trim().isEmpty()) {
                throw new IllegalArgumentException("Operation mode routine name is required.");
            }
            if (routine.targetType() == null || routine.targetId() == null) {
                throw new IllegalArgumentException("Operation mode routine target is required.");
            }
            if (routine.action() == null) {
                throw new IllegalArgumentException("Operation mode routine action is required.");
            }
            if (routine.triggerTime() == null || routine.triggerTime().trim().isEmpty()) {
                throw new IllegalArgumentException("Operation mode routine time is required.");
            }

            switch (routine.targetType()) {
                case DEVICE -> {
                    validateDeviceIdsInLocation(userId, locationId, List.of(routine.targetId()));
                    if (roomId != null) {
                        validateDeviceIdsInRoom(userId, roomId, List.of(routine.targetId()));
                    }
                }
                case GROUP -> {
                    validateGroupIdsInLocation(userId, locationId, List.of(routine.targetId()));
                    if (roomId != null) {
                        validateGroupIdsInRoom(userId, roomId, List.of(routine.targetId()));
                    }
                }
                case ROOM, WORKPLACE -> throw new IllegalArgumentException("Operation mode routines support devices or groups.");
            }
        }
    }

    private List<OperationModeRoutine> toModeRoutines(List<OperationModeRoutineCommand> commands) {
        if (commands == null) {
            return List.of();
        }

        return new java.util.ArrayList<>(commands.stream()
                .map(command -> new OperationModeRoutine(
                        command.name().trim(),
                        command.targetType(),
                        command.targetId(),
                        command.action(),
                        normalizeModeTime(command.triggerTime(), "00:00"),
                        command.enabled() == null || command.enabled()
                ))
                .toList());
    }

    private String normalizeModeTime(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.matches("^([01]\\d|2[0-3]):[0-5]\\d$") ? trimmed : fallback;
    }

    private void applyModeInternalRoutines(OperationMode mode, List<Long> turnedOn, List<Long> turnedOff) {
        mode.getInternalRoutines()
                .stream()
                .filter(routine -> routine.getEnabled() == null || routine.getEnabled())
                .forEach(routine -> resolveModeInternalRoutineDeviceIds(routine)
                        .forEach(deviceId -> applyModeRoutineAction(mode.getUserId(), deviceId, routine.getAction(), turnedOn, turnedOff)));
    }

    private Set<Long> resolveModeInternalRoutineDeviceIds(OperationModeRoutine routine) {
        if (routine.getTargetType() == RoutineTargetType.DEVICE) {
            return Set.of(routine.getTargetId());
        }

        if (routine.getTargetType() == RoutineTargetType.GROUP) {
            return new LinkedHashSet<>(getDeviceIdsByGroupId(routine.getTargetId()));
        }

        return Set.of();
    }

    private void applyModeRoutineAction(
            Long userId,
            Long deviceId,
            RoutineAction action,
            List<Long> turnedOn,
            List<Long> turnedOff
    ) {
        deviceRepository.findByIdAndUserId(deviceId, userId).ifPresent(device -> {
            if (!device.canReceiveOperationalChanges()) {
                return;
            }

            DeviceStatus previousStatus = device.getStatus();
            if (action == RoutineAction.TURN_ON) {
                device.turnOn();
            } else {
                device.turnOff();
            }

            Device savedDevice = deviceRepository.save(device);
            energyReadingRecorderService.recordStatusTransition(savedDevice, previousStatus);

            if (action == RoutineAction.TURN_ON) {
                turnedOn.add(savedDevice.getId());
            } else {
                turnedOff.add(savedDevice.getId());
            }
        });
    }

    private OperationMode getModeForUser(Long userId, Long modeId) {
        return operationModeRepository.findByIdAndUserId(modeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Operation mode not found."));
    }

    private OperationModePreviewResult buildOperationModePreview(OperationMode mode) {
        Location location = locationRepository.findById(mode.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found."));

        Set<Long> affectedDeviceIds = resolveModeDeviceIds(mode);
        List<Device> affectedDevices = affectedDeviceIds.stream()
                .map(deviceRepository::findById)
                .flatMap(Optional::stream)
                .filter(device -> device.getUserId().equals(mode.getUserId()))
                .toList();

        List<Long> removed = affectedDevices.stream()
                .filter(Device::isRemoved)
                .map(Device::getId)
                .toList();
        List<Long> maintenance = affectedDevices.stream()
                .filter(Device::isInMaintenance)
                .map(Device::getId)
                .toList();
        List<Long> applicable = affectedDevices.stream()
                .filter(Device::canReceiveOperationalChanges)
                .map(Device::getId)
                .toList();

        String evidence = "Sede %s, %d dispositivos aplicables, %d removidos, %d en mantenimiento."
                .formatted(location.getName(), applicable.size(), removed.size(), maintenance.size());
        String explanation = "El preview usa dispositivos directos, rooms, grupos y asignaciones de la sede. REMOVED y MAINTENANCE quedan fuera de la operacion normal.";
        String recommendedAction = applicable.isEmpty()
                ? "Selecciona dispositivos operativos o revisa asignaciones de sede antes de activar."
                : "Revisa los dispositivos ignorados antes de activar el modo.";

        return new OperationModePreviewResult(
                mode.getId(),
                mode.getLocationId(),
                location.getName(),
                applicable,
                removed,
                maintenance,
                parseIds(mode.getRoomIds()),
                parseIds(mode.getGroupIds()),
                parseIds(mode.getRoutineIds()),
                parseIds(mode.getGoalIds()),
                mode.getRuleProfileId(),
                mode.getPreferenceId(),
                evidence,
                explanation,
                recommendedAction
        );
    }

    private Set<Long> resolveModeDeviceIds(OperationMode mode) {
        Set<Long> deviceIds = new LinkedHashSet<>();
        deviceIds.addAll(parseIds(mode.getDeviceIds()));
        deviceIds.addAll(parseIds(mode.getTurnOnDeviceIds()));
        deviceIds.addAll(parseIds(mode.getTurnOffDeviceIds()));
        deviceIds.addAll(parseIds(mode.getKeepOnDeviceIds()));
        mode.getInternalRoutines().forEach(routine -> deviceIds.addAll(resolveModeInternalRoutineDeviceIds(routine)));

        parseIds(mode.getRoomIds()).forEach(roomId ->
                deviceAssignmentRepository.findByRoomId(roomId)
                        .forEach(assignment -> deviceIds.add(assignment.getDeviceId()))
        );

        parseIds(mode.getGroupIds()).forEach(groupId -> deviceIds.addAll(getDeviceIdsByGroupId(groupId)));

        deviceAssignmentRepository.findByLocationId(mode.getLocationId())
                .forEach(assignment -> {
                    if (parseIds(mode.getRoomIds()).isEmpty()) {
                        deviceIds.add(assignment.getDeviceId());
                    }
                });

        return deviceIds;
    }

    private void validateRoomIds(Long locationId, List<Long> roomIds) {
        for (Long roomId : safeIds(roomIds)) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
            if (!room.getLocationId().equals(locationId)) {
                throw new IllegalArgumentException("Room does not belong to selected location: " + roomId);
            }
        }
    }

    private void validateGroupIds(Long userId, List<Long> groupIds) {
        for (Long groupId : safeIds(groupIds)) {
            DeviceGroup group = deviceGroupRepository.findById(groupId)
                    .orElseThrow(() -> new IllegalArgumentException("Device group not found: " + groupId));
            if (!group.getUserId().equals(userId)) {
                throw new IllegalArgumentException("Device group does not belong to current user.");
            }
        }
    }

    private void validateDeviceIds(Long userId, List<Long> deviceIds) {
        for (Long deviceId : safeIds(deviceIds)) {
            deviceRepository.findByIdAndUserId(deviceId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        }
    }

    private void validateDeviceIdsInLocation(Long userId, Long locationId, List<Long> deviceIds) {
        for (Long deviceId : safeIds(deviceIds)) {
            Device device = deviceRepository.findByIdAndUserId(deviceId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

            boolean assignedToLocation = deviceAssignmentRepository.findByDeviceId(device.getId())
                    .stream()
                    .anyMatch(assignment -> locationId.equals(assignment.getLocationId()));

            if (!assignedToLocation) {
                throw new IllegalArgumentException("Device does not belong to selected location: " + device.getName());
            }
        }
    }

    private void validateDeviceIdsCanFormGroup(Long userId, List<Long> deviceIds) {
        List<Long> requestedDeviceIds = deviceIds == null
                ? List.of()
                : deviceIds.stream().filter(java.util.Objects::nonNull).toList();
        if (requestedDeviceIds.isEmpty()) {
            throw new IllegalArgumentException("devicecontrol.group.devices.required");
        }

        Set<Long> uniqueDeviceIds = new LinkedHashSet<>(requestedDeviceIds);
        if (uniqueDeviceIds.size() != requestedDeviceIds.size()) {
            throw new IllegalArgumentException("devicecontrol.group.devices.duplicate");
        }

        Set<Long> locationIds = new LinkedHashSet<>();
        Set<Long> roomIds = new LinkedHashSet<>();

        for (Long deviceId : uniqueDeviceIds) {
            Device device = deviceRepository.findByIdAndUserId(deviceId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

            if (device.isRemoved() || device.isInMaintenance()) {
                throw new IllegalArgumentException("Device is not available for groups: " + device.getName());
            }

            DeviceAssignment assignment = getRequiredAssignmentForDevice(device);
            if (assignment.getRoomId() == null) {
                throw new IllegalArgumentException("Device must be assigned to a room before grouping: " + device.getName());
            }

            locationIds.add(assignment.getLocationId());
            roomIds.add(assignment.getRoomId());
        }

        if (locationIds.size() > 1) {
            throw new IllegalArgumentException("Device groups cannot mix devices from different workplaces.");
        }

        if (roomIds.size() > 1) {
            throw new IllegalArgumentException("Device groups cannot mix devices from different rooms.");
        }
    }

    private DeviceAssignment getRequiredAssignmentForDevice(Device device) {
        return deviceAssignmentRepository.findByDeviceId(device.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Device must be assigned to a workplace before grouping: " + device.getName()
                ));
    }

    private Long resolveModeRoomScope(Long locationId, List<Long> roomIds) {
        List<Long> safeRoomIds = safeIds(roomIds);
        if (safeRoomIds.size() > 1) {
            throw new IllegalArgumentException("Operation modes can target General or one room only.");
        }

        validateRoomIds(locationId, safeRoomIds);
        return safeRoomIds.isEmpty() ? null : safeRoomIds.get(0);
    }

    private void validateModeRoomScope(
            Long userId,
            Long locationId,
            Long roomId,
            List<Long> groupIds,
            List<Long> deviceIds,
            List<Long> routineIds,
            List<Long> goalIds
    ) {
        if (roomId == null) {
            return;
        }

        validateDeviceIdsInRoom(userId, roomId, deviceIds);
        validateGroupIdsInRoom(userId, roomId, groupIds);
        validateRoutineIdsInRoom(userId, locationId, roomId, routineIds);
        validateGoalIdsInRoom(userId, roomId, goalIds);
    }

    private void validateDeviceIdsInRoom(Long userId, Long roomId, List<Long> deviceIds) {
        for (Long deviceId : safeIds(deviceIds)) {
            Device device = deviceRepository.findByIdAndUserId(deviceId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

            boolean assignedToRoom = deviceAssignmentRepository.findByDeviceId(device.getId())
                    .stream()
                    .anyMatch(assignment -> roomId.equals(assignment.getRoomId()));

            if (!assignedToRoom) {
                throw new IllegalArgumentException("Device does not belong to selected room: " + device.getName());
            }
        }
    }

    private void validateGroupIdsInRoom(Long userId, Long roomId, List<Long> groupIds) {
        for (Long groupId : safeIds(groupIds)) {
            DeviceGroup group = deviceGroupRepository.findById(groupId)
                    .filter(item -> item.getUserId().equals(userId))
                    .orElseThrow(() -> new IllegalArgumentException("Device group not found: " + groupId));

            List<Long> groupDeviceIds = getDeviceIdsByGroupId(group.getId());
            if (groupDeviceIds.isEmpty()) {
                throw new IllegalArgumentException("Device group has no devices: " + group.getName());
            }

            validateDeviceIdsInRoom(userId, roomId, groupDeviceIds);
        }
    }

    private void validateRoutineIdsInRoom(Long userId, Long locationId, Long roomId, List<Long> routineIds) {
        for (Long routineId : safeIds(routineIds)) {
            Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Routine not found: " + routineId));

            switch (routine.getTargetType()) {
                case DEVICE -> validateDeviceIdsInRoom(userId, roomId, List.of(routine.getTargetId()));
                case GROUP -> validateGroupIdsInRoom(userId, roomId, List.of(routine.getTargetId()));
                case ROOM -> {
                    if (!roomId.equals(routine.getTargetId())) {
                        throw new IllegalArgumentException("Routine belongs to another room: " + routine.getName());
                    }
                }
                case WORKPLACE -> {
                    if (locationId.equals(routine.getTargetId())) {
                        throw new IllegalArgumentException("Workplace routines cannot be attached to room-scoped modes: " + routine.getName());
                    }
                    throw new IllegalArgumentException("Routine belongs to another workplace: " + routine.getName());
                }
            }
        }
    }

    private void validateGoalIdsInRoom(Long userId, Long roomId, List<Long> goalIds) {
        for (Long goalId : safeIds(goalIds)) {
            var goal = energyGoalRepository.findById(goalId)
                    .filter(item -> item.getUserId().equals(userId))
                    .orElseThrow(() -> new IllegalArgumentException("Energy goal not found: " + goalId));

            String scopeType = goal.getScopeType() == null ? "GENERAL" : goal.getScopeType().trim().toUpperCase();
            Long scopeId = goal.getScopeId();

            switch (scopeType) {
                case "ROOM" -> {
                    if (!roomId.equals(scopeId)) {
                        throw new IllegalArgumentException("Energy goal room does not match selected room: " + goal.getTitle());
                    }
                }
                case "DEVICE" -> {
                    if (scopeId == null) {
                        throw new IllegalArgumentException("Energy goal device scope is missing: " + goal.getTitle());
                    }
                    validateDeviceIdsInRoom(userId, roomId, List.of(scopeId));
                }
                case "GROUP" -> {
                    if (scopeId == null) {
                        throw new IllegalArgumentException("Energy goal group scope is missing: " + goal.getTitle());
                    }
                    validateGroupIdsInRoom(userId, roomId, List.of(scopeId));
                }
                case "GENERAL", "WORKPLACE" -> throw new IllegalArgumentException(
                        "Energy goal is broader than selected room: " + goal.getTitle()
                );
                default -> throw new IllegalArgumentException("Unsupported energy goal scope: " + scopeType);
            }
        }
    }

    private void deactivateActiveModesInLocation(Long userId, OperationMode mode) {
        operationModeRepository.findActiveByUserIdAndLocationIdForUpdate(userId, mode.getLocationId(), OperationModeStatus.ACTIVE)
                .stream()
                .filter(activeMode -> !activeMode.getId().equals(mode.getId()))
                .forEach(activeMode -> {
                    activeMode.deactivate();
                    operationModeRepository.save(activeMode);
                });
    }

    private void lockModesInLocation(Long userId, Long locationId) {
        operationModeRepository.findByUserIdAndLocationIdForUpdate(userId, locationId);
    }

    private void validateGroupIdsInLocation(Long userId, Long locationId, List<Long> groupIds) {
        for (Long groupId : safeIds(groupIds)) {
            DeviceGroup group = deviceGroupRepository.findById(groupId)
                    .filter(item -> item.getUserId().equals(userId))
                    .orElseThrow(() -> new IllegalArgumentException("Device group not found: " + groupId));

            List<Long> groupDeviceIds = getDeviceIdsByGroupId(group.getId());
            if (groupDeviceIds.isEmpty()) {
                throw new IllegalArgumentException("Device group has no devices: " + group.getName());
            }

            validateDeviceIdsInLocation(userId, locationId, groupDeviceIds);
        }
    }

    private void validateRoutineIds(Long userId, List<Long> routineIds) {
        for (Long routineId : safeIds(routineIds)) {
            routineRepository.findByIdAndUserId(routineId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Routine not found: " + routineId));
        }
    }

    private void validateRoutineIdsInLocation(Long userId, Long locationId, List<Long> routineIds) {
        for (Long routineId : safeIds(routineIds)) {
            Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Routine not found: " + routineId));

            switch (routine.getTargetType()) {
                case DEVICE -> validateDeviceIdsInLocation(userId, locationId, List.of(routine.getTargetId()));
                case GROUP -> validateGroupIdsInLocation(userId, locationId, List.of(routine.getTargetId()));
                case ROOM -> validateRoomIds(locationId, List.of(routine.getTargetId()));
                case WORKPLACE -> {
                    if (!locationId.equals(routine.getTargetId())) {
                        throw new IllegalArgumentException("Routine belongs to another workplace: " + routine.getName());
                    }
                }
            }
        }
    }

    private void validateGoalIds(Long userId, List<Long> goalIds) {
        for (Long goalId : safeIds(goalIds)) {
            energyGoalRepository.findById(goalId)
                    .filter(goal -> goal.getUserId().equals(userId))
                    .orElseThrow(() -> new IllegalArgumentException("Energy goal not found: " + goalId));
        }
    }

    private void validateGoalIdsInLocation(Long userId, Long locationId, List<Long> goalIds) {
        Set<Long> locationRoomIds = roomRepository.findByLocationId(locationId)
                .stream()
                .map(Room::getId)
                .collect(Collectors.toSet());

        for (Long goalId : safeIds(goalIds)) {
            var goal = energyGoalRepository.findById(goalId)
                    .filter(item -> item.getUserId().equals(userId))
                    .orElseThrow(() -> new IllegalArgumentException("Energy goal not found: " + goalId));

            String scopeType = goal.getScopeType() == null ? "GENERAL" : goal.getScopeType().trim().toUpperCase();
            Long scopeId = goal.getScopeId();

            switch (scopeType) {
                case "GENERAL" -> {
                }
                case "WORKPLACE" -> {
                    if (!locationId.equals(scopeId)) {
                        throw new IllegalArgumentException("Energy goal belongs to another workplace: " + goal.getTitle());
                    }
                }
                case "ROOM" -> {
                    if (scopeId == null || !locationRoomIds.contains(scopeId)) {
                        throw new IllegalArgumentException("Energy goal room belongs to another workplace: " + goal.getTitle());
                    }
                }
                case "DEVICE" -> {
                    if (scopeId == null) {
                        throw new IllegalArgumentException("Energy goal device scope is missing: " + goal.getTitle());
                    }
                    validateDeviceIdsInLocation(userId, locationId, List.of(scopeId));
                }
                case "GROUP" -> {
                    if (scopeId == null) {
                        throw new IllegalArgumentException("Energy goal group scope is missing: " + goal.getTitle());
                    }
                    validateGroupIdsInLocation(userId, locationId, List.of(scopeId));
                }
                default -> throw new IllegalArgumentException("Unsupported energy goal scope: " + scopeType);
            }
        }
    }

    private void validateRuleProfile(Long userId, Long ruleProfileId) {
        if (ruleProfileId == null) return;
        alertRuleProfileRepository.findByIdAndUserId(ruleProfileId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Rule profile not found."));
    }

    private void validatePreference(Long userId, Long preferenceId) {
        if (preferenceId == null) return;
        notificationPreferenceRepository.findById(preferenceId)
                .filter(preference -> preference.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Notification preference not found."));
    }

    @SafeVarargs
    private final List<Long> mergeIds(List<Long>... values) {
        return Arrays.stream(values)
                .filter(java.util.Objects::nonNull)
                .flatMap(List::stream)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<Long> safeIds(List<Long> ids) {
        return ids == null ? List.of() : ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }

    private String toCsv(List<Long> ids) {
        return safeIds(ids).stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Long> parseIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(Long::valueOf)
                .distinct()
                .toList();
    }
}
