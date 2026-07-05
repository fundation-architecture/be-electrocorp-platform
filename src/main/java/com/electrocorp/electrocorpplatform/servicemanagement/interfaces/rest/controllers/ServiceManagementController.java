package com.electrocorp.electrocorpplatform.servicemanagement.interfaces.rest.controllers;

import com.electrocorp.electrocorpplatform.servicemanagement.application.commandservices.ServiceManagementCommandService;
import com.electrocorp.electrocorpplatform.servicemanagement.application.queryservices.ServiceManagementQueryService;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.commands.DeleteMaintenanceTicketCommand;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.commands.DeleteSupportTicketCommand;
import com.electrocorp.electrocorpplatform.servicemanagement.interfaces.rest.resources.*;
import com.electrocorp.electrocorpplatform.servicemanagement.interfaces.rest.transform.*;
import com.electrocorp.electrocorpplatform.iam.application.security.AccessAuthorizationService;
import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ServiceManagementController {

    private final ServiceManagementCommandService commandService;
    private final ServiceManagementQueryService queryService;
    private final AccessAuthorizationService accessAuthorizationService;

    @GetMapping("/support-tickets")
    public List<SupportTicketResource> getSupportTickets() {
        return queryService.getSupportTickets(requireSupportManagement()).stream()
                .map(SupportTicketResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @PostMapping("/support-tickets")
    public SupportTicketResource createSupportTicket(@Valid @RequestBody CreateSupportTicketResource request) {
        var command = CreateSupportTicketCommandFromResourceAssembler.toCommandFromResource(request, requireSupportManagement());
        var ticket = commandService.createSupportTicket(command);
        return SupportTicketResourceFromEntityAssembler.toResourceFromEntity(ticket);
    }

    @PatchMapping("/support-tickets/{ticketId}/status")
    public SupportTicketResource updateSupportStatus(@PathVariable Long ticketId, @Valid @RequestBody UpdateTicketStatusResource request) {
        var command = UpdateTicketStatusCommandFromResourceAssembler.toCommandFromResource(request);
        var ticket = commandService.updateSupportStatus(requireSupportManagement(), ticketId, command);
        return SupportTicketResourceFromEntityAssembler.toResourceFromEntity(ticket);
    }

    @DeleteMapping("/support-tickets/{ticketId}")
    public void deleteSupportTicket(@PathVariable Long ticketId) {
        commandService.deleteSupportTicket(new DeleteSupportTicketCommand(requireSupportManagement(), ticketId));
    }

    @GetMapping("/maintenance-tickets")
    public List<MaintenanceTicketResource> getMaintenanceTickets() {
        return queryService.getMaintenanceTickets(requireSupportManagement()).stream()
                .map(MaintenanceTicketResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @PostMapping("/maintenance-tickets")
    public MaintenanceTicketResource createMaintenanceTicket(@Valid @RequestBody CreateMaintenanceTicketResource request) {
        var command = CreateMaintenanceTicketCommandFromResourceAssembler.toCommandFromResource(request, requireSupportManagement());
        var ticket = commandService.createMaintenanceTicket(command);
        return MaintenanceTicketResourceFromEntityAssembler.toResourceFromEntity(ticket);
    }

    @PatchMapping("/maintenance-tickets/{ticketId}/status")
    public MaintenanceTicketResource updateMaintenanceStatus(@PathVariable Long ticketId, @Valid @RequestBody UpdateTicketStatusResource request) {
        var command = UpdateTicketStatusCommandFromResourceAssembler.toCommandFromResource(request);
        var ticket = commandService.updateMaintenanceStatus(requireSupportManagement(), ticketId, command);
        return MaintenanceTicketResourceFromEntityAssembler.toResourceFromEntity(ticket);
    }

    @DeleteMapping("/maintenance-tickets/{ticketId}")
    public void deleteMaintenanceTicket(@PathVariable Long ticketId) {
        commandService.deleteMaintenanceTicket(new DeleteMaintenanceTicketCommand(requireSupportManagement(), ticketId));
    }

    private Long requireSupportManagement() {
        return accessAuthorizationService.requirePermission(AccessPermission.MANAGE_SUPPORT);
    }
}
