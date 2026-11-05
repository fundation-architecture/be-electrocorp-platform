# language: es
@sprint-2 @energy-monitoring-sprint-2 @US-16
Característica: Visualización del costo en soles
  Como usuario, quiero visualizar el costo estimado de mi consumo eléctrico en soles
  para comprender cuánto representa económicamente la energía utilizada.

  Escenario: Visualización del consumo eléctrico en soles
    Dado que el sistema dispone de información sobre el consumo energético registrado
    Cuando el usuario consulta el detalle de su consumo
    Entonces el sistema calcula y muestra el costo estimado correspondiente expresado en soles (S/)

  Escenario: Costo calculado con la tarifa configurada
    Dado que la tarifa configurada es de S/ 0.75 por kWh y el usuario consumió 2 kWh hoy
    Cuando consulta el detalle de su consumo
    Entonces el costo estimado mostrado es de S/ 1.50
