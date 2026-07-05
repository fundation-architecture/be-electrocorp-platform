package com.electrocorp.electrocorpplatform.devicecontrol.application.internal.commandservices;

import com.electrocorp.electrocorpplatform.devicecontrol.application.commandservices.DeviceControlCommandService;
import com.electrocorp.electrocorpplatform.devicecontrol.application.results.DeviceGroupDetails;
import com.electrocorp.electrocorpplatform.devicecontrol.application.results.OperationModeActivationResult;
import com.electrocorp.electrocorpplatform.devicecontrol.application.results.RoutineDetails;
import com.electrocorp.electrocorpplatform.devicecontrol.application.services.DeviceControlApplicationService;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Device;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.OperationMode;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.ActivateOperationModeCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.ArchiveOperationModeCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.CreateDeviceCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.CreateDeviceGroupCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.CreateOperationModeCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.CreateRoutineCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.DeleteDeviceCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.DeleteDeviceGroupCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.DeleteRoutineCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.ExecuteGroupActionCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.ExecuteRoutineCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.PairDeviceCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.ToggleDeviceCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.UpdateDeviceCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.UpdateDeviceGroupCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.UpdateDeviceStatusCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.UpdateRoutineStatusCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceControlCommandServiceImpl implements DeviceControlCommandService {
    private final DeviceControlApplicationService applicationService;

    @Override
    public Device handle(CreateDeviceCommand command) {
        return applicationService.createDevice(command);
    }

    @Override
    public Device handle(PairDeviceCommand command) {
        return applicationService.pairDevice(command);
    }

    @Override
    public Device handle(UpdateDeviceCommand command) {
        return applicationService.updateDevice(command);
    }

    @Override
    public Device handle(ToggleDeviceCommand command) {
        return applicationService.toggleDevice(command.userId(), command.deviceId());
    }

    @Override
    public Device handle(Long deviceId, Long userId, UpdateDeviceStatusCommand command) {
        return applicationService.updateDeviceStatus(userId, deviceId, command);
    }

    @Override
    public void handle(DeleteDeviceCommand command) {
        applicationService.deleteDevice(command.userId(), command.deviceId());
    }

    @Override
    public RoutineDetails handle(CreateRoutineCommand command) {
        return applicationService.createRoutine(command);
    }

    @Override
    public RoutineDetails handle(Long routineId, Long userId, UpdateRoutineStatusCommand command) {
        return applicationService.updateRoutineStatus(userId, routineId, command);
    }

    @Override
    public RoutineDetails handle(ExecuteRoutineCommand command) {
        return applicationService.executeRoutine(command.userId(), command.routineId());
    }

    @Override
    public void handle(DeleteRoutineCommand command) {
        applicationService.deleteRoutine(command.userId(), command.routineId());
    }

    @Override
    public DeviceGroupDetails handle(CreateDeviceGroupCommand command) {
        return applicationService.createDeviceGroup(command);
    }

    @Override
    public DeviceGroupDetails handle(Long userId, Long groupId, UpdateDeviceGroupCommand command) {
        return applicationService.updateDeviceGroup(userId, groupId, command);
    }

    @Override
    public void handle(Long userId, Long groupId, ExecuteGroupActionCommand command) {
        applicationService.executeGroupAction(userId, groupId, command);
    }

    @Override
    public void handle(DeleteDeviceGroupCommand command) {
        applicationService.deleteDeviceGroup(command.userId(), command.deviceGroupId());
    }

    @Override
    public OperationMode handle(CreateOperationModeCommand command) {
        return applicationService.createOperationMode(command);
    }

    @Override
    public OperationModeActivationResult handle(ActivateOperationModeCommand command) {
        return applicationService.activateOperationMode(command.userId(), command.modeId());
    }

    @Override
    public void handle(ArchiveOperationModeCommand command) {
        applicationService.archiveOperationMode(command.userId(), command.modeId());
    }
}
