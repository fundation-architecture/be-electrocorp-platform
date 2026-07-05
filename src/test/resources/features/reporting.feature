@reporting
Feature: Reporting and Energy Goals
  As a registered user
  I want to review consumption reports and set energy goals
  So that I can track and reduce my energy spending

  # Covers: US-16 (Historial de consumo), US-18 (Recomendaciones de ahorro),
  #         US-25 (Consumo por area/equipo), US-37 (Exportar historial a CSV)

  Background:
    Given the RESTful API is available at base path "/api/v1"
    And the client is authenticated as "carlos.mendoza@example.com"

  @US-25 @platform-summary
  Scenario: Retrieving the platform reporting summary
    Given the user has consumption data across several rooms
    When the client sends a GET request to "/reporting/platform/summary"
    Then the response status code is 200
    And the response body contains a "breakdown" grouped by area

  @US-18 @energy-goal
  Scenario: Creating an energy saving goal
    When the client sends a POST request to "/reports/energy-goals" with body:
      """
      {
        "title": "Reduce 10% this month",
        "targetKwh": 120.5,
        "period": "MONTHLY"
      }
      """
    Then the response status code is 201
    And the response body contains "title" equal to "Reduce 10% this month"

  @US-18 @energy-goal
  Scenario: Listing energy goals
    Given the user has 1 active energy goal
    When the client sends a GET request to "/reports/energy-goals"
    Then the response status code is 200
    And the response body is a list with 1 item

  @US-18 @energy-goal
  Scenario: Updating an energy goal target
    Given an energy goal with id 2 exists
    When the client sends a PATCH request to "/reports/energy-goals/2" with body:
      """
      { "targetKwh": 100.0 }
      """
    Then the response status code is 200
    And the response body contains "targetKwh" equal to "100.0"

  @US-16 @report
  Scenario: Deleting a stored report
    Given a report with id 9 exists
    When the client sends a DELETE request to "/reports/9"
    Then the response status code is 204

  @US-37 @export
  Scenario: Exporting energy readings as CSV
    Given the user has energy readings for the last 7 days
    When the client sends a GET request to "/energy-readings/export" accepting "text/csv"
    Then the response status code is 200
    And the response "Content-Type" header is "text/csv"
    And the response body starts with a CSV header row
