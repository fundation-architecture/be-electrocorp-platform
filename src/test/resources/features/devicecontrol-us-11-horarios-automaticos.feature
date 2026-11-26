# language: es
@sprint-3 @device-control-sprint-3 @US-11
Característica: Programación de horarios automáticos
  Como usuario, quiero programar un horario de encendido o apagado
  para automatizar el funcionamiento de mis dispositivos.

  Escenario: Ejecución de una programación de apagado
    Dado que el usuario tiene un dispositivo vinculado y encendido
    Cuando registra una programación de apagado para las 18:00
    Entonces el sistema guarda la programación con la zona horaria "America/Lima"
    Cuando llega el horario programado de las 18:00
    Entonces el sistema apaga el dispositivo y registra la ejecución como "EXECUTED"

  Escenario: Dispositivo que no puede recibir el comando programado
    Dado que el usuario tiene un dispositivo vinculado y encendido
    Cuando registra una programación de apagado para las 18:00
    Y el dispositivo pasa a mantenimiento
    Cuando llega el horario programado de las 18:00
    Entonces el sistema registra la ejecución como "NOT_APPLIED" sin informar el apagado

  Escenario: Programación duplicada
    Dado que el usuario tiene un dispositivo vinculado y encendido
    Cuando registra una programación de apagado para las 18:00
    Y registra una programación de apagado para las 18:00
    Entonces el sistema rechaza la programación duplicada
