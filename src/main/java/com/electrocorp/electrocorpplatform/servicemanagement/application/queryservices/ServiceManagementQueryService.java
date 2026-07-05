package com.electrocorp.electrocorpplatform.servicemanagement.application.queryservices;

import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.aggregates.MaintenanceTicket;
import com.electrocorp.electrocorpplatform.servicemanagement.domain.model.aggregates.SupportTicket;

import java.util.List;

public interface ServiceManagementQueryService {
    List<SupportTicket> getSupportTickets(Long userId);

    List<MaintenanceTicket> getMaintenanceTickets(Long userId);
}
