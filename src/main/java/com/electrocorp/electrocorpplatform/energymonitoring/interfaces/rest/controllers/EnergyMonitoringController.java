package com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.controllers;

import com.electrocorp.electrocorpplatform.energymonitoring.application.commandservices.EnergyMonitoringCommandService;
import com.electrocorp.electrocorpplatform.energymonitoring.application.queryservices.EnergyMonitoringQueryService;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.commands.UpdateEnergySamplingSettingsCommand;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.queries.GetEnergyDashboardSummaryQuery;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.queries.GetEnergyReadingsByUserQuery;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.queries.GetEnergySamplingSettingsQuery;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.CreateEnergyReadingResource;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.EnergyDashboardSummaryResource;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.EnergyReadingResource;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.UpdateEnergySamplingSettingsResource;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.EnergySamplingSettingsResource;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.transform.CreateEnergyReadingCommandFromResourceAssembler;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.transform.EnergyReadingResourceFromEntityAssembler;
import com.electrocorp.electrocorpplatform.iam.application.security.AccessAuthorizationService;
import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Tag(name = "Energy Monitoring", description = "Lecturas de consumo, resumen del panel, costo en soles y proyeccion mensual")
@RestController
@RequestMapping("/api/v1/energy-readings")
@RequiredArgsConstructor
public class EnergyMonitoringController {

    private final EnergyMonitoringCommandService commandService;
    private final EnergyMonitoringQueryService queryService;
    private final AccessAuthorizationService accessAuthorizationService;

    @Operation(summary = "Listar las lecturas de consumo", description = "Devuelve las lecturas del usuario; acepta recordedAt_gte y recordedAt_lte (yyyy-MM-dd). Es una consulta de solo lectura (CQRS).")
    @GetMapping
    public List<EnergyReadingResource> getReadings(
            @RequestParam(name = "recordedAt_gte", required = false) String recordedAtGte,
            @RequestParam(name = "recordedAt_lte", required = false) String recordedAtLte
    ) {
        Long userId = requireEnergyAccess();
        if (recordedAtGte != null && recordedAtLte != null) {
            return queryService.handle(new GetEnergyReadingsByUserQuery(
                    userId,
                    parseStart(recordedAtGte),
                    parseEnd(recordedAtLte)
            )).stream()
                    .map(EnergyReadingResourceFromEntityAssembler::toResourceFromEntity)
                    .toList();
        }

        return queryService.handle(new GetEnergyReadingsByUserQuery(userId, null, null)).stream()
                .map(EnergyReadingResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @Operation(summary = "Consultar el resumen del panel de consumo", description = "Devuelve currentWatts, todayKilowattHours, todayEstimatedCost, monthToDateCost, projectedMonthlyCost y costPerHour en soles (US-13, US-16, US-18). Se sirve desde Redis durante 30 s (QA-02).")
    @GetMapping("/dashboard-summary")
    public EnergyDashboardSummaryResource getDashboardSummary() {
        return queryService.handle(new GetEnergyDashboardSummaryQuery(requireEnergyAccess()));
    }

    @Operation(summary = "Consultar el intervalo de muestreo", description = "Devuelve cada cuantos segundos el planificador registra lecturas de los dispositivos encendidos.")
    @GetMapping("/sampling-settings")
    public EnergySamplingSettingsResource getSamplingSettings() {
        requireEnergyAccess();
        return new EnergySamplingSettingsResource(queryService.handle(new GetEnergySamplingSettingsQuery()));
    }

    @Operation(summary = "Actualizar el intervalo de muestreo", description = "Cambia el intervalo del planificador de ingesta (entre 5 y 3600 segundos).")
    @PatchMapping("/sampling-settings")
    public EnergySamplingSettingsResource updateSamplingSettings(
            @Valid @RequestBody UpdateEnergySamplingSettingsResource request
    ) {
        requireEnergyAccess();
        return new EnergySamplingSettingsResource(
                commandService.handle(new UpdateEnergySamplingSettingsCommand(request.sampleSeconds()))
        );
    }

    @Operation(summary = "Registrar una lectura de consumo", description = "Registra una lectura del dispositivo (sampleSeconds opcional para calcular kWh y costo). Si supera el limite configurado publica EnergyThresholdExceededEvent (US-20) e invalida la cache del resumen.")
    @PostMapping
    public EnergyReadingResource createReading(@Valid @RequestBody CreateEnergyReadingResource request) {
        var command = CreateEnergyReadingCommandFromResourceAssembler.toCommandFromResource(request, requireEnergyAccess());
        var reading = commandService.handle(command);
        return EnergyReadingResourceFromEntityAssembler.toResourceFromEntity(reading);
    }

    private Long requireEnergyAccess() {
        return accessAuthorizationService.requirePermission(AccessPermission.VIEW_ENERGY);
    }

    private LocalDateTime parseStart(String value) {
        return LocalDate.parse(value).atStartOfDay();
    }

    private LocalDateTime parseEnd(String value) {
        return LocalDate.parse(value).atTime(LocalTime.MAX);
    }
}
