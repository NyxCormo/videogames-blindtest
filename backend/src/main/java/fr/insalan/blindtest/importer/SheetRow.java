package fr.insalan.blindtest.importer;

import java.util.Map;

/**
 * Une ligne du Google Sheet. Les cellules vides valent null.
 */
public record SheetRow(
        String franchise,
        String game,
        String track,
        String khinsiderLink,
        String youtubeLink,
        Map<String, Boolean> votes) {
}
