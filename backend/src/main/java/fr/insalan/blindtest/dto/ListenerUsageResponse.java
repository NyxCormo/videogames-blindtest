package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.repository.ListenerUsage;

public record ListenerUsageResponse(
    Integer id,
    String name,
    long votes,
    long blindtests
) {
    public static ListenerUsageResponse from(ListenerUsage usage) {
        return new ListenerUsageResponse(usage.getId(), usage.getName(), usage.getVotes(), usage.getBlindtests());
    }
}
