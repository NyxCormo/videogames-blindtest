package fr.insalan.blindtest.dto.admin;

import java.time.Instant;

import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.repository.ListenerUsage;

public record ListenerUsageResponse(
    Integer id,
    String name,
    Instant createdAt,
    Instant lastSeenAt,
    long votes,
    long blindtests
) {
    public static ListenerUsageResponse from(ListenerUsage usage, Listener listener) {
        return new ListenerUsageResponse(
            usage.getId(),
            usage.getName(),
            listener.getCreatedAt(),
            listener.getLastSeenAt(),
            usage.getVotes(),
            usage.getBlindtests()
        );
    }
}
