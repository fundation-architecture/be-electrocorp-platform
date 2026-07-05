@device-control
Feature: Device Control
  As a registered user
  I want to register, control, group and automate my smart devices
  So that I can manage my home or business remotely

  # Covers: US-08 (Emparejar/registrar dispositivo), US-10 (Nombrar), US-11 (Estado),
  #         US-12 (Encender/apagar), US-13 (Programar horarios), US-14 (Rutinas),
  #         US-19 (Administrar multiples), US-23 (Control de negocio),
  #         US-24 (Horario comercial), US-38 (Agrupar dispositivos)
  # TS-11 (Endpoints CRUD Dispositivos)

  Background:
    Given the RESTful API is available at base path "/api/v1"
    And the client is authenticated as "carlos.mendoza@example.com"

  @US-08 @TS-11 @create
  Scenario: Registering a new device
    When the client sends a POST request to "/devices" with body:
      """
      {
        "name": "Sala Lamp",
        "room": "Living Room",
        "type": "SMART_PLUG",
        "powerWatts": 60.0
      }
      """
    Then the response status code is 201
    And the response body contains a non-empty "id"
    And the response body contains "status" equal to "OFF"

  @US-08 @validation
  Scenario: Registering a device is rejected without a name
    When the client sends a POST request to "/devices" with body:
      """
      {
        "name": "",
        "type": "SMART_PLUG",
        "powerWatts": 60.0
      }
      """
    Then the response status code is 400

  @US-08 @validation
  Scenario: Registering a device is rejected with non-positive power
    When the client sends a POST request to "/devices" with body:
      """
      {
        "name": "Sala Lamp",
        "type": "SMART_PLUG",
        "powerWatts": -5.0
      }
      """
    Then the response status code is 400

  @US-19 @TS-11 @list
  Scenario: Listing all devices of the user
    Given the user has 3 registered devices
    When the client sends a GET request to "/devices"
    Then the response status code is 200
    And the response body is a list with 3 items

  @US-11 @US-12 @toggle
  Scenario: Toggling a device from OFF to ON
    Given a device with id 10 exists with status "OFF"
    When the client sends a PATCH request to "/devices/10/toggle"
    Then the response status code is 200
    And the response body contains "status" equal to "ON"

  @US-12 @status
  Scenario: Setting a device status explicitly
    Given a device with id 10 exists with status "OFF"
    When the client sends a PATCH request to "/devices/10/status" with body:
      """
      { "status": "ON" }
      """
    Then the response status code is 200
    And the response body contains "status" equal to "ON"

  @US-10 @delete
  Scenario: Deleting a device
    Given a device with id 10 exists with status "OFF"
    When the client sends a DELETE request to "/devices/10"
    Then the response status code is 204

  @US-13 @US-14 @routine
  Scenario: Creating a scheduled routine that turns a device off at night
    Given a device with id 10 exists with status "ON"
    When the client sends a POST request to "/routines" with body:
      """
      {
        "deviceId": 10,
        "targetType": "DEVICE",
        "targetId": 10,
        "name": "Dormir",
        "action": "TURN_OFF",
        "time": "23:00",
        "repeatType": "DAILY"
      }
      """
    Then the response status code is 201
    And the response body contains "name" equal to "Dormir"
    And the response body contains "action" equal to "TURN_OFF"

  @US-14 @routine
  Scenario: Enabling and disabling a routine
    Given a routine with id 5 exists and is enabled
    When the client sends a PATCH request to "/routines/5/status" with body:
      """
      { "enabled": false }
      """
    Then the response status code is 200
    And the response body contains "enabled" equal to "false"

  @US-24 @routine
  Scenario: Executing a routine on demand
    Given a routine with id 5 exists targeting device 10 which is "OFF"
    When the client sends a PATCH request to "/routines/5/execute"
    Then the response status code is 200

  @US-38 @group
  Scenario: Creating a device group with member devices
    Given devices with ids 10 and 11 exist
    When the client sends a POST request to "/device-groups" with body:
      """
      {
        "name": "Iluminacion Sala",
        "description": "Lights in the living room",
        "deviceIds": [10, 11]
      }
      """
    Then the response status code is 201
    And the response body contains "name" equal to "Iluminacion Sala"

  @US-38 @US-23 @group
  Scenario: Executing a group action turns off all member devices simultaneously
    Given a device group with id 7 contains devices 10 and 11 which are "ON"
    When the client sends a PATCH request to "/device-groups/7/execute" with body:
      """
      { "action": "TURN_OFF" }
      """
    Then the response status code is 200
    And every device in group 7 has status "OFF"

  @US-24 @operation-mode
  Scenario: Previewing an operation mode before activation
    Given an operation mode with id 3 exists
    When the client sends a GET request to "/operation-modes/3/preview"
    Then the response status code is 200
    And the response body contains a list of affected devices

  @US-24 @operation-mode
  Scenario: Activating an operation mode
    Given an operation mode with id 3 exists
    When the client sends a PATCH request to "/operation-modes/3/activate"
    Then the response status code is 200
