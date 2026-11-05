# language: es
@sprint-2 @energy-monitoring-sprint-2 @US-13
Característica: Visualización de consumo energético actual
  Como usuario, quiero visualizar el consumo energético actual de mis dispositivos
  para conocer cuánta energía están utilizando.

  Escenario: Visualización del consumo energético actual
    Dado que el usuario tiene dispositivos IoT vinculados y enviando información de consumo
    Cuando accede al panel de monitoreo
    Entonces el sistema muestra el consumo energético actual registrado por los dispositivos

  Escenario: Usuario con dispositivos que aún no envían lecturas
    Dado que el usuario tiene dispositivos vinculados que todavía no han enviado lecturas
    Cuando accede al panel de monitoreo
    Entonces el sistema muestra un consumo actual de 0 W sin devolver error
