@iam
Feature: Identity and Access Management
  As a visitor or registered user
  I want to sign up, authenticate and manage my account
  So that I can access the ElectroCorp platform securely

  # Covers: US-07 (Registro), US-31 (Recuperar contraseña),
  #         US-32 (Editar perfil), US-33 (Eliminar cuenta),
  #         TS-13 (JWT filter), TS-15 (BCrypt password hashing)

  Background:
    Given the RESTful API is available at base path "/api/v1"

  @US-07 @signup
  Scenario: Successful user registration
    Given no account exists for email "rosa.gutierrez@example.com"
    When the client sends a POST request to "/auth/sign-up" with body:
      """
      {
        "fullName": "Rosa Gutierrez",
        "email": "rosa.gutierrez@example.com",
        "password": "Secur3Pass"
      }
      """
    Then the response status code is 200
    And the response body contains a non-empty "token"
    And the response body contains "user.email" equal to "rosa.gutierrez@example.com"

  @US-07 @signup @validation
  Scenario: Registration is rejected for an invalid email
    When the client sends a POST request to "/auth/sign-up" with body:
      """
      {
        "fullName": "Rosa Gutierrez",
        "email": "not-an-email",
        "password": "Secur3Pass"
      }
      """
    Then the response status code is 400

  @US-07 @signup @validation
  Scenario: Registration is rejected for a password shorter than 8 characters
    When the client sends a POST request to "/auth/sign-up" with body:
      """
      {
        "fullName": "Rosa Gutierrez",
        "email": "rosa.short@example.com",
        "password": "123"
      }
      """
    Then the response status code is 400

  @US-07 @signup @conflict
  Scenario: Registration is rejected when the email is already in use
    Given an account already exists for email "carlos.mendoza@example.com"
    When the client sends a POST request to "/auth/sign-up" with body:
      """
      {
        "fullName": "Carlos Mendoza",
        "email": "carlos.mendoza@example.com",
        "password": "Secur3Pass"
      }
      """
    Then the response status code is 400

  @TS-13 @TS-15 @signin
  Scenario: Successful sign in returns a JWT token
    Given an account exists for email "carlos.mendoza@example.com" with password "Secur3Pass"
    When the client sends a POST request to "/auth/sign-in" with body:
      """
      {
        "email": "carlos.mendoza@example.com",
        "password": "Secur3Pass"
      }
      """
    Then the response status code is 200
    And the response body contains a non-empty "token"

  @TS-13 @TS-15 @signin
  Scenario: Sign in is rejected with wrong credentials
    Given an account exists for email "carlos.mendoza@example.com" with password "Secur3Pass"
    When the client sends a POST request to "/auth/sign-in" with body:
      """
      {
        "email": "carlos.mendoza@example.com",
        "password": "WrongPass"
      }
      """
    Then the response status code is 400

  @TS-13 @security
  Scenario: A protected endpoint rejects requests without a token
    When the client sends a GET request to "/users/me" without an Authorization header
    Then the response status code is 403

  @TS-13 @security
  Scenario: A protected endpoint accepts a valid Bearer token
    Given the client is authenticated as "carlos.mendoza@example.com"
    When the client sends a GET request to "/auth/me"
    Then the response status code is 200
    And the response body contains "email" equal to "carlos.mendoza@example.com"

  @US-31 @recover-password
  Scenario: Requesting a password recovery link
    Given an account exists for email "carlos.mendoza@example.com" with password "Secur3Pass"
    When the client sends a POST request to "/auth/recover-password" with body:
      """
      { "email": "carlos.mendoza@example.com" }
      """
    Then the response status code is 200

  @US-32 @profile
  Scenario: Updating the authenticated user profile
    Given the client is authenticated as "carlos.mendoza@example.com"
    When the client sends a PUT request to "/users/me" with body:
      """
      {
        "fullName": "Carlos A. Mendoza",
        "email": "carlos.mendoza@example.com"
      }
      """
    Then the response status code is 200
    And the response body contains "fullName" equal to "Carlos A. Mendoza"

  @US-33 @delete-account
  Scenario: Deleting the authenticated user account
    Given the client is authenticated as "carlos.mendoza@example.com"
    When the client sends a DELETE request to "/users/me"
    Then the response status code is 200
    And a subsequent sign in with the same credentials returns status code 400
