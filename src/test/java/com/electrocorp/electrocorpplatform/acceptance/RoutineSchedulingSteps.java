package com.electrocorp.electrocorpplatform.acceptance;

import com.electrocorp.electrocorpplatform.devicecontrol.application.services.DeviceControlApplicationService;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Routine;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.RoutineRepository;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.services.RoutineSchedulePolicyService;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pasos de las pruebas de aceptacion de US-11 y US-12 (programacion y modificacion de horarios).
 * La llegada de la hora programada se simula invocando la misma operacion que usa el planificador
 * (executeScheduledRoutine) con la fecha y hora del escenario, en lugar de esperar al reloj real.
 */
public class RoutineSchedulingSteps {

    private static final String DEVICE_NAME = "Terma electrica";

    @Autowired
    private SprintTwoApiClient api;

    @Autowired
    private DeviceControlApplicationService deviceControlApplicationService;

    @Autowired
    private RoutineRepository routineRepository;

    @Autowired
    private RoutineSchedulePolicyService routineSchedulePolicyService;

    private String token;
    private Long deviceId;
    private JsonNode routine;
    private HttpResponse<String> lastResponse;

    @Dado("que el usuario tiene un dispositivo vinculado y encendido")
    public void userHasALinkedDeviceTurnedOn() {
        token = api.registerAndSignInNewUser();
        deviceId = api.createDevice(token, DEVICE_NAME, 1500);
        api.turnOn(token, deviceId);
        assertThat(deviceStatus()).isEqualTo("ON");
    }

    @Cuando("registra una programación de apagado para las {word}")
    public void registersATurnOffSchedule(String time) {
        lastResponse = createRoutine("TURN_OFF", time);
        if (lastResponse.statusCode() == 200) {
            routine = api.json(lastResponse);
        }
    }

    @Entonces("el sistema guarda la programación con la zona horaria {string}")
    public void savesTheScheduleWithTheTimeZone(String timeZone) {
        assertThat(lastResponse.statusCode()).isEqualTo(200);
        assertThat(routine.get("deviceId").asLong()).isEqualTo(deviceId);
        assertThat(routine.get("action").asText()).isEqualTo("TURN_OFF");
        assertThat(routine.get("enabled").asBoolean()).isTrue();
        assertThat(routine.get("timeZone").asText()).isEqualTo(timeZone);
    }

    @Cuando("llega el horario programado de las {word}")
    public void theScheduledTimeArrives(String time) {
        routine = api.json(api.get(token, "/api/v1/routines")).get(0);
        deviceControlApplicationService.executeScheduledRoutine(
                routine.get("userId").asLong(),
                routine.get("id").asLong(),
                LocalDate.now().atTime(LocalTime.parse(time))
        );
        routine = api.json(api.get(token, "/api/v1/routines")).get(0);
    }

    @Entonces("el sistema apaga el dispositivo y registra la ejecución como {string}")
    public void turnsOffTheDeviceAndRecordsTheExecution(String status) {
        assertThat(deviceStatus()).isEqualTo("OFF");
        assertThat(routine.get("lastExecutionStatus").asText()).isEqualTo(status);
        assertThat(routine.get("lastExecutedAt").asText()).isNotBlank();
    }

