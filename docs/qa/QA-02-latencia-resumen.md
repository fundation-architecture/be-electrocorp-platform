# QA-02 - Latencia del resumen del panel

Escenario de calidad: el resumen de consumo del panel responde en menos de 5 s.
Endpoint medido: `GET /api/v1/energy-readings/dashboard-summary`.

## Medicion automatizada (T-42)

Prueba: `src/test/java/com/electrocorp/electrocorpplatform/energymonitoring/DashboardSummaryLatencyTest.java`.

- Perfil `test` (H2 en memoria), 1 usuario, 1 dispositivo y 300 lecturas.
- 5 peticiones de calentamiento y 40 peticiones medidas por escenario.
- "Con cache" usa una implementacion en memoria del puerto `ConsumptionSummaryCache`
  (mismo contrato y clave `consumption:{id}:summary` que el adaptador Redis), para medir
  el efecto de la cache sin depender de un servidor Redis en la maquina de pruebas.
- Cada ejecucion deja el resultado en `target/qa/QA-02-latencia-resumen.md`.

Resultado registrado el 05/11/2026:

| Escenario | Promedio (ms) | p95 (ms) | Maximo (ms) | Limite QA-02 | Cumple |
|-----------|---------------|----------|-------------|--------------|--------|
| Sin cache | 14.4 | 21 | 29 | < 5000 ms | Si |
| Con cache | 5.3 | 8 | 11 | < 5000 ms | Si |

Con la cache, el tiempo promedio baja alrededor de 63 % porque la segunda consulta dentro
de los 30 s del TTL no vuelve a leer dispositivos, lecturas ni alertas.

Comando:

```bash
./mvnw test -Dtest=DashboardSummaryLatencyTest
```

## Medicion contra Redis real (docker compose)

1. `cp .env.example .env` y `docker compose up --build`.
2. Iniciar sesion con la coleccion `docs/postman/ElectroCorp-Sprint2.postman_collection.json` y copiar el token.
3. Medir sin cache y con cache:

```bash
# Sin cache: reiniciar solo la app con ENERGY_SUMMARY_CACHE_ENABLED=false
for i in $(seq 1 20); do curl -s -o /dev/null -w "%{time_total}\n" -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/energy-readings/dashboard-summary; done

# Con cache (valor por defecto): repetir el mismo comando
docker exec -it electrocorp-redis redis-cli TTL consumption:<userId>:summary
```

El comando `TTL` debe devolver un valor entre 1 y 30 segundos despues de consultar el resumen.
