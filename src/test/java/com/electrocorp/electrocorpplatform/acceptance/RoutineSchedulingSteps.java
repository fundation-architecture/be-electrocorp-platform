package com.electrocorp.electrocorpplatform.acceptance;

import com.electrocorp.electrocorpplatform.devicecontrol.application.services.DeviceControlApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;
import java.time.LocalDate;
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

    String token() {
        return token;
    }

    Long deviceId() {
        return deviceId;
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
