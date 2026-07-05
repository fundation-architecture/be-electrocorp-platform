@energy-monitoring
Feature: Energy Monitoring
  As a registered user
  I want to monitor real-time and historical energy consumption
  So that I can identify opportunities to save energy

  # Covers: US-15 (Consumo en tiempo real), US-35 (Simular conexion / telemetria),
  #         US-36 (Filtrar historial por fechas)
  # TS-10 (Telemetria @Scheduled), TS-14 (Datos para graficas de consumo)

  Background:
    Given the RESTful API is available at base path "/api/v1/energy-readings"
    And the client is authenticated as "carlos.mendoza@example.com"

  @US-15 @TS-14 @dashboard
  Scenario: Retrieving the energy dashboard summary
    Given the user has energy readings for the current period
    When the client sends a GET request to "/dashboard-summary"
    Then the response status code is 200
    And the response body contains a numeric "totalWatts"
    And the response body contains a "series" array usable for charting

  @US-35 @TS-10 @telemetry
  Scenario: Simulated telemetry produces readings over time
    Given the scheduled energy sampler is enabled
    And a device with id 10 is "ON"
    When the sampler runs its scheduled cycle
    Then a new energy reading is stored for device 10
    And the reading is included in the next "/dashboard-summary" response

  @US-36 @sampling
  Scenario: Retrieving the current sampling settings
    When the client sends a GET request to "/sampling-settings"
    Then the response status code is 200
    And the response body contains a numeric "intervalSeconds"

  @US-36 @sampling
  Scenario: Updating the sampling interval
    When the client sends a PATCH request to "/sampling-settings" with body:
      """
      { "intervalSeconds": 30 }
      """
    Then the response status code is 200
    And the response body contains "intervalSeconds" equal to "30"
