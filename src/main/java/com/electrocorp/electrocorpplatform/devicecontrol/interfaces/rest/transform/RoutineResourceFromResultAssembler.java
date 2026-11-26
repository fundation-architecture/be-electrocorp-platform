package com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.transform;

import com.electrocorp.electrocorpplatform.devicecontrol.application.results.RoutineDetails;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.RoutineResource;

public class RoutineResourceFromResultAssembler {
    public static RoutineResource toResourceFromResult(RoutineDetails details) {
        var routine = details.routine();
        return RoutineResource.from(
                routine,
                details.targetName(),
                details.applicableDeviceCount(),
                details.blockedDeviceCount()
        );
    }
}
