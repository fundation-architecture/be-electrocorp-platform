package com.electrocorp.electrocorpplatform.reporting.application.internal.eventhandlers;

import com.electrocorp.electrocorpplatform.billing.interfaces.events.PaymentRegisteredIntegrationEvent;
import com.electrocorp.electrocorpplatform.billing.interfaces.events.SubscriptionActivatedIntegrationEvent;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.events.DeviceCreatedIntegrationEvent;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.events.OperationModeActivatedIntegrationEvent;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.events.EnergyThresholdExceededIntegrationEvent;
import com.electrocorp.electrocorpplatform.iam.interfaces.events.UserRegisteredIntegrationEvent;
import com.electrocorp.electrocorpplatform.reporting.application.commandservices.ReportingCommandService;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.RecordReportingEventCommand;
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
public class ReportingIntegrationEventHandler {

    private final ReportingCommandService reportingCommandService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(UserRegisteredIntegrationEvent event) {
        reportingCommandService.handle(activity(
                event.userId(),
                "USER_REGISTERED",
                "IAM",
                "USER",
                event.userId(),
                "Cuenta registrada",
                "Usuario registrado con email " + event.email() + ".",
                event.occurredOn()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(SubscriptionActivatedIntegrationEvent event) {
        reportingCommandService.handle(activity(
                event.userId(),
                "SUBSCRIPTION_ACTIVATED",
                "BILLING",
                "SUBSCRIPTION",
                event.subscriptionId(),
                "Suscripcion activada",
                "Plan " + event.planCode() + " activado para el usuario.",
                event.occurredOn()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PaymentRegisteredIntegrationEvent event) {
        reportingCommandService.handle(activity(
                event.userId(),
                "PAYMENT_REGISTERED",
                "BILLING",
                "PAYMENT",
                event.paymentId(),
                "Pago registrado",
                "Pago aprobado por S/ " + event.amount() + ".",
                event.occurredOn()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(DeviceCreatedIntegrationEvent event) {
        reportingCommandService.handle(activity(
                event.userId(),
                "DEVICE_CREATED",
                "DEVICE_CONTROL",
                "DEVICE",
                event.deviceId(),
                "Dispositivo agregado",
                "Dispositivo " + event.deviceName() + " preparado para control, alertas y reportes.",
                event.occurredOn()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(OperationModeActivatedIntegrationEvent event) {
        reportingCommandService.handle(activity(
                event.userId(),
                "OPERATION_MODE_ACTIVATED",
                "DEVICE_CONTROL",
                "MODE",
                event.modeId(),
                "Modo operativo activado",
                event.evidence() + " " + event.explanation(),
                event.occurredOn()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(DeviceAssignedToRoomIntegrationEvent event) {
        reportingCommandService.handle(activity(
                event.userId(),
                "DEVICE_ASSIGNED_TO_ROOM",
                "WORKPLACE",
                "ROOM",
                event.roomId(),
                "Dispositivo asignado a ambiente",
                "Dispositivo " + event.deviceId() + " asignado al ambiente " + event.roomId() + ".",
                event.occurredOn()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(EnergyThresholdExceededIntegrationEvent event) {
        reportingCommandService.handle(activity(
                event.userId(),
                "ENERGY_THRESHOLD_EXCEEDED",
                "ENERGY_MONITORING",
                "DEVICE",
                event.deviceId(),
                "Umbral de consumo excedido",
                "Lectura alta de " + event.watts() + " W registrada para el dispositivo.",
                event.occurredOn()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(SupportTicketCreatedIntegrationEvent event) {
        reportingCommandService.handle(activity(
                event.userId(),
                "SUPPORT_TICKET_CREATED",
                "SERVICE_MANAGEMENT",
                "SUPPORT_TICKET",
                event.ticketId(),
                "Ticket de soporte creado",
                "Prioridad registrada: " + event.priority() + ".",
                event.occurredOn()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(MaintenanceTicketCreatedIntegrationEvent event) {
        reportingCommandService.handle(activity(
                event.userId(),
                "MAINTENANCE_TICKET_CREATED",
                "SERVICE_MANAGEMENT",
                "MAINTENANCE_TICKET",
                event.ticketId(),
                "Ticket de mantenimiento creado",
                "Mantenimiento registrado para el dispositivo " + event.deviceId() + ".",
                event.occurredOn()
        ));
    }

    private RecordReportingEventCommand activity(
            Long userId,
            String eventName,
            String sourceContext,
            String subjectType,
            Long subjectId,
            String summary,
            String detail,
            LocalDateTime occurredOn
    ) {
        return new RecordReportingEventCommand(
                userId,
                eventName,
                sourceContext,
                subjectType,
                subjectId == null ? null : String.valueOf(subjectId),
                summary,
                detail,
                occurredOn
        );
    }
}
