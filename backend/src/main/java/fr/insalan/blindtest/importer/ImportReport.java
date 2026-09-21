package fr.insalan.blindtest.importer;


/**
 * Résultat d'une importation de Google Sheet. Informe de ce qui a été créé, mis à jour ou ignoré.
 */
public record ImportReport(
    int franchisesCreated, 
    int gamesCreated, 
    int tracksCreated, 
    int listenersCreated,
    int votesCreated,
    int rowsIgnored
){}
