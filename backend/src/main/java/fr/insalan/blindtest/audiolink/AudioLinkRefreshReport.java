package fr.insalan.blindtest.audiolink;

/*
 * Résultat d'un balayage
 */
public record AudioLinkRefreshReport(
    int checked,
    int alive,
    int refreshed,
    int failed
) {}
