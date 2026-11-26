# QA-02 · Resumen del panel con PostgreSQL y Redis reales (Sprint 3, T-22)

Prueba: `src/test/java/com/electrocorp/electrocorpplatform/energymonitoring/SummaryCachePostgresRedisIntegrationTest.java`.

La prueba levanta PostgreSQL 16 (`postgres:16-alpine`) y Redis 7 (`redis:7-alpine`) con Testcontainers y ejecuta la aplicación contra ellos (no usa H2 ni una caché en memoria). Requiere Docker: en una máquina sin Docker se omite, y en GitHub Actions (T-19) se ejecuta en cada push.

## Verificación de la caché

| Comprobación | Resultado |
|---|---|
| Clave creada al consultar `GET /api/v1/energy-readings/dashboard-summary` | `consumption:{userId}:summary` (una por usuario) |
| TTL de la clave | Entre 1 y 30 segundos |
| Invalidación al registrar una lectura (`POST /api/v1/energy-readings`) | La clave se elimina y se vuelve a crear en la siguiente consulta |

La clave usa el `userId` porque el resumen del panel agrupa todos los dispositivos del usuario. La documentación del Sprint 2 mencionaba `consumption:{deviceId}:summary`; el formato real es el de esta tabla.

## Medición de tiempo de respuesta

Resultado registrado el 26/11/2026 (1 usuario, 1 dispositivo, 200 lecturas, 30 peticiones por escenario):

| Escenario | Promedio (ms) | p95 (ms) | Máximo (ms) | Límite QA-02 | Cumple |
|---|---|---|---|---|---|
| Sin caché (clave borrada antes de cada consulta) | 31.4 | 41 | 45 | < 5000 ms | Sí |
| Con caché (Redis) | 12.1 | 16 | 17 | < 5000 ms | Sí |

Con Redis, el tiempo promedio baja alrededor de 61 %. Cada ejecución deja su propio resultado en `target/qa/QA-02-postgres-redis.md`.

Comando:

```bash
./mvnw test -Dtest=SummaryCachePostgresRedisIntegrationTest
```
