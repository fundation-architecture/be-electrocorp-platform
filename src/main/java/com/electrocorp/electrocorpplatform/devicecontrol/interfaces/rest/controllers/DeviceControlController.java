package com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.controllers;

import com.electrocorp.electrocorpplatform.devicecontrol.application.commandservices.DeviceControlCommandService;
import com.electrocorp.electrocorpplatform.devicecontrol.application.queryservices.DeviceControlQueryService;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.DeleteDeviceCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.DeleteDeviceGroupCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.DeleteRoutineCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.ExecuteRoutineCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.UpdateRoutineCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.UpdateRoutineResource;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.ActivateOperationModeCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.ArchiveOperationModeCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.ToggleDeviceCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.queries.GetDeviceGroupsQuery;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.queries.GetDevicesQuery;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.queries.GetOperationModesQuery;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.queries.GetRoutinesQuery;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.queries.PreviewOperationModeQuery;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.CreateDeviceGroupResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.CreateDeviceResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.CreateOperationModeResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.CreateRoutineResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.DeviceGroupResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.DeviceResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.ExecuteGroupActionResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.OperationModeActivationResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.OperationModePreviewResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.OperationModeResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.PairDeviceResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.RoutineResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.UpdateRoutineStatusResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.UpdateDeviceResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.UpdateDeviceStatusResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.UpdateDeviceGroupResource;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.transform.*;
import com.electrocorp.electrocorpplatform.iam.application.security.AccessAuthorizationService;
import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Device Control", description = "Dispositivos, control remoto y programaciones de encendido y apagado")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DeviceControlController {

    private final DeviceControlCommandService commandService;
    private final DeviceControlQueryService queryService;
    private final AccessAuthorizationService accessAuthorizationService;

    @GetMapping("/devices")
    public List<DeviceResource> getDevices() {
        return queryService.handle(new GetDevicesQuery(requireDeviceControl())).stream()
                .map(DeviceResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @PostMapping("/devices")
    public DeviceResource createDevice(@Valid @RequestBody CreateDeviceResource request) {
        var command = CreateDeviceCommandFromResourceAssembler.toCommandFromResource(request, requireDeviceManagement());
        var device = commandService.handle(command);
        return DeviceResourceFromEntityAssembler.toResourceFromEntity(device);
    }

    @PostMapping("/devices/pairings")
    public DeviceResource pairDevice(@Valid @RequestBody PairDeviceResource request) {
        var command = PairDeviceCommandFromResourceAssembler.toCommandFromResource(request, requireDeviceManagement());
        var device = commandService.handle(command);
        return DeviceResourceFromEntityAssembler.toResourceFromEntity(device);
    }

    @PatchMapping("/devices/{deviceId}/toggle")
    public DeviceResource toggleDevice(
            @PathVariable Long deviceId
    ) {
        var device = commandService.handle(new ToggleDeviceCommand(requireDeviceControl(), deviceId));
        return DeviceResourceFromEntityAssembler.toResourceFromEntity(device);
    }

    @PatchMapping("/devices/{deviceId}")
    public DeviceResource updateDevice(
            @PathVariable Long deviceId,
            @Valid @RequestBody UpdateDeviceResource request
    ) {
        var command = UpdateDeviceCommandFromResourceAssembler.toCommandFromResource(
                request,
                requireDeviceManagement(),
                deviceId
        );
        var device = commandService.handle(command);
        return DeviceResourceFromEntityAssembler.toResourceFromEntity(device);
    }

    @DeleteMapping("/devices/{deviceId}")
    public void deleteDevice(
            @PathVariable Long deviceId
    ) {
        commandService.handle(new DeleteDeviceCommand(requireDeviceManagement(), deviceId));
    }

    @Operation(summary = "Listar las programaciones del usuario", description = "Devuelve las rutinas de encendido y apagado con su hora local (timeZone), la ultima ejecucion (lastExecutedAt) y su resultado (lastExecutionStatus: EXECUTED, PARTIAL o NOT_APPLIED). US-11.")
    @GetMapping("/routines")
    public List<RoutineResource> getRoutines() {
        return queryService.handle(new GetRoutinesQuery(requireRoutineManagement())).stream()
                .map(RoutineResourceFromResultAssembler::toResourceFromResult)
                .toList();
    }

    @Operation(summary = "Registrar una programacion de encendido o apagado", description = "Registra una rutina para un dispositivo, grupo, ambiente o sede: accion TURN_ON o TURN_OFF, hora HH:mm en la zona de la plataforma (America/Lima) y repeticion ONCE, DAILY, WEEKLY o CUSTOM_INTERVAL. Responde 409 si ya existe una rutina con el mismo objetivo, hora y accion. US-11.")
    @PostMapping("/routines")
    public RoutineResource createRoutine(
            @Valid @RequestBody CreateRoutineResource request
    ) {
        var command = CreateRoutineCommandFromResourceAssembler.toCommandFromResource(request, requireRoutineManagement());
        var routine = commandService.handle(command);
        return RoutineResourceFromResultAssembler.toResourceFromResult(routine);
    }

    @Operation(summary = "Modificar una programacion existente", description = "Cambia la hora, la accion o la repeticion de una rutina del usuario; los campos omitidos se conservan. La nueva hora rige desde el cambio: una hora ya pasada del dia espera al dia siguiente. Responde 404 si la rutina no pertenece al usuario. US-12.")
    @PatchMapping("/routines/{routineId}")
    public RoutineResource updateRoutine(
            @PathVariable Long routineId,
            @RequestBody UpdateRoutineResource request
    ) {
        var routine = commandService.handle(new UpdateRoutineCommand(
                requireRoutineManagement(),
                routineId,
                request.name(),
                request.action(),
                request.time(),
                request.repeatType(),
                request.daysOfWeek(),
                request.intervalDays(),
                request.startsOn()
        ));
        return RoutineResourceFromResultAssembler.toResourceFromResult(routine);
    }

    @Operation(summary = "Activar o desactivar una programacion", description = "Habilita o deshabilita la rutina sin cambiar su horario.")
    @PatchMapping("/routines/{routineId}/status")
    public RoutineResource updateRoutineStatus(
            @PathVariable Long routineId,
            @RequestBody UpdateRoutineStatusResource request
    ) {
        var command = UpdateRoutineStatusCommandFromResourceAssembler.toCommandFromResource(request);
        var routine = commandService.handle(routineId, requireRoutineManagement(), command);
        return RoutineResourceFromResultAssembler.toResourceFromResult(routine);
    }

    @Operation(summary = "Eliminar una programacion", description = "Elimina la rutina indicada del usuario autenticado.")
    @DeleteMapping("/routines/{routineId}")
    public void deleteRoutine(
            @PathVariable Long routineId
    ) {
        commandService.handle(new DeleteRoutineCommand(requireRoutineManagement(), routineId));
    }

    @Operation(summary = "Ejecutar una programacion ahora", description = "Aplica de inmediato la accion de la rutina a sus dispositivos disponibles, por el mismo flujo de comandos que usa el planificador.")
    @PatchMapping("/routines/{routineId}/execute")
    public RoutineResource executeRoutine(
            @PathVariable Long routineId
    ) {
        var routine = commandService.handle(new ExecuteRoutineCommand(requireRoutineManagement(), routineId));
        return RoutineResourceFromResultAssembler.toResourceFromResult(routine);
    }

    @GetMapping("/device-groups")
    public List<DeviceGroupResource> getDeviceGroups() {
        return queryService.handle(new GetDeviceGroupsQuery(requireRoutineManagement())).stream()
                .map(DeviceGroupResourceFromResultAssembler::toResourceFromResult)
                .toList();
    }

    @PostMapping("/device-groups")
    public DeviceGroupResource createDeviceGroup(
            @Valid @RequestBody CreateDeviceGroupResource request
    ) {
        var command = CreateDeviceGroupCommandFromResourceAssembler.toCommandFromResource(request, requireRoutineManagement());
        var group = commandService.handle(command);
        return DeviceGroupResourceFromResultAssembler.toResourceFromResult(group);
    }

    @PatchMapping("/device-groups/{groupId}")
    public DeviceGroupResource updateDeviceGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateDeviceGroupResource request
    ) {
        var command = UpdateDeviceGroupCommandFromResourceAssembler.toCommandFromResource(request);
        var group = commandService.handle(requireRoutineManagement(), groupId, command);
        return DeviceGroupResourceFromResultAssembler.toResourceFromResult(group);
    }

    @PatchMapping("/device-groups/{groupId}/execute")
    public void executeGroupAction(
            @PathVariable Long groupId,
            @Valid @RequestBody ExecuteGroupActionResource request
    ) {
        var command = ExecuteGroupActionCommandFromResourceAssembler.toCommandFromResource(request);
        commandService.handle(requireDeviceControl(), groupId, command);
    }

    @DeleteMapping("/device-groups/{groupId}")
    public void deleteDeviceGroup(@PathVariable Long groupId) {
        commandService.handle(new DeleteDeviceGroupCommand(requireRoutineManagement(), groupId));
    }
    @PatchMapping("/devices/{deviceId}/status")
    public DeviceResource updateDeviceStatus(
            @PathVariable Long deviceId,
            @Valid @RequestBody UpdateDeviceStatusResource request
    ) {
        var command = UpdateDeviceStatusCommandFromResourceAssembler.toCommandFromResource(request);
        var device = commandService.handle(deviceId, requireDeviceControl(), command);
        return DeviceResourceFromEntityAssembler.toResourceFromEntity(device);
    }

    @GetMapping("/operation-modes")
    public List<OperationModeResource> getOperationModes() {
        return queryService.handle(new GetOperationModesQuery(requireRoutineManagement())).stream()
                .map(OperationModeResource::from)
                .toList();
    }

    @PostMapping("/operation-modes")
    public OperationModeResource createOperationMode(@Valid @RequestBody CreateOperationModeResource request) {
        var command = CreateOperationModeCommandFromResourceAssembler.toCommandFromResource(request, requireRoutineManagement());
        var mode = commandService.handle(command);
        return OperationModeResource.from(mode);
    }

    @GetMapping("/operation-modes/{modeId}/preview")
    public OperationModePreviewResource previewOperationMode(
            @PathVariable Long modeId
    ) {
        var preview = queryService.handle(new PreviewOperationModeQuery(requireRoutineManagement(), modeId));
        return OperationModePreviewResource.from(preview);
    }

    @PatchMapping("/operation-modes/{modeId}/activate")
    public OperationModeActivationResource activateOperationMode(
            @PathVariable Long modeId
    ) {
        var result = commandService.handle(new ActivateOperationModeCommand(requireRoutineManagement(), modeId));
        return OperationModeActivationResource.from(result);
    }

    @DeleteMapping("/operation-modes/{modeId}")
    public void archiveOperationMode(
            @PathVariable Long modeId
    ) {
        commandService.handle(new ArchiveOperationModeCommand(requireRoutineManagement(), modeId));
    }

    private Long requireDeviceControl() {
        return accessAuthorizationService.requirePermission(AccessPermission.CONTROL_DEVICES);
    }

    private Long requireDeviceManagement() {
        return accessAuthorizationService.requirePermission(AccessPermission.MANAGE_DEVICES);
    }

    private Long requireRoutineManagement() {
        return accessAuthorizationService.requirePermission(AccessPermission.MANAGE_ROUTINES);
    }
}
