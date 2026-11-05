# language: es
@sprint-2 @notifications-sprint-2 @US-20
Característica: Notificación por consumo elevado
  Como usuario, quiero recibir una notificación cuando un dispositivo presente un consumo
  superior al límite configurado para poder tomar una acción oportuna.

  Escenario: Generación de alerta por consumo elevado
    Dado que el usuario tiene una alerta de consumo configurada
    Cuando un dispositivo supera el límite establecido
    Entonces el sistema genera una notificación informando al usuario sobre el consumo elevado y el dispositivo involucrado

  Escenario: Lectura dentro del límite
    Dado que el usuario tiene una alerta de consumo configurada
    Cuando un dispositivo registra un consumo menor al límite establecido
    Entonces el sistema no genera ninguna notificación
