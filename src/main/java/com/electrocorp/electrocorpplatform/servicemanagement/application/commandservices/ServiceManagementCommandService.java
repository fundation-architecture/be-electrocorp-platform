package com.electrocorp.electrocorpplatform.servicemanagement.application.commandservices;

import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.aggregates.MaintenanceTicket;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.aggregates.SupportTicket;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.commands.CreateMaintenanceTicketCommand;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.commands.CreateSupportTicketCommand;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.commands.DeleteMaintenanceTicketCommand;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.commands.DeleteSupportTicketCommand;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.commands.UpdateTicketStatusCommand;

public interface ServiceManagementCommandService {
    SupportTicket createSupportTicket(CreateSupportTicketCommand command);

    SupportTicket updateSupportStatus(Long userId, Long ticketId, UpdateTicketStatusCommand command);

    void deleteSupportTicket(DeleteSupportTicketCommand command);

    MaintenanceTicket createMaintenanceTicket(CreateMaintenanceTicketCommand command);

    MaintenanceTicket updateMaintenanceStatus(Long userId, Long ticketId, UpdateTicketStatusCommand command);

    void deleteMaintenanceTicket(DeleteMaintenanceTicketCommand command);
}
