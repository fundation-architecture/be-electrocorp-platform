# language: es
@sprint-3 @notifications-sprint-3 @US-19
Característica: Configuración de límite de consumo
  Como usuario, quiero configurar un límite de consumo para un dispositivo
  para recibir una alerta cuando dicho límite sea superado.
  El límite es de potencia en watts y se supera cuando la lectura es estrictamente mayor.

  Escenario: Configuración válida de un límite por dispositivo
    Dado que el usuario está autenticado y tiene un dispositivo vinculado
    Cuando configura un límite de potencia de 1000 W para ese dispositivo
    Entonces el sistema guarda el límite en watts asociado al dispositivo
    Y mantiene activa la regla asociada al dispositivo

  Esquema del escenario: Evaluación del límite configurado
    Dado que el dispositivo tiene una regla activa con un límite de 1000 W
    Y no existen alertas previas de consumo para el dispositivo
    Cuando se registra una lectura de <potencia> W para ese dispositivo
    Entonces se generan <cantidad> alertas nuevas por consumo elevado para esa lectura

    Ejemplos:
      | potencia | cantidad |
      | 999      | 0        |
      | 1000     | 0        |
      | 1001     | 1        |

  Escenario: Rechazo de un límite inválido
    Dado que el usuario está autenticado y tiene un dispositivo vinculado
    Cuando intenta configurar un límite de potencia negativo
    Entonces el sistema rechaza la configuración
    Y no crea ninguna regla para el dispositivo
