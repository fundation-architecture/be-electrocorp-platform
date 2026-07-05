package com.electrocorp.electrocorpplatform.devicecontrol.application.commandservices;

import com.electrocorp.electrocorpplatform.devicecontrol.application.results.DeviceGroupDetails;
import com.electrocorp.electrocorpplatform.devicecontrol.application.results.OperationModeActivationResult;
import com.electrocorp.electrocorpplatform.devicecontrol.application.results.RoutineDetails;
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

public interface DeviceControlCommandService {
    Device handle(CreateDeviceCommand command);
    Device handle(PairDeviceCommand command);
    Device handle(UpdateDeviceCommand command);
    Device handle(ToggleDeviceCommand command);
    Device handle(Long deviceId, Long userId, UpdateDeviceStatusCommand command);
    void handle(DeleteDeviceCommand command);
    RoutineDetails handle(CreateRoutineCommand command);
    RoutineDetails handle(Long routineId, Long userId, UpdateRoutineStatusCommand command);
    RoutineDetails handle(ExecuteRoutineCommand command);
    void handle(DeleteRoutineCommand command);
    DeviceGroupDetails handle(CreateDeviceGroupCommand command);
    DeviceGroupDetails handle(Long userId, Long groupId, UpdateDeviceGroupCommand command);
    void handle(Long userId, Long groupId, ExecuteGroupActionCommand command);
    void handle(DeleteDeviceGroupCommand command);
    OperationMode handle(CreateOperationModeCommand command);
    OperationModeActivationResult handle(ActivateOperationModeCommand command);
    void handle(ArchiveOperationModeCommand command);
}
