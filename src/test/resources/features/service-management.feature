@service-management
Feature: Service Management
  As a registered user
  I want to open support and maintenance tickets
  So that I can get help and keep my devices operational

  # Covers: Gestion de servicios y soporte (Service Management bounded context)

  Background:
    Given the RESTful API is available at base path "/api/v1"
    And the client is authenticated as "carlos.mendoza@example.com"

  @support-ticket
  Scenario: Opening a support ticket
    When the client sends a POST request to "/support-tickets" with body:
      """
      {
        "subject": "No puedo vincular mi dispositivo",
        "description": "El enchufe no aparece en la lista tras ingresar el codigo."
      }
      """
    Then the response status code is 201
    And the response body contains "status" equal to "OPEN"

  @support-ticket
  Scenario: Listing support tickets
    Given the user has 1 open support ticket
    When the client sends a GET request to "/support-tickets"
    Then the response status code is 200
    And the response body is a list with 1 item

  @support-ticket
  Scenario: Changing the status of a support ticket
    Given a support ticket with id 3 exists with status "OPEN"
    When the client sends a PATCH request to "/support-tickets/3/status" with body:
      """
      { "status": "IN_PROGRESS" }
      """
    Then the response status code is 200
    And the response body contains "status" equal to "IN_PROGRESS"

  @maintenance-ticket
  Scenario: Scheduling a maintenance ticket for a device
    Given a device with id 10 exists
    When the client sends a POST request to "/maintenance-tickets" with body:
      """
      {
        "deviceId": 10,
        "description": "Revision preventiva del rele"
      }
      """
    Then the response status code is 201
    And the response body contains "status" equal to "SCHEDULED"

  @maintenance-ticket
  Scenario: Closing a maintenance ticket
    Given a maintenance ticket with id 6 exists with status "SCHEDULED"
    When the client sends a PATCH request to "/maintenance-tickets/6/status" with body:
      """
      { "status": "COMPLETED" }
      """
    Then the response status code is 200
    And the response body contains "status" equal to "COMPLETED"
