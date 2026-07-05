package com.electrocorp.electrocorpplatform.workplace.interfaces.rest.controllers;

import com.electrocorp.electrocorpplatform.workplace.application.commandservices.WorkplaceCommandService;
import com.electrocorp.electrocorpplatform.workplace.application.queryservices.WorkplaceQueryService;
import com.electrocorp.electrocorpplatform.workplace.interfaces.rest.resources.*;
import com.electrocorp.electrocorpplatform.workplace.interfaces.rest.transform.AssignDeviceCommandFromResourceAssembler;
import com.electrocorp.electrocorpplatform.workplace.interfaces.rest.transform.CreateLocationCommandFromResourceAssembler;
import com.electrocorp.electrocorpplatform.workplace.interfaces.rest.transform.CreateRoomCommandFromResourceAssembler;
import com.electrocorp.electrocorpplatform.workplace.interfaces.rest.transform.DeviceAssignmentResourceFromEntityAssembler;
import com.electrocorp.electrocorpplatform.workplace.interfaces.rest.transform.LocationResourceFromEntityAssembler;
import com.electrocorp.electrocorpplatform.workplace.interfaces.rest.transform.RoomResourceFromEntityAssembler;
import com.electrocorp.electrocorpplatform.shared.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workplace")
@RequiredArgsConstructor
public class WorkplaceController {

    private final WorkplaceCommandService commandService;
    private final WorkplaceQueryService queryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/locations")
    public List<LocationResource> getLocations() {
        return queryService.getLocations(currentUserProvider.getCurrentUserId())
                .stream()
                .map(LocationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @PostMapping("/locations")
    public LocationResource createLocation(@Valid @RequestBody CreateLocationResource request) {
        return LocationResourceFromEntityAssembler.toResourceFromEntity(
                commandService.createLocation(CreateLocationCommandFromResourceAssembler.toCommandFromResource(request, currentUserProvider.getCurrentUserId()))
        );
    }

    @GetMapping("/rooms")
    public List<RoomResource> getRooms(
            @RequestParam(required = false) Long locationId
    ) {
        List<com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.Room> rooms = locationId != null
                ? queryService.getRooms(currentUserProvider.getCurrentUserId(), locationId)
                : queryService.getRoomsByUserId(currentUserProvider.getCurrentUserId());

        return rooms
                .stream()
                .map(RoomResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @PostMapping("/rooms")
    public RoomResource createRoom(@Valid @RequestBody CreateRoomResource request) {
        return RoomResourceFromEntityAssembler.toResourceFromEntity(
                commandService.createRoom(currentUserProvider.getCurrentUserId(), CreateRoomCommandFromResourceAssembler.toCommandFromResource(request))
        );
    }

    @GetMapping("/device-assignments")
    public List<DeviceAssignmentResource> getAssignments(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long deviceId
    ) {
        List<com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.DeviceAssignment> assignments = deviceId != null
                ? queryService.getAssignmentsByDeviceId(currentUserProvider.getCurrentUserId(), deviceId)
                : locationId != null
                ? queryService.getAssignments(currentUserProvider.getCurrentUserId(), locationId)
                : queryService.getAssignmentsByUserId(currentUserProvider.getCurrentUserId());

        return assignments
                .stream()
                .map(DeviceAssignmentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @PostMapping("/device-assignments")
    public DeviceAssignmentResource assignDevice(@Valid @RequestBody AssignDeviceResource request) {
        return DeviceAssignmentResourceFromEntityAssembler.toResourceFromEntity(
                commandService.assignDevice(currentUserProvider.getCurrentUserId(), AssignDeviceCommandFromResourceAssembler.toCommandFromResource(request))
        );
    }

    @PatchMapping("/locations/{locationId}")
    public LocationResource updateLocation(
            @PathVariable Long locationId,
            @RequestBody UpdateLocationResource request
    ) {
        return LocationResourceFromEntityAssembler.toResourceFromEntity(
                commandService.updateLocation(currentUserProvider.getCurrentUserId(), locationId, request.name(), request.address(), request.type())
        );
    }

    @DeleteMapping("/locations/{locationId}")
    public void deleteLocation(@PathVariable Long locationId) {
        commandService.deleteLocation(currentUserProvider.getCurrentUserId(), locationId);
    }

    @PatchMapping("/rooms/{roomId}")
    public RoomResource updateRoom(
            @PathVariable Long roomId,
            @RequestBody UpdateRoomResource request
    ) {
        return RoomResourceFromEntityAssembler.toResourceFromEntity(
                commandService.updateRoom(currentUserProvider.getCurrentUserId(), roomId, request.locationId(), request.name(), request.floor())
        );
    }

    @DeleteMapping("/rooms/{roomId}")
    public void deleteRoom(@PathVariable Long roomId) {
        commandService.deleteRoom(currentUserProvider.getCurrentUserId(), roomId);
    }

    @PatchMapping("/device-assignments/{assignmentId}")
    public DeviceAssignmentResource moveAssignment(
            @PathVariable Long assignmentId,
            @RequestBody MoveDeviceAssignmentResource request
    ) {
        return DeviceAssignmentResourceFromEntityAssembler.toResourceFromEntity(
                commandService.moveAssignment(currentUserProvider.getCurrentUserId(), assignmentId, request.locationId(), request.roomId())
        );
    }

    @DeleteMapping("/device-assignments/{assignmentId}")
    public void deleteAssignment(@PathVariable Long assignmentId) {
        commandService.deleteAssignment(currentUserProvider.getCurrentUserId(), assignmentId);
    }
}
