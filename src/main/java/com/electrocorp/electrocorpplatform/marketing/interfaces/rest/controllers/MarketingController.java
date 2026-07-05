package com.electrocorp.electrocorpplatform.marketing.interfaces.rest.controllers;

import com.electrocorp.electrocorpplatform.marketing.application.services.MarketingApplicationService;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.LandingContentResource;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.MarketingSearchResultResource;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.MultimediaConfigResource;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.MultimediaStateResource;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.NewsletterSubscriptionRequestResource;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.NewsletterSubscriptionResource;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.TranslationBundleResource;
import com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources.UpdateMultimediaStateResource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MarketingController {

    private final MarketingApplicationService marketingApplicationService;

    @GetMapping("/marketing/landing-content")
    public LandingContentResource getLandingContent(@RequestParam(defaultValue = "en") String lang) {
        return marketingApplicationService.getLandingContent(lang);
    }

    @GetMapping("/translations")
    public TranslationBundleResource getTranslations(@RequestParam(defaultValue = "en") String lang) {
        return marketingApplicationService.getTranslations(lang);
    }

    @GetMapping("/marketing/search")
    public MarketingSearchResultResource search(@RequestParam String term) {
        return marketingApplicationService.search(term);
    }

    @PostMapping("/newsletter/subscriptions")
    public NewsletterSubscriptionResource subscribe(@Valid @RequestBody NewsletterSubscriptionRequestResource request) {
        return marketingApplicationService.subscribe(request.email(), request.source());
    }

    @GetMapping("/marketing/multimedia-config")
    public MultimediaConfigResource multimediaConfig() {
        return marketingApplicationService.getMultimediaConfig();
    }

    @PostMapping("/marketing/multimedia-state")
    public MultimediaStateResource updateMultimediaState(@RequestBody UpdateMultimediaStateResource request) {
        return marketingApplicationService.updateMultimediaState(request.audioEnabled(), request.menuOpen());
    }
}
