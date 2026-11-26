# language: es
@sprint-3 @energy-monitoring-sprint-3 @US-17
Característica: Consulta del gasto acumulado del periodo
  Como usuario, quiero consultar el gasto eléctrico acumulado del periodo actual
  para controlar mejor mi presupuesto.

  Escenario: Cálculo del gasto acumulado del mes actual
    Dado que el usuario tiene lecturas del mes actual con costos de S/ 1.50 y S/ 2.25
    Cuando consulta el gasto acumulado del mes actual
    Entonces el sistema devuelve un gasto acumulado de S/ 3.75 expresado en soles

  Escenario: Exclusión de lecturas de otros periodos
    Dado que el usuario tiene lecturas del mes actual con costos de S/ 1.50 y S/ 2.25
    Cuando consulta el gasto acumulado del mes anterior
    Entonces el periodo consultado no incluye las lecturas del mes actual

  Escenario: Periodo actual sin lecturas
    Dado que el usuario no tiene lecturas registradas durante el mes actual
    Cuando consulta el gasto acumulado del mes actual
    Entonces el sistema devuelve un gasto acumulado de S/ 0.00 expresado en soles

  Escenario: Exclusión de lecturas de otro usuario
    Dado que el usuario tiene lecturas del mes actual con costos de S/ 1.50 y S/ 2.25
    Y otro usuario acumula S/ 6.00 durante el mismo mes
    Cuando consulta el gasto acumulado del mes actual
    Entonces el sistema devuelve un gasto acumulado de S/ 3.75 expresado en soles

  Escenario: Periodo con fechas inválidas
    Dado que el usuario no tiene lecturas registradas durante el mes actual
    Cuando consulta un periodo cuya fecha final es anterior a la inicial
    Entonces el sistema rechaza la consulta indicando que el periodo no es válido
