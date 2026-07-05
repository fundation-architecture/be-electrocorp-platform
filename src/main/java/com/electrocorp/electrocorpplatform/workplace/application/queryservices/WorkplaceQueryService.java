package com.electrocorp.electrocorpplatform.workplace.application.queryservices;

import com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.DeviceAssignment;
import com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.Location;
import com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.Room;

import java.util.List;

public interface WorkplaceQueryService {
    List<Location> getLocations(Long userId);

    List<Room> getRooms(Long userId, Long locationId);

    List<Room> getRoomsByUserId(Long userId);

    List<DeviceAssignment> getAssignments(Long userId, Long locationId);

    List<DeviceAssignment> getAssignmentsByUserId(Long userId);

    List<DeviceAssignment> getAssignmentsByDeviceId(Long userId, Long deviceId);
}
