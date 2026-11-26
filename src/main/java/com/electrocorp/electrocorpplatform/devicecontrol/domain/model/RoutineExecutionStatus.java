package com.electrocorp.electrocorpplatform.devicecontrol.domain.model;

/**
 * Resultado de la ultima ejecucion de una programacion (US-11).
 * <ul>
 *     <li>EXECUTED: el comando se aplico a todos los dispositivos de la programacion.</li>
 *     <li>PARTIAL: el comando se aplico solo a una parte; el resto estaba en mantenimiento o retirado.</li>
 *     <li>NOT_APPLIED: ningun dispositivo pudo recibir el comando; no se informa la accion como realizada.</li>
 * </ul>
 */
public enum RoutineExecutionStatus {
    EXECUTED,
    PARTIAL,
    NOT_APPLIED
}
