package com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources;

import java.time.LocalDateTime;

public record MultimediaStateResource(
        Boolean audioEnabled,
        Boolean menuOpen,
        LocalDateTime updatedAt
) {
}