    @Y("el dispositivo pasa a mantenimiento")
    public void theDeviceGoesIntoMaintenance() {
        HttpResponse<String> response = api.send("PATCH", "/api/v1/devices/" + deviceId + "/status", token,
                "{\"status\":\"MAINTENANCE\"}");
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Entonces("el sistema registra la ejecución como {string} sin informar el apagado")
    public void recordsTheExecutionWithoutReportingTheTurnOff(String status) {
        assertThat(routine.get("lastExecutionStatus").asText()).isEqualTo(status);
        assertThat(deviceStatus()).isEqualTo("MAINTENANCE");
    }

    @Entonces("el sistema rechaza la programación duplicada")
    public void rejectsTheDuplicatedSchedule() {
        assertThat(lastResponse.statusCode()).isEqualTo(409);
        assertThat(api.json(api.get(token, "/api/v1/routines"))).hasSize(1);
    }

    private HttpResponse<String> createRoutine(String action, String time) {
        return api.send("POST", "/api/v1/routines", token, """
                {
                  "deviceId": %d,
                  "targetType": "DEVICE",
                  "targetId": %d,
                  "name": "Programacion de la terma",
                  "action": "%s",
                  "time": "%s",
                  "repeatType": "DAILY"
                }
                """.formatted(deviceId, deviceId, action, time));
    }

    // ---------- US-12 · Modificación de programación existente ----------

    @Y("tiene una programación de {word} para las {word}")
    public void hasASchedule(String action, String time) {
        HttpResponse<String> response = createRoutine(actionCode(action), time);
        assertThat(response.statusCode()).isEqualTo(200);
        routine = api.json(response);
    }

    @Cuando("cambia el horario a las {word}")
    public void changesTheTime(String time) {
        lastResponse = updateRoutine(token, "{\"time\":\"%s\"}".formatted(time));
    }

    @Cuando("cambia la acción programada a {word}")
    public void changesTheAction(String action) {
        lastResponse = updateRoutine(token, "{\"action\":\"%s\"}".formatted(actionCode(action)));
    }

    @Entonces("el sistema guarda la programación con la hora {word} y la acción {word}")
    public void savesTheChanges(String time, String action) {
        assertThat(lastResponse.statusCode()).isEqualTo(200);
        JsonNode updated = api.json(lastResponse);
        assertThat(updated.get("id").asLong()).isEqualTo(routine.get("id").asLong());
        assertThat(updated.get("time").asText()).isEqualTo(time);
        assertThat(updated.get("action").asText()).isEqualTo(actionCode(action));
    }

    @Y("el planificador no ejecuta la programación a las {word} del día siguiente")
    public void theSchedulerDoesNotRunAt(String time) {
        assertThat(runSchedulerAt(time)).isFalse();
        assertThat(deviceStatus()).isEqualTo("ON");
    }

    @Y("el planificador ejecuta la programación a las {word} del día siguiente y el dispositivo queda {word}")
    public void theSchedulerRunsAt(String time, String expectedStatus) {
        assertThat(runSchedulerAt(time)).isTrue();
        assertThat(deviceStatus()).isEqualTo(expectedStatus);
    }

    @Cuando("otro usuario intenta cambiar el horario de esa programación a las {word}")
    public void anotherUserTriesToChangeTheSchedule(String time) {
        String otherToken = api.registerAndSignInNewUser();
        lastResponse = updateRoutine(otherToken, "{\"time\":\"%s\"}".formatted(time));
    }

    @Entonces("el sistema rechaza la operación y conserva la hora {word}")
    public void rejectsTheOperation(String originalTime) {
        assertThat(lastResponse.statusCode()).isEqualTo(404);
        JsonNode stored = api.json(api.get(token, "/api/v1/routines")).get(0);
        assertThat(stored.get("time").asText()).isEqualTo(originalTime);
    }

    /**
     * Aplica la misma regla que RoutineSchedulerService: la rutina se ejecuta solo si
     * RoutineSchedulePolicyService la considera pendiente a esa hora.
     */
    private boolean runSchedulerAt(String time) {
        Long userId = routine.get("userId").asLong();
        Long routineId = routine.get("id").asLong();
        LocalDateTime moment = LocalDate.now().plusDays(1).atTime(LocalTime.parse(time));
        Routine stored = routineRepository.findByIdAndUserId(routineId, userId).orElseThrow();
        if (!routineSchedulePolicyService.isDue(stored, moment)) {
            return false;
        }
        deviceControlApplicationService.executeScheduledRoutine(userId, routineId, moment);
        return true;
    }

    private HttpResponse<String> updateRoutine(String userToken, String body) {
        return api.send("PATCH", "/api/v1/routines/" + routine.get("id").asLong(), userToken, body);
    }

    private String actionCode(String action) {
        return switch (action) {
            case "apagado" -> "TURN_OFF";
            case "encendido" -> "TURN_ON";
            default -> action;
        };
    }

    private String deviceStatus() {
        for (JsonNode device : api.json(api.get(token, "/api/v1/devices"))) {
            if (device.get("id").asLong() == deviceId) {
                return device.get("status").asText();
            }
        }
        throw new IllegalStateException("Device not found: " + deviceId);
    }
}
