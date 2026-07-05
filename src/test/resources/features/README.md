# Acceptance Tests (BDD / Gherkin)

Estos archivos `.feature` especifican las **pruebas de aceptación** del RESTful API de
ElectroCorp usando la notación Gherkin (Given-When-Then), tal como exige el enunciado
del trabajo final (sección *Source Code Management*: el repositorio de Web Services
"incluye el proyecto y los archivos de pruebas, tanto unitarias como de
integración/aceptación").

Cada escenario está redactado en tiempo presente, describe interacciones
request/response del API y está etiquetado con el `US-##` / `TS-##` del Product Backlog
del informe.

## Cobertura por bounded context

| Archivo `.feature`          | Bounded Context      | User / Technical Stories                              |
|-----------------------------|----------------------|-------------------------------------------------------|
| `iam.feature`               | IAM                  | US-07, US-31, US-32, US-33, TS-13, TS-15              |
| `billing.feature`           | Billing              | US-06, US-27, US-28                                   |
| `device-control.feature`    | Device Control       | US-08, US-10, US-11, US-12, US-13, US-14, US-19, US-23, US-24, US-38, TS-11 |
| `energy-monitoring.feature` | Energy Monitoring    | US-15, US-35, US-36, TS-10, TS-14                     |
| `notifications.feature`     | Notifications        | US-17, US-29, US-34                                   |
| `reporting.feature`         | Reporting            | US-16, US-18, US-25, US-37                            |
| `workplace.feature`         | Workplace            | US-25, US-38, US-40                                   |
| `service-management.feature`| Service Management   | Gestión de servicios y soporte                        |

## Notas de fidelidad (importante)

Los escenarios reflejan **lo realmente implementado** en el backend:

- **Pagos (US-27):** validados por una política interna. **No** se integra una pasarela
  externa Stripe.
- **Alertas (US-29):** se entregan **in-app**. No hay envío de correo (Mailchimp).
- Los endpoints, verbos HTTP y campos de request corresponden a los controllers reales.

## Cómo ejecutar

Requiere: dependencias Cucumber en `pom.xml`, el runner `CucumberRunnerTest`, el puente
`SpringAcceptanceContext`, y las step definitions en
`src/test/java/com/electrocorp/electrocorpplatform/acceptance`.

```bash
./mvnw test
```
