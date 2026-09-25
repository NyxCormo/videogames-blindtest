package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.model.Franchise;

public record FranchiseResponse(
    Integer id,
    String name
) {
    public static FranchiseResponse from(Franchise franchise) {
        return new FranchiseResponse(franchise.getId(), franchise.getName());
    }
}
