# language: es
@sprint-3 @device-control-sprint-3 @US-12
Característica: Modificación de programación existente
  Como usuario, quiero modificar una programación existente para adaptar
  el funcionamiento automático de un dispositivo a mis necesidades.

  Escenario: Modificación del horario de apagado
    Dado que el usuario tiene un dispositivo vinculado y encendido
    Y tiene una programación de apagado para las 18:00
    Cuando cambia el horario a las 19:00
    Entonces el sistema guarda la programación con la hora 19:00 y la acción apagado
    Y el planificador no ejecuta la programación a las 18:00 del día siguiente
    Y el planificador ejecuta la programación a las 19:00 del día siguiente y el dispositivo queda OFF

  Escenario: Modificación de la acción programada
    Dado que el usuario tiene un dispositivo vinculado y encendido
    Y tiene una programación de encendido para las 19:00
    Cuando cambia la acción programada a apagado
    Entonces el sistema guarda la programación con la hora 19:00 y la acción apagado
    Y el planificador ejecuta la programación a las 19:00 del día siguiente y el dispositivo queda OFF

  Escenario: Modificación de una programación de otro usuario
    Dado que el usuario tiene un dispositivo vinculado y encendido
    Y tiene una programación de apagado para las 18:00
    Cuando otro usuario intenta cambiar el horario de esa programación a las 20:00
    Entonces el sistema rechaza la operación y conserva la hora 18:00
