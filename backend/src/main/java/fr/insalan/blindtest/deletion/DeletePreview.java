package fr.insalan.blindtest.deletion;

// Ce que la suppression emporterait, ou pourquoi elle est refusée.
public record DeletePreview(boolean allowed, String impact) {}
