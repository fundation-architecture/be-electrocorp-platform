package com.electrocorp.electrocorpplatform.servicemanagement.application.services;

import com.electrocorp.electrocorpplatform.servicemanagement.application.commandservices.ServiceManagementCommandService;
import com.electrocorp.electrocorpplatform.servicemanagement.application.queryservices.ServiceManagementQueryService;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.commands.*;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.factories.SupportTicketFactory;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.aggregates.*;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.events.MaintenanceTicketCreatedEvent;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.events.SupportTicketCreatedEvent;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.repositories.*;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.DeviceRepository;
import com.electrocorp.electrocorpplatform.shared.application.events.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceManagementApplicationService implements ServiceManagementCommandService, ServiceManagementQueryService {

    private final SupportTicketRepository supportTicketRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final DeviceRepository deviceRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final SupportTicketFactory supportTicketFactory = new SupportTicketFactory();

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> getSupportTickets(Long userId) {
        return supportTicketRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public SupportTicket createSupportTicket(CreateSupportTicketCommand command) {
        SupportTicket ticket = supportTicketFactory.create(
                command.userId(),
                command.subject(),
                command.description(),
                command.priority()
        );
        SupportTicket savedTicket = supportTicketRepository.save(ticket);
        domainEventPublisher.publish(new SupportTicketCreatedEvent(
                savedTicket.getUserId(),
                savedTicket.getId(),
                savedTicket.getPriority()
        ));
        return savedTicket;
    }

    @Override
    @Transactional
    public SupportTicket updateSupportStatus(Long userId, Long ticketId, UpdateTicketStatusCommand command) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .filter(item -> item.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found."));
        ticket.setStatus(command.status());
        return supportTicketRepository.save(ticket);
    }

    @Override
    @Transactional
    public void deleteSupportTicket(DeleteSupportTicketCommand command) {
        SupportTicket ticket = supportTicketRepository.findById(command.ticketId())
                .filter(item -> item.getUserId().equals(command.userId()))
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found."));
        supportTicketRepository.delete(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceTicket> getMaintenanceTickets(Long userId) {
        return maintenanceTicketRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public MaintenanceTicket createMaintenanceTicket(CreateMaintenanceTicketCommand command) {
        var device = deviceRepository.findByIdAndUserId(command.deviceId(), command.userId())
                .orElseThrow(() -> new IllegalArgumentException("Device not found."));

        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setUserId(command.userId());
        ticket.setDeviceId(device.getId());
        ticket.setDeviceName(device.getName());
        ticket.setType(command.type());
        ticket.setDescription(command.description());
        ticket.setScheduledDate(command.scheduledDate());
        ticket.setStatus("OPEN");
        MaintenanceTicket savedTicket = maintenanceTicketRepository.save(ticket);
        domainEventPublisher.publish(new MaintenanceTicketCreatedEvent(
                savedTicket.getUserId(),
                savedTicket.getId(),
                savedTicket.getDeviceId()
        ));
        return savedTicket;
    }

    @Override
    @Transactional
    public MaintenanceTicket updateMaintenanceStatus(Long userId, Long ticketId, UpdateTicketStatusCommand command) {
        MaintenanceTicket ticket = maintenanceTicketRepository.findById(ticketId)
                .filter(item -> item.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Maintenance ticket not found."));
        ticket.setStatus(command.status());
        return maintenanceTicketRepository.save(ticket);
    }

    @Override
    @Transactional
    public void deleteMaintenanceTicket(DeleteMaintenanceTicketCommand command) {
        MaintenanceTicket ticket = maintenanceTicketRepository.findById(command.ticketId())
                .filter(item -> item.getUserId().equals(command.userId()))
                .orElseThrow(() -> new IllegalArgumentException("Maintenance ticket not found."));
        maintenanceTicketRepository.delete(ticket);
    }
}
