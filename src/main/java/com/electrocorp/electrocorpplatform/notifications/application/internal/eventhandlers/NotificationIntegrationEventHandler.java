package com.electrocorp.electrocorpplatform.notifications.application.internal.eventhandlers;

import com.electrocorp.electrocorpplatform.billing.interfaces.events.PaymentRegisteredIntegrationEvent;
import com.electrocorp.electrocorpplatform.billing.interfaces.events.SubscriptionActivatedIntegrationEvent;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.events.DeviceCreatedIntegrationEvent;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.events.OperationModeActivatedIntegrationEvent;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.events.EnergyThresholdExceededIntegrationEvent;
import com.electrocorp.electrocorpplatform.iam.interfaces.events.UserRegisteredIntegrationEvent;
import com.electrocorp.electrocorpplatform.notifications.application.services.NotificationApplicationService;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.Alert;
import com.electrocorp.electrocorpplatform.notifications.domain.model.commands.CreateAlertCommand;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.RuleScopeType;
import com.electrocorp.electrocorpplatform.servicemanagement.interfaces.events.MaintenanceTicketCreatedIntegrationEvent;
import com.electrocorp.electrocorpplatform.servicemanagement.interfaces.events.SupportTicketCreatedIntegrationEvent;
import com.electrocorp.electrocorpplatform.workplace.interfaces.events.DeviceAssignedToRoomIntegrationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class NotificationIntegrationEventHandler {

    private final NotificationApplicationService notificationApplicationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(UserRegisteredIntegrationEvent event) {
        notificationApplicationService.createAlert(systemAlert(
                event.userId(),
                "Cuenta creada",
                "Tu cuenta ElectroCorp quedo activa y lista para conectar sedes y dispositivos.",
                "INFO",
                "IAM:USER:%d:REGISTERED".formatted(event.userId()),
                "Usuario registrado con email " + event.email() + ".",
                "La identidad ya puede operar los bounded contexts protegidos con JWT.",
                "Completa tu primera sede y agrega dispositivos para empezar a monitorear consumo.",
                30
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(SubscriptionActivatedIntegrationEvent event) {
        notificationApplicationService.createAlert(systemAlert(
                event.userId(),
                "Plan activado: " + event.planCode(),
                "La suscripcion quedo activa para la plataforma.",
                "SUCCESS",
                "BILLING:SUBSCRIPTION:%d:ACTIVATED".formatted(event.subscriptionId()),
                "Suscripcion " + event.subscriptionId() + " asociada al plan " + event.planCode() + ".",
                "Billing confirmo el cambio de estado y notifico a la plataforma mediante integration event.",
                "Revisa las capacidades habilitadas para usar dispositivos, alertas y reportes.",
                45
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PaymentRegisteredIntegrationEvent event) {
        notificationApplicationService.createAlert(systemAlert(
                event.userId(),
                "Pago registrado",
                "Se registro un pago aprobado por S/ " + event.amount() + ".",
                "SUCCESS",
                "BILLING:PAYMENT:%d:REGISTERED".formatted(event.paymentId()),
                "Pago " + event.paymentId() + " por S/ " + event.amount() + ".",
                "Billing emitio el evento de pago para mantener trazabilidad operativa.",
                "Conserva la factura generada en tu historial de pagos.",
                38
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(DeviceCreatedIntegrationEvent event) {
        notificationApplicationService.createAlert(new CreateAlertCommand(
                event.userId(),
                "Dispositivo agregado: " + event.deviceName(),
                "El dispositivo ya esta disponible para control y monitoreo.",
                "INFO",
                "DEVICE",
                String.valueOf(event.deviceId()),
                event.deviceName(),
                "DEVICE_STATUS",
                "DEVICE:%d:CREATED".formatted(event.deviceId()),
                "DeviceControl registro el dispositivo " + event.deviceId() + ".",
                "El dispositivo queda preparado para rutinas, grupos, alertas y reportes.",
                "Asigna el dispositivo a una habitacion para mejorar la lectura de consumo por ambiente.",
                35,
                null
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(OperationModeActivatedIntegrationEvent event) {
        notificationApplicationService.createAlert(new CreateAlertCommand(
                event.userId(),
                "Modo activado: " + event.modeName(),
                "El plan operativo se aplico sobre la sede seleccionada.",
                "INFO",
                "MODE",
                String.valueOf(event.modeId()),
                event.modeName(),
                "MODE_ACTIVITY",
                "MODE:%d:ACTIVATION".formatted(event.modeId()),
                event.evidence(),
                event.explanation(),
                event.recommendedAction(),
                42,
                null
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(DeviceAssignedToRoomIntegrationEvent event) {
        String sourceType = event.roomId() == null ? "DEVICE" : "ROOM";
        String sourceId = String.valueOf(event.roomId() == null ? event.deviceId() : event.roomId());

        notificationApplicationService.createAlert(new CreateAlertCommand(
                event.userId(),
                "Dispositivo asignado a ambiente",
                "La ubicacion del dispositivo fue sincronizada con Workplace.",
                "INFO",
                sourceType,
                sourceId,
                event.roomId() == null ? "Dispositivo " + event.deviceId() : "Ambiente " + event.roomId(),
                "ROOM_ACTIVITY",
                "WORKPLACE:DEVICE:%d:ROOM:%s".formatted(event.deviceId(), sourceId),
                "Dispositivo " + event.deviceId() + " asignado al ambiente " + sourceId + ".",
                "Workplace emitio el evento y DeviceControl quedo sincronizado.",
                "Usa los reportes por ambiente para comparar consumo despues de la asignacion.",
                32,
                null
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(EnergyThresholdExceededIntegrationEvent event) {
        Alert ruleAlert = notificationApplicationService.createAlertFromRuleEvaluation(
                event.userId(),
                RuleScopeType.DEVICE,
                String.valueOf(event.deviceId()),
                event.watts(),
                "Dispositivo " + event.deviceId()
        );

        if (ruleAlert != null) {
            return;
        }

        notificationApplicationService.createAlert(new CreateAlertCommand(
                event.userId(),
                "Consumo elevado detectado",
                "El dispositivo supero el umbral de consumo esperado.",
                "WARNING",
                "DEVICE",
                String.valueOf(event.deviceId()),
                "Dispositivo " + event.deviceId(),
                "CONSUMPTION_REVIEW",
                "ENERGY:DEVICE:%d:THRESHOLD".formatted(event.deviceId()),
                "Lectura de " + event.watts() + " W registrada en monitoreo energetico.",
                "Energy Monitoring clasifico la lectura como alta y la comunico a Notifications.",
                "Revisa rutinas activas o apaga cargas no prioritarias.",
                68,
                LocalDateTime.now().plusHours(2).toString()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(SupportTicketCreatedIntegrationEvent event) {
        notificationApplicationService.createAlert(systemAlert(
                event.userId(),
                "Ticket de soporte creado",
                "Tu solicitud de soporte fue registrada con prioridad " + event.priority() + ".",
                "INFO",
                "SERVICE:SUPPORT:%d:CREATED".formatted(event.ticketId()),
                "Ticket de soporte " + event.ticketId() + " creado.",
                "Service Management notifico la apertura del ticket para seguimiento.",
                "Revisa el estado del ticket desde la seccion de soporte.",
                36
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(MaintenanceTicketCreatedIntegrationEvent event) {
        notificationApplicationService.createAlert(new CreateAlertCommand(
                event.userId(),
                "Mantenimiento programado",
                "Se registro un ticket de mantenimiento para el dispositivo.",
                "INFO",
                "DEVICE",
                String.valueOf(event.deviceId()),
                "Dispositivo " + event.deviceId(),
                "SYSTEM_STATUS",
                "SERVICE:MAINTENANCE:%d:CREATED".formatted(event.ticketId()),
                "Ticket de mantenimiento " + event.ticketId() + " para dispositivo " + event.deviceId() + ".",
                "Service Management conecto el mantenimiento con el dispositivo afectado.",
                "Valida la fecha programada y prepara el dispositivo para revision.",
                40,
                null
        ));
    }

    private CreateAlertCommand systemAlert(
            Long userId,
            String title,
            String message,
            String level,
            String threadKey,
            String evidence,
            String explanation,
            String recommendedAction,
            Integer severityScore
    ) {
        return new CreateAlertCommand(
                userId,
                title,
                message,
                level,
                "SYSTEM",
                null,
                "ElectroCorp",
                "SYSTEM_STATUS",
                threadKey,
                evidence,
                explanation,
                recommendedAction,
                severityScore,
                null
        );
    }
}
