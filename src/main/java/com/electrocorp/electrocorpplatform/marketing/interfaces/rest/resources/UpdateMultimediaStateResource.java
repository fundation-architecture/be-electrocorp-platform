package com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources;

public record UpdateMultimediaStateResource(
        Boolean audioEnabled,
        Boolean menuOpen
) {
}
