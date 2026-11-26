package com.electrocorp.electrocorpplatform.notifications.domain.model.queries;

/**
 * Consulta del centro de notificaciones del usuario (US-21). Los filtros son opcionales:
 * deviceId limita la lista a las alertas de un dispositivo, sourceType al tipo de origen
 * (DEVICE, ROOM, SYSTEM, ...), eventType al tipo de evento y active a las alertas activas.
 */
public record GetAlertsByUserQuery(
        Long userId,
        Long deviceId,
        String sourceType,
        String eventType,
        Boolean active
) {
    public GetAlertsByUserQuery(Long userId) {
        this(userId, null, null, null, null);
    }
}
