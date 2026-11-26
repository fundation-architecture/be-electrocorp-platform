# Sprint 3 · Revisión inicial de la implementación (T-01)

Revisión del código de `develop` al inicio del Sprint 3 para las historias US-11, US-12, US-17, US-19 y US-21. Por cada historia se indica qué ya existe y qué falta, y la tarea del Sprint Backlog que lo resuelve.

## Device Control · US-11 Programación de horarios automáticos

| Capacidad existente | Brecha detectada | Tarea |
|---|---|---|
| `POST /api/v1/routines` registra la rutina (dispositivo, grupo, ambiente o sede; acción `TURN_ON`/`TURN_OFF`; hora `HH:mm`; repetición `ONCE`, `DAILY`, `WEEKLY`, `CUSTOM_INTERVAL`). | La respuesta no indica la zona horaria del horario ni la última ejecución. | T-02 |
| `RoutineSchedulePolicyService` valida el horario y `validateRoutineTarget` valida que el objetivo pertenezca al usuario. | `RoutineConflictCheckerService` existe pero no se usa: se pueden registrar dos rutinas idénticas (mismo objetivo, hora y acción) que dispararían el comando dos veces. | T-03 |
| `RoutineSchedulerService` revisa las rutinas cada 60 s y evita repetir una ocurrencia ya ejecutada (`lastExecutedAt`). | Usa `LocalDateTime.now()` del servidor. En Docker el contenedor trabaja en UTC: una rutina de las 19:00 se ejecutaría a las 14:00 hora de Lima. | T-04 |
| La ejecución reutiliza el mismo flujo de comandos que el control manual (`turnOn`/`turnOff` y registro de consumo). | Si ningún dispositivo puede recibir el comando (mantenimiento o retirado), la ejecución programada falla y solo queda en el log; no hay registro del resultado. | T-05 |

No existe un broker MQTT ni confirmación asíncrona del dispositivo en este repositorio: el estado del dispositivo se actualiza en la misma transacción del comando. La confirmación por MQTT queda fuera del alcance del monolito actual.

## Device Control · US-12 Modificación de programación existente

| Capacidad existente | Brecha detectada | Tarea |
|---|---|---|
| `PATCH /api/v1/routines/{id}/status` activa o desactiva una rutina. | No existe una operación para cambiar la hora, la acción o la repetición de una rutina. | T-07 |
| — | Al adelantar una rutina a una hora ya pasada del día, el planificador la ejecutaría de inmediato (recupera la ocurrencia "pendiente"). | T-08 |

## Energy Monitoring · US-17 Consulta del gasto acumulado del periodo

| Capacidad existente | Brecha detectada | Tarea |
|---|---|---|
| `GET /api/v1/energy-readings/dashboard-summary` devuelve `monthToDateCost` (Sprint 2). | La respuesta no indica el periodo (inicio y fin) al que corresponde el acumulado. | T-10 |
| Las consultas filtran por el usuario del JWT. | No hay una consulta del gasto de un periodo distinto al mes en curso ni del detalle por dispositivo. | T-11 |

## Notifications · US-19 Configuración de límite de consumo

| Capacidad existente | Brecha detectada | Tarea |
|---|---|---|
| `POST /api/v1/alerts/rules` guarda reglas por dispositivo, ambiente o sede. | `AlertRulePolicyService.validateRule` existe pero no se invoca: se aceptan límites negativos o métricas inválidas. | T-13 |
| La alerta de US-20 se crea al recibir `EnergyThresholdExceededIntegrationEvent`. | Energy Monitoring solo publica el evento cuando la lectura supera el límite general (1800 W). Un límite configurado de 1000 W nunca se aplica a una lectura de 1200 W. | T-14 |

## Notifications · US-21 Consulta del centro de notificaciones

| Capacidad existente | Brecha detectada | Tarea |
|---|---|---|
| `GET /api/v1/alerts` devuelve las alertas del usuario autenticado. | No permite filtrar por dispositivo ni por tipo de evento. | T-16 |
| Cada consulta usa el `userId` del JWT. | El orden de la lista no está definido; el centro de notificaciones debe mostrar primero la alerta más reciente. | T-17 |

## Tareas generales

| Brecha | Tarea |
|---|---|
| No hay integración continua: la compilación y las pruebas solo se ejecutan en local. | T-19 |
| La documentación OpenAPI no cubre las operaciones nuevas del Sprint 3. | T-20 |
| La colección de Postman solo cubre el Sprint 2. | T-21 |
| La caché del resumen solo se probó con una implementación en memoria; falta la prueba con PostgreSQL y Redis reales. | T-22 |
