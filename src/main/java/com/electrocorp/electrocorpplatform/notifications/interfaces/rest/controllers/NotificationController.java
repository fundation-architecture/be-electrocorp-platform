package com.electrocorp.electrocorpplatform.notifications.interfaces.rest.controllers;

import com.electrocorp.electrocorpplatform.notifications.application.commandservices.NotificationCommandService;
import com.electrocorp.electrocorpplatform.notifications.application.queryservices.NotificationQueryService;
import com.electrocorp.electrocorpplatform.notifications.domain.model.commands.ActivateAlertRuleProfileCommand;
import com.electrocorp.electrocorpplatform.notifications.domain.model.commands.DismissAlertCommand;
import com.electrocorp.electrocorpplatform.notifications.domain.model.commands.MarkAlertAsReadCommand;
import com.electrocorp.electrocorpplatform.notifications.domain.model.commands.ResolveAlertCommand;
import com.electrocorp.electrocorpplatform.notifications.domain.model.commands.ToggleAlertRuleCommand;
import com.electrocorp.electrocorpplatform.notifications.domain.model.queries.EvaluateAlertRulesQuery;
import com.electrocorp.electrocorpplatform.notifications.domain.model.queries.GetAlertRulesByUserQuery;
import com.electrocorp.electrocorpplatform.notifications.domain.model.queries.GetAlertRuleProfilesByUserQuery;
import com.electrocorp.electrocorpplatform.notifications.domain.model.queries.GetAlertsByUserQuery;
import com.electrocorp.electrocorpplatform.notifications.domain.model.queries.GetNotificationPreferenceQuery;
import com.electrocorp.electrocorpplatform.notifications.interfaces.rest.resources.*;
import com.electrocorp.electrocorpplatform.notifications.interfaces.rest.transform.*;
import com.electrocorp.electrocorpplatform.iam.application.security.AccessAuthorizationService;
import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notifications", description = "Alertas in-app, reglas de consumo y preferencias de notificacion")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationCommandService commandService;
    private final NotificationQueryService queryService;
    private final AccessAuthorizationService accessAuthorizationService;

    @Operation(summary = "Listar las alertas del usuario", description = "Devuelve las alertas in-app del usuario autenticado, incluidas las de consumo elevado (US-20).")
    @GetMapping("/alerts")
    public List<AlertResource> getAlerts() {
        return queryService.handle(new GetAlertsByUserQuery(requireAlertManagement())).stream()
                .map(AlertResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @Operation(summary = "Crear una alerta", description = "Registra una alerta manual para el usuario autenticado.")
    @PostMapping("/alerts")
    public AlertResource createAlert(@Valid @RequestBody CreateAlertResource request) {
        var command = CreateAlertCommandFromResourceAssembler.toCommandFromResource(request, requireAlertManagement());
        var alert = commandService.handle(command);
        return AlertResourceFromEntityAssembler.toResourceFromEntity(alert);
    }

    @Operation(summary = "Marcar una alerta como leida", description = "Cambia el estado de lectura de la alerta indicada.")
    @PatchMapping("/alerts/{alertId}/read")
    public AlertResource markAsRead(@PathVariable Long alertId) {
        var alert = commandService.handle(new MarkAlertAsReadCommand(requireAlertManagement(), alertId));
        return AlertResourceFromEntityAssembler.toResourceFromEntity(alert);
    }

    @Operation(summary = "Posponer una alerta", description = "Silencia la alerta durante los minutos indicados.")
    @PatchMapping("/alerts/{alertId}/dismiss")
    public AlertResource dismissAlert(
            @PathVariable Long alertId,
            @RequestParam(defaultValue = "10") Integer minutes
    ) {
        var alert = commandService.handle(new DismissAlertCommand(requireAlertManagement(), alertId, minutes));
        return AlertResourceFromEntityAssembler.toResourceFromEntity(alert);
    }

    @Operation(summary = "Resolver una alerta", description = "Marca la alerta como resuelta y la desactiva.")
    @PatchMapping("/alerts/{alertId}/resolve")
    public AlertResource resolveAlert(@PathVariable Long alertId) {
        var alert = commandService.handle(new ResolveAlertCommand(requireAlertManagement(), alertId));
        return AlertResourceFromEntityAssembler.toResourceFromEntity(alert);
    }

    @Operation(summary = "Eliminar una alerta", description = "Elimina la alerta indicada del usuario autenticado.")
    @DeleteMapping("/alerts/{alertId}")
    public void deleteAlert(@PathVariable Long alertId) {
        commandService.deleteAlert(requireAlertManagement(), alertId);
    }

    @Operation(summary = "Listar las reglas de alerta", description = "Devuelve los limites de consumo configurados por el usuario.")
    @GetMapping("/alerts/rules")
    public List<AlertRuleResource> getRules() {
        return queryService.handle(new GetAlertRulesByUserQuery(requireAlertManagement())).stream()
                .map(AlertRuleResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @Operation(summary = "Crear una regla de alerta", description = "Configura un limite de consumo (por ejemplo, WATTS GREATER_THAN 2000) para un dispositivo, ambiente o sede.")
    @PostMapping("/alerts/rules")
    public AlertRuleResource createRule(@Valid @RequestBody CreateAlertRuleResource request) {
        var command = CreateAlertRuleCommandFromResourceAssembler.toCommandFromResource(request, requireAlertManagement());
        var rule = commandService.handle(command);
        return AlertRuleResourceFromEntityAssembler.toResourceFromEntity(rule);
    }

    @Operation(summary = "Activar o desactivar una regla", description = "Alterna el estado habilitado de la regla de alerta.")
    @PatchMapping("/alerts/rules/{ruleId}/toggle")
    public AlertRuleResource toggleRule(@PathVariable Long ruleId) {
        var rule = commandService.handle(new ToggleAlertRuleCommand(requireAlertManagement(), ruleId));
        return AlertRuleResourceFromEntityAssembler.toResourceFromEntity(rule);
    }

    @Operation(summary = "Eliminar una regla de alerta", description = "Elimina la regla indicada.")
    @DeleteMapping("/alerts/rules/{ruleId}")
    public void deleteRule(@PathVariable Long ruleId) {
        commandService.deleteRule(requireAlertManagement(), ruleId);
    }

    @Operation(summary = "Listar los perfiles de reglas", description = "Devuelve los perfiles que agrupan reglas de alerta.")
    @GetMapping("/alerts/rule-profiles")
    public List<AlertRuleProfileResource> getRuleProfiles() {
        return queryService.handle(new GetAlertRuleProfilesByUserQuery(requireAlertManagement())).stream()
                .map(AlertRuleProfileResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @Operation(summary = "Crear un perfil de reglas", description = "Registra un perfil para agrupar reglas de alerta.")
    @PostMapping("/alerts/rule-profiles")
    public AlertRuleProfileResource createRuleProfile(@Valid @RequestBody CreateAlertRuleProfileResource request) {
        var command = CreateAlertRuleProfileCommandFromResourceAssembler.toCommandFromResource(request, requireAlertManagement());
        var profile = commandService.handle(command);
        return AlertRuleProfileResourceFromEntityAssembler.toResourceFromEntity(profile);
    }

    @Operation(summary = "Activar un perfil de reglas", description = "Activa el perfil indicado y sus reglas.")
    @PatchMapping("/alerts/rule-profiles/{profileId}/activate")
    public AlertRuleProfileResource activateRuleProfile(@PathVariable Long profileId) {
        var profile = commandService.handle(new ActivateAlertRuleProfileCommand(requireAlertManagement(), profileId));
        return AlertRuleProfileResourceFromEntityAssembler.toResourceFromEntity(profile);
    }

    @Operation(summary = "Evaluar las reglas de alerta", description = "Evalua las reglas activas contra un valor observado sin crear la alerta.")
    @PostMapping("/alerts/rules/evaluate")
    public RuleEvaluationResultResource evaluateRules(@Valid @RequestBody EvaluateAlertRulesResource request) {
        var result = queryService.handle(new EvaluateAlertRulesQuery(
                requireAlertManagement(),
                request.scopeType(),
                request.scopeId(),
                request.observedValue()
        ));
        return RuleEvaluationResultResource.from(result);
    }

    @Operation(summary = "Consultar las preferencias de notificacion", description = "Devuelve los canales y horarios de notificacion del usuario.")
    @GetMapping("/notifications/preferences")
    public NotificationPreferenceResource getPreference() {
        var preference = queryService.handle(new GetNotificationPreferenceQuery(requireAlertManagement()));
        return NotificationPreferenceResourceFromEntityAssembler.toResourceFromEntity(preference);
    }

    @Operation(summary = "Actualizar las preferencias de notificacion", description = "Actualiza los canales y horarios de notificacion del usuario.")
    @PutMapping("/notifications/preferences")
    public NotificationPreferenceResource updatePreference(@Valid @RequestBody UpdateNotificationPreferenceResource request) {
        var command = UpdateNotificationPreferenceCommandFromResourceAssembler.toCommandFromResource(request, requireAlertManagement());
        var preference = commandService.handle(command);
        return NotificationPreferenceResourceFromEntityAssembler.toResourceFromEntity(preference);
    }

    private Long requireAlertManagement() {
        return accessAuthorizationService.requirePermission(AccessPermission.MANAGE_ALERTS);
    }
}
