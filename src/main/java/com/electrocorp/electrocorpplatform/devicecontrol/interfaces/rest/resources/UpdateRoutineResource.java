package com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineAction;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineRepeatType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cambios de una programacion existente (US-12). Los campos omitidos conservan su valor.")
public record UpdateRoutineResource(
        @Schema(description = "Nuevo nombre de la programacion.", example = "Apagar la terma")
        String name,
        @Schema(description = "Nueva accion programada.", example = "TURN_OFF")
        RoutineAction action,
        @Schema(description = "Nueva hora en formato HH:mm, hora local de la plataforma (America/Lima).", example = "19:00")
        String time,
        @Schema(description = "Nueva repeticion.", example = "DAILY")
        RoutineRepeatType repeatType,
        @Schema(description = "Dias para WEEKLY, separados por comas.", example = "MON,WED,FRI")
        String daysOfWeek,
        @Schema(description = "Intervalo en dias para CUSTOM_INTERVAL.", example = "2")
        Integer intervalDays,
        @Schema(description = "Fecha de inicio yyyy-MM-dd.", example = "2026-11-27")
        String startsOn
) {
}
