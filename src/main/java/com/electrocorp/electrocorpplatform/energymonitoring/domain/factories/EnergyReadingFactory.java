package com.electrocorp.electrocorpplatform.energymonitoring.domain.factories;

import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.EnergyReadingStatus;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.valueobjects.KilowattHours;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.valueobjects.SampleSeconds;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.valueobjects.Watts;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.specifications.HighConsumptionSpecification;
import com.electrocorp.electrocorpplatform.shared.domain.valueobjects.NonBlankText;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EnergyReadingFactory {

    private final HighConsumptionSpecification highConsumptionSpecification;

    public EnergyReadingFactory() {
        this(new HighConsumptionSpecification());
    }

    public EnergyReadingFactory(HighConsumptionSpecification highConsumptionSpecification) {
        this.highConsumptionSpecification = highConsumptionSpecification;
    }

    public EnergyReading create(
            Long userId,
            Long deviceId,
            String deviceName,
            BigDecimal watts,
            BigDecimal kilowattHours,
            BigDecimal estimatedCost,
            Integer sampleSeconds,
            LocalDateTime recordedAt
    ) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id must be positive.");
        }
        if (deviceId == null || deviceId <= 0) {
            throw new IllegalArgumentException("Device id must be positive.");
        }

        Watts validWatts = new Watts(watts);
        KilowattHours validKilowattHours = new KilowattHours(kilowattHours != null ? kilowattHours : BigDecimal.ZERO);
        SampleSeconds validSampleSeconds = new SampleSeconds(sampleSeconds != null ? sampleSeconds : 0);

        EnergyReading reading = new EnergyReading();
        reading.setUserId(userId);
        reading.setDeviceId(deviceId);
        reading.setDeviceName(NonBlankText.of(deviceName).value());
        reading.setWatts(validWatts.value());
        reading.setKilowattHours(validKilowattHours.value());
        reading.setEstimatedCost(estimatedCost != null ? estimatedCost : BigDecimal.ZERO);
        reading.setSampleSeconds(validSampleSeconds.value());
        reading.setRecordedAt(recordedAt != null ? recordedAt : LocalDateTime.now());
        reading.setStatus(highConsumptionSpecification.isSatisfiedBy(validWatts.value())
                ? EnergyReadingStatus.HIGH
                : EnergyReadingStatus.NORMAL);
        return reading;
    }
}