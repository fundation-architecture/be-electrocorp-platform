# language: es
@sprint-2 @energy-monitoring-sprint-2 @US-18
Característica: Proyección del gasto mensual
  Como usuario, quiero visualizar una proyección de mi gasto mensual para anticipar
  el monto aproximado que podría pagar al finalizar el periodo de facturación.

  Escenario: Proyección del gasto mensual
    Dado que el sistema dispone de registros suficientes del consumo energético del usuario
    Cuando este consulta la proyección mensual
    Entonces el sistema estima y muestra el gasto esperado al finalizar el periodo de facturación

  Escenario: Proyección con el costo acumulado del mes
    Dado que en los primeros 10 días de un mes de 30 días el usuario acumuló S/ 20.00 de costo
    Cuando consulta la proyección mensual
    Entonces el sistema muestra una proyección de S/ 60.00
