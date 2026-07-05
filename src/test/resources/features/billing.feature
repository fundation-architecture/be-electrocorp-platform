@billing
Feature: Billing and Subscriptions
  As a registered user
  I want to browse plans, subscribe via checkout, and cancel
  So that I can access the features included in my plan

  # Covers: US-06 (Comparar planes), US-27 (Contratar plan via checkout - pago simulado interno),
  #         US-28 (Cancelar suscripcion)
  # NOTE: Direct subscription creation is disabled by design; subscriptions are
  #       created through /subscriptions/checkout. Payment is validated by an
  #       internal policy service (no external Stripe gateway).

  Background:
    Given the RESTful API is available at base path "/api/v1/billing"
    And the billing plan catalog is initialized
    And the client is authenticated as "carlos.mendoza@example.com"

  @US-06 @plans
  Scenario: Listing the available service plans
    When the client sends a GET request to "/plans"
    Then the response status code is 200
    And the response body is a list containing a plan with code "STARTER"
    And the response body is a list containing a plan with code "ENTERPRISE"

  @US-27 @checkout
  Scenario: Checking out a subscription with valid card details
    When the client sends a POST request to "/subscriptions/checkout" with body:
      """
      {
        "planCode": "ENTERPRISE",
        "holderName": "Carlos Mendoza",
        "cardNumber": "4111111111111111",
        "expirationDate": "12/29",
        "cvv": "123"
      }
      """
    Then the response status code is 200
    And the response body contains "status" equal to "ACTIVE"
    And the response body contains "planCode" equal to "ENTERPRISE"

  @US-27 @checkout @validation
  Scenario: Checkout is rejected when card details are incomplete
    When the client sends a POST request to "/subscriptions/checkout" with body:
      """
      {
        "planCode": "ENTERPRISE",
        "holderName": "Carlos Mendoza",
        "cardNumber": "",
        "expirationDate": "12/29",
        "cvv": "123"
      }
      """
    Then the response status code is 400

  @US-06 @subscription
  Scenario: Retrieving the current subscription after checkout
    Given the client has an active subscription with plan "ENTERPRISE"
    When the client sends a GET request to "/subscriptions/current"
    Then the response status code is 200
    And the response body contains "planCode" equal to "ENTERPRISE"
    And the response body contains "status" equal to "ACTIVE"

  @US-28 @cancel
  Scenario: Cancelling the current subscription
    Given the client has an active subscription with plan "ENTERPRISE"
    When the client sends a DELETE request to "/subscriptions/current"
    Then the response status code is 200

  @billing @invoices
  Scenario: Listing invoices for the authenticated user
    When the client sends a GET request to "/invoices"
    Then the response status code is 200
    And the response body is a JSON array
