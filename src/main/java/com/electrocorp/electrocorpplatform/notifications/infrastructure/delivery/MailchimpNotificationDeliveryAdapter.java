package com.electrocorp.electrocorpplatform.notifications.infrastructure.delivery;

import com.electrocorp.electrocorpplatform.iam.domain.repositories.UserRepository;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.Alert;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.NotificationPreference;
import com.electrocorp.electrocorpplatform.notifications.domain.model.results.NotificationDeliveryDecision;
import com.electrocorp.electrocorpplatform.notifications.domain.model.valueobjects.NotificationDeliveryMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MailchimpNotificationDeliveryAdapter implements NotificationDeliveryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailchimpNotificationDeliveryAdapter.class);

    private final RestClient restClient;
    private final UserRepository userRepository;

    @Value("${electrocorp.integrations.mailchimp.api-key:}")
    private String mailchimpApiKey;

    @Value("${electrocorp.integrations.mailchimp.messages-url:https://mandrillapp.com/api/1.0/messages/send.json}")
    private String mailchimpMessagesUrl;

    @Value("${electrocorp.integrations.mailchimp.from-email:no-reply@electrocorp.app}")
    private String fromEmail;

    @Value("${electrocorp.integrations.mailchimp.from-name:ElectroCorp}")
    private String fromName;

    public MailchimpNotificationDeliveryAdapter(RestClient.Builder restClientBuilder, UserRepository userRepository) {
        this.restClient = restClientBuilder.build();
        this.userRepository = userRepository;
    }

    @Override
    public void send(Alert alert, NotificationPreference preference, NotificationDeliveryDecision decision) {
        if (alert == null || decision == null || !decision.allowed()) {
            return;
        }

        List<String> channels = channelsFor(preference, decision);
        if (channels.isEmpty()) {
            return;
        }

        boolean mailchimpSent = false;
        if (channels.contains("EMAIL")) {
            mailchimpSent = sendMailchimpEmail(alert);
        }

        LOGGER.info(
                "ElectroCorp notification delivery alert={} mode={} channels={} mailchimpSent={} reason={} title={}",
                alert.getId(),
                decision.deliveryMode(),
                String.join(",", channels),
                mailchimpSent,
                decision.reason(),
                alert.getTitle()
        );
    }

    private List<String> channelsFor(NotificationPreference preference, NotificationDeliveryDecision decision) {
        List<String> channels = new ArrayList<>();

        if (decision.deliveryMode() == NotificationDeliveryMode.MUTED) {
            if (decision.inboxAllowed()) {
                channels.add("IN_APP_INBOX");
            }
            return channels;
        }

        if (preference != null && Boolean.TRUE.equals(preference.getEmailEnabled())) {
            channels.add("EMAIL");
        }

        if (preference != null && Boolean.TRUE.equals(preference.getPushEnabled()) && decision.soundAllowed()) {
            channels.add("PUSH");
        }

        if (decision.inboxAllowed()) {
            channels.add("IN_APP_INBOX");
        }

        if (decision.toastAllowed()) {
            channels.add("TOAST");
        }

        return channels;
    }

    private boolean sendMailchimpEmail(Alert alert) {
        if (!hasMailchimpCredentials()) {
            LOGGER.info("Mailchimp delivery skipped for alert {} because API credentials are not configured.", alert.getId());
            return false;
        }

        Optional<String> recipientEmail = resolveRecipientEmail(alert);
        if (recipientEmail.isEmpty()) {
            LOGGER.warn("Mailchimp delivery skipped for alert {} because the recipient email could not be resolved.", alert.getId());
            return false;
        }

        Map<String, Object> payload = Map.of(
                "key", mailchimpApiKey.trim(),
                "message", Map.of(
                        "subject", safe(alert.getTitle()),
                        "text", buildEmailText(alert),
                        "from_email", fromEmail,
                        "from_name", fromName,
                        "to", List.of(Map.of(
                                "email", recipientEmail.get(),
                                "type", "to"
                        ))
                ),
                "async", false
        );

        try {
            restClient.post()
                    .uri(mailchimpMessagesUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException exception) {
            LOGGER.warn("Mailchimp delivery failed for alert {}.", alert.getId(), exception);
            return false;
        }
    }

    private Optional<String> resolveRecipientEmail(Alert alert) {
        if (alert.getUserId() == null) {
            return Optional.empty();
        }

        return userRepository.findById(alert.getUserId())
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .map(user -> user.getEmail().trim().toLowerCase());
    }

    private boolean hasMailchimpCredentials() {
        return mailchimpApiKey != null && !mailchimpApiKey.isBlank()
                && mailchimpMessagesUrl != null && !mailchimpMessagesUrl.isBlank();
    }

    private String buildEmailText(Alert alert) {
        List<String> lines = new ArrayList<>();
        lines.add(safe(alert.getMessage()));

        if (alert.getEvidence() != null && !alert.getEvidence().isBlank()) {
            lines.add("Evidence: " + alert.getEvidence());
        }

        if (alert.getExplanation() != null && !alert.getExplanation().isBlank()) {
            lines.add("Explanation: " + alert.getExplanation());
        }

        if (alert.getRecommendedAction() != null && !alert.getRecommendedAction().isBlank()) {
            lines.add("Recommended action: " + alert.getRecommendedAction());
        }

        return String.join(System.lineSeparator() + System.lineSeparator(), lines);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
