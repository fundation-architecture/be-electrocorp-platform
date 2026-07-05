@notifications
Feature: Notifications and Alerts
  As a registered user
  I want to configure alert rules and receive in-app alerts
  So that I am warned about unusual energy consumption

  # Covers: US-17 (Alertas por alto consumo), US-29 (Alertas - entregadas in-app),
  #         US-34 (Configurar preferencias de notificaciones)
  # NOTE: Alerts are delivered in-app. Email delivery (Mailchimp) is not integrated
  #       in this version.

  Background:
    Given the RESTful API is available at base path "/api/v1"
    And the client is authenticated as "carlos.mendoza@example.com"

  @US-17 @alert-rule
  Scenario: Creating a high-consumption alert rule
    When the client sends a POST request to "/alerts/rules" with body:
      """
      {
        "name": "High consumption",
        "thresholdWatts": 500,
        "comparison": "GREATER_THAN"
      }
      """
    Then the response status code is 201
    And the response body contains "thresholdWatts" equal to "500"

  @US-17 @alert
  Scenario: Evaluating rules generates an alert when the threshold is exceeded
    Given an enabled alert rule with threshold 500 watts exists
    And a device sustains a consumption of 650 watts
    When the client sends a POST request to "/alerts/rules/evaluate"
    Then the response status code is 200
    And a new alert is present when listing "/alerts"

  @US-17 @alert
  Scenario: Listing active alerts
    Given the user has 2 unread alerts
    When the client sends a GET request to "/alerts"
    Then the response status code is 200
    And the response body is a list with 2 items

  @US-29 @alert
  Scenario: Marking an alert as read
    Given an alert with id 8 exists and is unread
    When the client sends a PATCH request to "/alerts/8/read"
    Then the response status code is 200
    And the response body contains "read" equal to "true"

  @US-29 @alert
  Scenario: Resolving an alert
    Given an alert with id 8 exists and is unread
    When the client sends a PATCH request to "/alerts/8/resolve"
    Then the response status code is 200
    And the response body contains "status" equal to "RESOLVED"

  @US-17 @alert-rule
  Scenario: Toggling an alert rule on and off
    Given an alert rule with id 4 exists and is enabled
    When the client sends a PATCH request to "/alerts/rules/4/toggle"
    Then the response status code is 200
    And the response body contains "enabled" equal to "false"

  @US-34 @preferences
  Scenario: Reading the notification preferences
    When the client sends a GET request to "/notifications/preferences"
    Then the response status code is 200
    And the response body contains a boolean "monthlyReportEmails"

  @US-34 @preferences
  Scenario: Disabling monthly report emails from the preferences
    When the client sends a PUT request to "/notifications/preferences" with body:
      """
      { "monthlyReportEmails": false }
      """
    Then the response status code is 200
    And the response body contains "monthlyReportEmails" equal to "false"
