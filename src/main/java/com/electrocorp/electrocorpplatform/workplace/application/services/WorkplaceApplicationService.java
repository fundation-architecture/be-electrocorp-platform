package com.electrocorp.electrocorpplatform.workplace.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.application.facades.DeviceControlContextFacade;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import com.electrocorp.electrocorpplatform.workplace.application.commandservices.WorkplaceCommandService;
import com.electrocorp.electrocorpplatform.workplace.application.queryservices.WorkplaceQueryService;
import com.electrocorp.electrocorpplatform.workplace.domain.model.commands.AssignDeviceCommand;
import com.electrocorp.electrocorpplatform.workplace.domain.model.commands.CreateLocationCommand;
import com.electrocorp.electrocorpplatform.workplace.domain.model.commands.CreateRoomCommand;
import com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.*;
import com.electrocorp.electrocorpplatform.workplace.domain.model.events.DeviceAssignedToRoomEvent;
import com.electrocorp.electrocorpplatform.workplace.domain.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkplaceApplicationService implements WorkplaceCommandService, WorkplaceQueryService {

    private final LocationRepository locationRepository;
    private final RoomRepository roomRepository;
    private final DeviceAssignmentRepository assignmentRepository;
    private final DeviceControlContextFacade deviceControlContextFacade;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<Location> getLocations(Long userId) {
        return locationRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Location createLocation(CreateLocationCommand command) {
        Location location = new Location();
        location.setUserId(command.userId());
        location.setName(command.name());
        location.setAddress(command.address());
        location.setType(command.type());
        return locationRepository.save(location);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> getRooms(Long userId, Long locationId) {
        getLocationForUser(locationId, userId);
        return roomRepository.findByLocationId(locationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> getRoomsByUserId(Long userId) {
        List<Long> locationIds = locationRepository.findByUserId(userId)
                .stream()
                .map(Location::getId)
                .toList();

        return locationIds.isEmpty() ? List.of() : roomRepository.findByLocationIdIn(locationIds);
    }

    @Override
    @Transactional
    public Room createRoom(Long userId, CreateRoomCommand command) {
        getLocationForUser(command.locationId(), userId);

        Room room = new Room();
        room.setLocationId(command.locationId());
        room.setName(command.name());
        room.setFloor(command.floor());
        return roomRepository.save(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceAssignment> getAssignments(Long userId, Long locationId) {
        getLocationForUser(locationId, userId);
        return getCurrentAssignmentsByUserId(userId)
                .stream()
                .filter(assignment -> assignment.getLocationId().equals(locationId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceAssignment> getAssignmentsByUserId(Long userId) {
        return getCurrentAssignmentsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceAssignment> getAssignmentsByDeviceId(Long userId, Long deviceId) {
        deviceControlContextFacade.ensureDeviceBelongsToUser(deviceId, userId);

        return getCurrentAssignmentsByUserId(userId)
                .stream()
                .filter(assignment -> assignment.getDeviceId().equals(deviceId))
                .toList();
    }

    @Override
    @Transactional
    public DeviceAssignment assignDevice(Long userId, AssignDeviceCommand command) {
        getLocationForUser(command.locationId(), userId);
        deviceControlContextFacade.ensureDeviceBelongsToUser(command.deviceId(), userId);

        Room room = getRoomForLocation(command.roomId(), command.locationId());

        if (!assignmentRepository.findByDeviceId(command.deviceId()).isEmpty()) {
            throw new IllegalArgumentException("Device is already assigned.");
        }

        DeviceAssignment assignment = new DeviceAssignment();
        assignment.setDeviceId(command.deviceId());
        assignment.setRoomId(command.roomId());
        assignment.setLocationId(command.locationId());
        assignment.setAssignedAt(LocalDateTime.now());
        DeviceAssignment savedAssignment = assignmentRepository.save(assignment);
        syncDeviceRoom(command.deviceId(), userId, room);
        domainEventPublisher.publish(new DeviceAssignedToRoomEvent(
                userId,
                savedAssignment.getDeviceId(),
                savedAssignment.getRoomId()
        ));
        return savedAssignment;
    }

    @Override
    @Transactional
    public Location updateLocation(Long userId, Long locationId, String name, String address, String type) {
        Location location = getLocationForUser(locationId, userId);

        if (name != null && !name.isBlank()) location.setName(name.trim());
        if (address != null) location.setAddress(address.trim());
        if (type != null) location.setType(type);

        return locationRepository.save(location);
    }

    @Override
    @Transactional
    public void deleteLocation(Long userId, Long locationId) {
        Location location = getLocationForUser(locationId, userId);
        assignmentRepository.deleteByLocationId(locationId);
        locationRepository.delete(location);
    }

    @Override
    @Transactional
    public Room updateRoom(Long userId, Long roomId, Long locationId, String name, String floor) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found."));

        getLocationForUser(room.getLocationId(), userId);
        if (locationId != null) {
            getLocationForUser(locationId, userId);
            room.setLocationId(locationId);
        }
        if (name != null && !name.isBlank()) room.setName(name.trim());
        if (floor != null) room.setFloor(floor.trim());

        Room savedRoom = roomRepository.save(room);
        assignmentRepository.findByRoomId(savedRoom.getId())
                .forEach(assignment -> {
                    assignment.setLocationId(savedRoom.getLocationId());
                    assignmentRepository.save(assignment);
                    syncDeviceRoom(assignment.getDeviceId(), userId, savedRoom);
                });

        return savedRoom;
    }

    @Override
    @Transactional
    public void deleteRoom(Long userId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found."));

        getLocationForUser(room.getLocationId(), userId);

        assignmentRepository.findByRoomId(roomId)
                .forEach(assignment -> {
                    assignment.setRoomId(null);
                    syncDeviceRoom(assignment.getDeviceId(), userId, null);
                    assignmentRepository.save(assignment);
                });

        roomRepository.delete(room);
    }

    @Override
    @Transactional
    public DeviceAssignment moveAssignment(Long userId, Long assignmentId, Long locationId, Long roomId) {
        DeviceAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found."));

        getLocationForUser(assignment.getLocationId(), userId);
        Long targetLocationId = locationId != null ? locationId : assignment.getLocationId();
        if (locationId != null) {
            getLocationForUser(locationId, userId);
            assignment.setLocationId(locationId);
        }

        Room room = getRoomForLocation(roomId, targetLocationId);
        assignment.setRoomId(roomId);
        assignment.setAssignedAt(LocalDateTime.now());

        DeviceAssignment savedAssignment = assignmentRepository.save(assignment);
        syncDeviceRoom(assignment.getDeviceId(), userId, room);
        domainEventPublisher.publish(new DeviceAssignedToRoomEvent(
                userId,
                savedAssignment.getDeviceId(),
                savedAssignment.getRoomId()
        ));
        return savedAssignment;
    }

    @Override
    @Transactional
    public void deleteAssignment(Long userId, Long assignmentId) {
        DeviceAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found."));
        getLocationForUser(assignment.getLocationId(), userId);
        syncDeviceRoom(assignment.getDeviceId(), userId, null);
        assignmentRepository.delete(assignment);
    }

    private Location getLocationForUser(Long locationId, Long userId) {
        return locationRepository.findById(locationId)
                .filter(location -> location.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Location does not belong to current user."));
    }

    private Room getRoomForLocation(Long roomId, Long locationId) {
        if (roomId == null) {
            return null;
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found."));

        if (!room.getLocationId().equals(locationId)) {
            throw new IllegalArgumentException("Room does not belong to the selected location.");
        }

        return room;
    }

    private void syncDeviceRoom(Long deviceId, Long userId, Room room) {
        deviceControlContextFacade.updateDeviceRoom(deviceId, userId, room != null ? room.getName() : "");
    }

    private List<DeviceAssignment> getCurrentAssignmentsByUserId(Long userId) {
        List<Long> locationIds = locationRepository.findByUserId(userId)
                .stream()
                .map(Location::getId)
                .toList();

        if (locationIds.isEmpty()) {
            return List.of();
        }

        return currentAssignments(assignmentRepository.findByLocationIdIn(locationIds));
    }

    private List<DeviceAssignment> currentAssignments(List<DeviceAssignment> assignments) {
        Map<Long, DeviceAssignment> currentByDeviceId = new LinkedHashMap<>();

        for (DeviceAssignment assignment : assignments) {
            if (assignment.getDeviceId() == null) {
                continue;
            }

            DeviceAssignment current = currentByDeviceId.get(assignment.getDeviceId());

            if (current == null || isNewerAssignment(assignment, current)) {
                currentByDeviceId.put(assignment.getDeviceId(), assignment);
            }
        }

        return currentByDeviceId.values()
                .stream()
                .sorted(this::compareAssignmentsNewestFirst)
                .toList();
    }

    private int compareAssignmentsNewestFirst(DeviceAssignment first, DeviceAssignment second) {
        if (isNewerAssignment(first, second)) {
            return -1;
        }

        if (isNewerAssignment(second, first)) {
            return 1;
        }

        return 0;
    }

    private boolean isNewerAssignment(DeviceAssignment candidate, DeviceAssignment current) {
        LocalDateTime candidateAssignedAt = candidate.getAssignedAt();
        LocalDateTime currentAssignedAt = current.getAssignedAt();

        if (candidateAssignedAt == null && currentAssignedAt == null) {
            return assignmentId(candidate) > assignmentId(current);
        }

        if (candidateAssignedAt == null) {
            return false;
        }

        if (currentAssignedAt == null) {
            return true;
        }

        int compared = candidateAssignedAt.compareTo(currentAssignedAt);
        return compared > 0 || (compared == 0 && assignmentId(candidate) > assignmentId(current));
    }

    private long assignmentId(DeviceAssignment assignment) {
        return assignment.getId() == null ? 0L : assignment.getId();
    }
}
