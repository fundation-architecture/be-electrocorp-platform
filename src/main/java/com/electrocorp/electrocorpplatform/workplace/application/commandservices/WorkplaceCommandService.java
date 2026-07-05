package com.electrocorp.electrocorpplatform.workplace.application.commandservices;

import com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.DeviceAssignment;
import com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.Location;
import com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.Room;
import com.electrocorp.electrocorpplatform.workplace.domain.model.commands.AssignDeviceCommand;
import com.electrocorp.electrocorpplatform.workplace.domain.model.commands.CreateLocationCommand;
import com.electrocorp.electrocorpplatform.workplace.domain.model.commands.CreateRoomCommand;

public interface WorkplaceCommandService {
    Location createLocation(CreateLocationCommand command);

    Room createRoom(Long userId, CreateRoomCommand command);

    DeviceAssignment assignDevice(Long userId, AssignDeviceCommand command);

    Location updateLocation(Long userId, Long locationId, String name, String address, String type);

    void deleteLocation(Long userId, Long locationId);

    Room updateRoom(Long userId, Long roomId, Long locationId, String name, String floor);

    void deleteRoom(Long userId, Long roomId);

    DeviceAssignment moveAssignment(Long userId, Long assignmentId, Long locationId, Long roomId);

    void deleteAssignment(Long userId, Long assignmentId);
}
