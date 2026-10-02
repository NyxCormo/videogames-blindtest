package fr.insalan.blindtest.merge;

// Ce que la fusion déplacerait, ou pourquoi elle est refusée.
public record MergePreview(boolean allowed, String summary) {}
