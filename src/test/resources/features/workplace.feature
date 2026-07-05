@workplace
Feature: Workplace Management
  As a registered user or business owner
  I want to organize my locations, rooms and device assignments
  So that I can manage the physical spaces where my devices operate

  # Covers: US-40 (Gestionar multiples locales), US-25 (Consumo por area),
  #         US-38 support (Agrupar por habitaciones)

  Background:
    Given the RESTful API is available at base path "/api/v1/workplace"
    And the client is authenticated as "carlos.mendoza@example.com"

  @US-40 @location
  Scenario: Creating a location
    When the client sends a POST request to "/locations" with body:
      """
      {
        "name": "Local A",
        "address": "Av. Los Proceres 123",
        "latitude": -12.0464,
        "longitude": -77.0428
      }
      """
    Then the response status code is 201
    And the response body contains "name" equal to "Local A"

  @US-40 @location
  Scenario: Listing all locations of the user
    Given the user owns 2 locations
    When the client sends a GET request to "/locations"
    Then the response status code is 200
    And the response body is a list with 2 items

  @US-40 @location
  Scenario: Updating a location
    Given a location with id 1 exists
    When the client sends a PATCH request to "/locations/1" with body:
      """
      { "name": "Local A - Renovado" }
      """
    Then the response status code is 200
    And the response body contains "name" equal to "Local A - Renovado"

  @US-40 @location
  Scenario: Deleting a location
    Given a location with id 1 exists
    When the client sends a DELETE request to "/locations/1"
    Then the response status code is 204

  @US-25 @room
  Scenario: Creating a room inside a location
    Given a location with id 1 exists
    When the client sends a POST request to "/rooms" with body:
      """
      {
        "locationId": 1,
        "name": "Almacen"
      }
      """
    Then the response status code is 201
    And the response body contains "name" equal to "Almacen"

  @US-25 @assignment
  Scenario: Assigning a device to a room
    Given a room with id 2 exists
    And a device with id 10 exists
    When the client sends a POST request to "/device-assignments" with body:
      """
      {
        "roomId": 2,
        "deviceId": 10
      }
      """
    Then the response status code is 201
    And the response body links device 10 to room 2
