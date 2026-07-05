package com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources;

public record MultimediaConfigResource(
        String audioSource,
        double defaultVolume,
        boolean loopEnabled,
        String floatingButtonPosition
) {
}
