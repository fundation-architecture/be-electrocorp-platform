# language: es
@sprint-3 @notifications-sprint-3 @US-21
Característica: Consulta del centro de notificaciones
  Como usuario, quiero consultar las alertas generadas por el sistema para identificar
  los dispositivos y situaciones que requieren mi atención.

  Escenario: Consulta de una alerta generada por consumo elevado
    Dado que el dispositivo tiene una regla activa con un límite de 1000 W
    Cuando se registra una lectura de 1200 W para ese dispositivo
    Y el usuario consulta su centro de notificaciones
    Entonces encuentra una alerta de consumo elevado asociada al dispositivo
    Y la alerta contiene información para identificar la situación detectada

  Escenario: Filtro de notificaciones por dispositivo
    Dado que el dispositivo tiene una regla activa con un límite de 1000 W
    Cuando se registra una lectura de 1200 W para ese dispositivo
    Y filtra sus notificaciones por ese dispositivo
    Entonces solo recibe alertas de ese dispositivo

  Escenario: Consulta de un usuario sin alertas de dispositivos
    Dado que el usuario recién registrado no tiene alertas de dispositivos
    Cuando consulta sus alertas de consumo
    Entonces el sistema devuelve una lista vacía sin error

  Escenario: Aislamiento de notificaciones entre usuarios
    Dado que existe una alerta de consumo elevado de otro usuario
    Y un segundo usuario inicia sesión
    Cuando el usuario consulta su centro de notificaciones
    Entonces la respuesta no incluye la alerta del otro usuario
