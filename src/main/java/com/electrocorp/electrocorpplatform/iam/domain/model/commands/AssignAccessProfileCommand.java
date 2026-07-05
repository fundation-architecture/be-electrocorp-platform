package com.electrocorp.electrocorpplatform.iam.domain.model.commands;

public record AssignAccessProfileCommand(
        Long actorUserId,
        Long targetUserId,
        Long accessProfileId
) {
}
