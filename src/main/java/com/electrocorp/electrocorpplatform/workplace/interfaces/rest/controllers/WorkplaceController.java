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
import com.electrocorp.electrocorpplatform.iam.application.security.AccessAuthorizationService;
import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;
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
    private final AccessAuthorizationService accessAuthorizationService;

    @GetMapping("/locations")
    public List<LocationResource> getLocations() {
        return queryService.getLocations(requireSpaceManagement())
                .stream()
                .map(LocationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @PostMapping("/locations")
    public LocationResource createLocation(@Valid @RequestBody CreateLocationResource request) {
        return LocationResourceFromEntityAssembler.toResourceFromEntity(
                commandService.createLocation(CreateLocationCommandFromResourceAssembler.toCommandFromResource(request, requireSpaceManagement()))
        );
    }

    @GetMapping("/rooms")
    public List<RoomResource> getRooms(
            @RequestParam(required = false) Long locationId
    ) {
        Long userId = requireSpaceManagement();
        List<com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.Room> rooms = locationId != null
                ? queryService.getRooms(userId, locationId)
                : queryService.getRoomsByUserId(userId);

        return rooms
                .stream()
                .map(RoomResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @PostMapping("/rooms")
    public RoomResource createRoom(@Valid @RequestBody CreateRoomResource request) {
        return RoomResourceFromEntityAssembler.toResourceFromEntity(
                commandService.createRoom(requireSpaceManagement(), CreateRoomCommandFromResourceAssembler.toCommandFromResource(request))
        );
    }

    @GetMapping("/device-assignments")
    public List<DeviceAssignmentResource> getAssignments(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long deviceId
    ) {
        Long userId = requireSpaceManagement();
        List<com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.DeviceAssignment> assignments = deviceId != null
                ? queryService.getAssignmentsByDeviceId(userId, deviceId)
                : locationId != null
                ? queryService.getAssignments(userId, locationId)
                : queryService.getAssignmentsByUserId(userId);

        return assignments
                .stream()
                .map(DeviceAssignmentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @PostMapping("/device-assignments")
    public DeviceAssignmentResource assignDevice(@Valid @RequestBody AssignDeviceResource request) {
        return DeviceAssignmentResourceFromEntityAssembler.toResourceFromEntity(
                commandService.assignDevice(requireSpaceManagement(), AssignDeviceCommandFromResourceAssembler.toCommandFromResource(request))
        );
    }

    @PatchMapping("/locations/{locationId}")
    public LocationResource updateLocation(
            @PathVariable Long locationId,
            @RequestBody UpdateLocationResource request
    ) {
        return LocationResourceFromEntityAssembler.toResourceFromEntity(
                commandService.updateLocation(requireSpaceManagement(), locationId, request.name(), request.address(), request.type())
        );
    }

    @DeleteMapping("/locations/{locationId}")
    public void deleteLocation(@PathVariable Long locationId) {
        commandService.deleteLocation(requireSpaceManagement(), locationId);
    }

    @PatchMapping("/rooms/{roomId}")
    public RoomResource updateRoom(
            @PathVariable Long roomId,
            @RequestBody UpdateRoomResource request
    ) {
        return RoomResourceFromEntityAssembler.toResourceFromEntity(
                commandService.updateRoom(requireSpaceManagement(), roomId, request.locationId(), request.name(), request.floor())
        );
    }

    @DeleteMapping("/rooms/{roomId}")
    public void deleteRoom(@PathVariable Long roomId) {
        commandService.deleteRoom(requireSpaceManagement(), roomId);
    }

    @PatchMapping("/device-assignments/{assignmentId}")
    public DeviceAssignmentResource moveAssignment(
            @PathVariable Long assignmentId,
            @RequestBody MoveDeviceAssignmentResource request
    ) {
        return DeviceAssignmentResourceFromEntityAssembler.toResourceFromEntity(
                commandService.moveAssignment(requireSpaceManagement(), assignmentId, request.locationId(), request.roomId())
        );
    }

    @DeleteMapping("/device-assignments/{assignmentId}")
    public void deleteAssignment(@PathVariable Long assignmentId) {
        commandService.deleteAssignment(requireSpaceManagement(), assignmentId);
    }

    private Long requireSpaceManagement() {
        return accessAuthorizationService.requirePermission(AccessPermission.MANAGE_SPACES);
    }
}
