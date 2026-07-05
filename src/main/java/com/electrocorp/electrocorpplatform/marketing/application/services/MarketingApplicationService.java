package com.electrocorp.electrocorpplatform.marketing.application.services;

import com.electrocorp.electrocorpplatform.marketing.domain.model.aggregates.NewsletterSubscription;
import com.electrocorp.electrocorpplatform.marketing.domain.repositories.NewsletterSubscriptionRepository;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.LandingContentResource;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.MarketingSearchResultResource;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.MultimediaConfigResource;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.MultimediaStateResource;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.NewsletterSubscriptionResource;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.TranslationBundleResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketingApplicationService {

    private final NewsletterSubscriptionRepository newsletterSubscriptionRepository;

    @Transactional(readOnly = true)
    public LandingContentResource getLandingContent(String language) {
        String lang = normalizeLanguage(language);
        Map<String, String> messages = translationsFor(lang);

        return new LandingContentResource(
                lang,
                messages.get("hero_title"),
                messages.get("hero_subtitle"),
                List.of(
                        messages.get("desc_text1"),
                        messages.get("desc_text3"),
                        messages.get("goals_text")
                ),
                List.of(
                        new LandingContentResource.PlanSummaryResource("STARTER", messages.get("starter_plan_name"), "S/ 19.00", messages.get("starter_plan_desc")),
                        new LandingContentResource.PlanSummaryResource("PROFESSIONAL", messages.get("professional_plan_name"), "S/ 49.00", messages.get("professional_plan_desc")),
                        new LandingContentResource.PlanSummaryResource("ENTERPRISE", messages.get("enterprise_plan_name"), "S/ 99.00", messages.get("enterprise_plan_desc"))
                ),
                List.of(
                        new LandingContentResource.TestimonialResource("Ana", messages.get("testimonials_rolePremium"), messages.get("testimonials_q1")),
                        new LandingContentResource.TestimonialResource("Luis", messages.get("testimonials_roleFree"), messages.get("testimonials_q3"))
                )
        );
    }

    @Transactional(readOnly = true)
    public TranslationBundleResource getTranslations(String language) {
        String lang = normalizeLanguage(language);
        return new TranslationBundleResource(lang, translationsFor(lang));
    }

    @Transactional(readOnly = true)
    public MarketingSearchResultResource search(String term) {
        String normalized = term == null ? "" : term.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return new MarketingSearchResultResource("", null, "Empty search", false);
        }

        for (var entry : searchDictionary().entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return new MarketingSearchResultResource(term, entry.getValue(), entry.getKey(), true);
            }
        }

        return new MarketingSearchResultResource(term, null, "No matching section", false);
    }

    @Transactional
    public NewsletterSubscriptionResource subscribe(String email, String source) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        NewsletterSubscription subscription = newsletterSubscriptionRepository.findByEmail(normalizedEmail)
                .map(existing -> {
                    existing.reactivate(source);
                    return existing;
                })
                .orElseGet(() -> new NewsletterSubscription(normalizedEmail, source));

        return NewsletterSubscriptionResource.from(newsletterSubscriptionRepository.save(subscription));
    }

    @Transactional(readOnly = true)
    public MultimediaConfigResource getMultimediaConfig() {
        return new MultimediaConfigResource(
                "audio/ElectroCorp.mp3",
                0.3,
                true,
                "bottom-right"
        );
    }

    @Transactional(readOnly = true)
    public MultimediaStateResource updateMultimediaState(Boolean audioEnabled, Boolean menuOpen) {
        return new MultimediaStateResource(
                Boolean.TRUE.equals(audioEnabled),
                Boolean.TRUE.equals(menuOpen),
                LocalDateTime.now()
        );
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }

        String normalized = language.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "es", "en", "pt" -> normalized;
            default -> "en";
        };
    }

    private Map<String, String> searchDictionary() {
        return Map.ofEntries(
                Map.entry("inicio", "hero"),
                Map.entry("home", "hero"),
                Map.entry("descripcion", "description"),
                Map.entry("startup", "description"),
                Map.entry("planes", "service"),
                Map.entry("precios", "service"),
                Map.entry("pricing", "service"),
                Map.entry("testimonios", "reviews"),
                Map.entry("reviews", "reviews"),
                Map.entry("equipo", "about"),
                Map.entry("team", "about"),
                Map.entry("soporte", "contact"),
                Map.entry("contact", "contact"),
                Map.entry("boletin", "contact")
        );
    }

    private Map<String, String> translationsFor(String language) {
        Map<String, String> values = new LinkedHashMap<>();
        switch (language) {
            case "es" -> fillSpanish(values);
            case "pt" -> fillPortuguese(values);
            default -> fillEnglish(values);
        }
        return values;
    }

    private void fillSpanish(Map<String, String> values) {
        values.put("nav_home", "Inicio");
        values.put("nav_plans", "Planes");
        values.put("nav_reviews", "Testimonios");
        values.put("nav_about", "Nosotros");
        values.put("nav_support", "Soporte");
        values.put("hero_title", "Domina el consumo electrico de tu hogar");
        values.put("hero_subtitle", "15% de descuento por 3 meses para los primeros 50 nuevos usuarios");
        values.put("desc_text1", "Startup enfocada en democratizar la domotica y eficiencia energetica.");
        values.put("desc_text3", "Ecosistema IoT plug-and-play con control remoto y monitoreo en tiempo real.");
        values.put("goals_text", "Gestionar consumo electrico de manera eficiente, economica y sostenible.");
        values.put("starter_plan_name", "Plan Starter");
        values.put("starter_plan_desc", "Gestion basica para hogares pequenos.");
        values.put("professional_plan_name", "Plan Professional");
        values.put("professional_plan_desc", "Control avanzado para hogares y pequenos negocios.");
        values.put("enterprise_plan_name", "Plan Enterprise");
        values.put("enterprise_plan_desc", "Plataforma completa para multiples locales.");
        values.put("testimonials_rolePremium", "Cliente Premium");
        values.put("testimonials_roleFree", "Usuario gratuito");
        values.put("testimonials_q1", "ElectroCorp muestra que equipo consume mas y ayuda a bajar el recibo.");
        values.put("testimonials_q3", "Programo enchufes y veo el consumo al instante.");
    }

    private void fillEnglish(Map<String, String> values) {
        values.put("nav_home", "Home");
        values.put("nav_plans", "Pricing");
        values.put("nav_reviews", "Reviews");
        values.put("nav_about", "About us");
        values.put("nav_support", "Support");
        values.put("hero_title", "Take control of your home's electricity consumption");
        values.put("hero_subtitle", "15% discount for 3 months for the first 50 new users");
        values.put("desc_text1", "Startup focused on democratizing home automation and energy efficiency.");
        values.put("desc_text3", "Plug-and-play IoT ecosystem with remote control and real-time monitoring.");
        values.put("goals_text", "Manage electricity consumption efficiently, affordably, and sustainably.");
        values.put("starter_plan_name", "Starter Plan");
        values.put("starter_plan_desc", "Basic energy management for small homes.");
        values.put("professional_plan_name", "Professional Plan");
        values.put("professional_plan_desc", "Advanced control for homes and small businesses.");
        values.put("enterprise_plan_name", "Enterprise Plan");
        values.put("enterprise_plan_desc", "Complete platform for multiple business locations.");
        values.put("testimonials_rolePremium", "Premium customer");
        values.put("testimonials_roleFree", "Free user");
        values.put("testimonials_q1", "ElectroCorp shows which appliance is driving watts and helps lower the bill.");
        values.put("testimonials_q3", "I schedule appliances and watch consumption in real time.");
    }

    private void fillPortuguese(Map<String, String> values) {
        values.put("nav_home", "Inicio");
        values.put("nav_plans", "Planos");
        values.put("nav_reviews", "Avaliacoes");
        values.put("nav_about", "Sobre nos");
        values.put("nav_support", "Suporte");
        values.put("hero_title", "Assuma o controle do consumo eletrico da sua casa");
        values.put("hero_subtitle", "15% de desconto por 3 meses para os primeiros 50 novos usuarios");
        values.put("desc_text1", "Startup focada em democratizar automacao residencial e eficiencia energetica.");
        values.put("desc_text3", "Ecossistema IoT plug-and-play com controle remoto e monitoramento em tempo real.");
        values.put("goals_text", "Gerenciar consumo eletrico de forma eficiente, economica e sustentavel.");
        values.put("starter_plan_name", "Plano Starter");
        values.put("starter_plan_desc", "Gestao basica para casas pequenas.");
        values.put("professional_plan_name", "Plano Professional");
        values.put("professional_plan_desc", "Controle avancado para residencias e pequenos negocios.");
        values.put("enterprise_plan_name", "Plano Enterprise");
        values.put("enterprise_plan_desc", "Plataforma completa para varias sedes.");
        values.put("testimonials_rolePremium", "Cliente Premium");
        values.put("testimonials_roleFree", "Usuario gratuito");
        values.put("testimonials_q1", "ElectroCorp mostra qual aparelho consome mais e ajuda a reduzir a conta.");
        values.put("testimonials_q3", "Programo tomadas e vejo o consumo em tempo real.");
    }
}
