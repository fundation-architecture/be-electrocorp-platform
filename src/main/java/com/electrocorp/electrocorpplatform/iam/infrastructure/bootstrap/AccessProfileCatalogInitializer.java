package com.electrocorp.electrocorpplatform.iam.infrastructure.bootstrap;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.AccessProfile;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.AccessProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AccessProfileCatalogInitializer implements ApplicationRunner {

    private final AccessProfileRepository accessProfileRepository;

    @Override
    public void run(ApplicationArguments args) {
        Map<String, String> profiles = Map.of(
                "OWNER", "Propietario de cuenta con administracion completa.",
                "ADMIN", "Administrador operativo con gestion de espacios, dispositivos y accesos.",
                "MEMBER", "Miembro con operacion diaria, reportes, alertas y soporte.",
                "GUEST", "Invitado con control limitado de dispositivos."
        );

        profiles.forEach((name, description) ->
                accessProfileRepository.findByName(name)
                        .orElseGet(() -> accessProfileRepository.save(new AccessProfile(name, description)))
        );
    }
}
