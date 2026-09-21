package fr.insalan.blindtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.insalan.blindtest.importer.SheetReader;
import fr.insalan.blindtest.importer.SheetRow;

class SheetReaderTests {

    // Trois lignes de consignes, puis les en-têtes (colonnes A à I, puis deux votants)
    private static final String HEADER = """
            Titre,,,,,,,,,,
            Consigne,,,,,,,,,,
            Consigne,,,,,,,,,,
            Licence,Jeu,OST,Noms Valides,Lien page web,,Lien audio,Nb de vote,Vote moyenne,UserA,UserB
            """;

    private final SheetReader reader = new SheetReader();

    private List<SheetRow> read(String rows) throws IOException {
        return reader.read(new StringReader(HEADER + rows));
    }

    @Test
    void readsStellarBladeFile() throws IOException {
        List<SheetRow> rows;
        try (Reader file = new InputStreamReader(
                getClass().getResourceAsStream("/import/stellar-blade.csv"), StandardCharsets.UTF_8)) {
            rows = reader.read(file);
        }

        assertEquals(14, rows.size());

        SheetRow dawn = rows.get(0);
        assertEquals("Stellar Blade (Licence)", dawn.franchise());
        assertEquals("Stellar Blade", dawn.game());
        assertEquals("Dawn", dawn.track());
        assertTrue(dawn.khinsiderLink().startsWith("https://downloads.khinsider.com/"));
        assertEquals(Map.of("UserG", true, "UserJ", true), dawn.votes());

        // Le lien audio de Raven n'est pas un lien YouTube : il est ignoré
        SheetRow raven = rows.get(1);
        assertEquals("Raven", raven.track());
        assertNull(raven.khinsiderLink());
        assertNull(raven.youtubeLink());

        SheetRow shael = rows.stream().filter(row -> row.track().equals("Shaël")).findFirst().orElseThrow();
        assertEquals(Map.of("UserG", false, "UserJ", true), shael.votes());
    }

    @Test
    void keepsOnlyYoutubeAudioLinks() throws IOException {
        List<SheetRow> rows = read("""
                Stellar Blade,Stellar Blade,Dawn,,,,https://www.youtube.com/watch?v=exemple,,,,
                Stellar Blade,Stellar Blade,Raven,,,,https://audio.jukehost.co.uk/exemple,,,,
                """);

        assertEquals("https://www.youtube.com/watch?v=exemple", rows.get(0).youtubeLink());
        assertNull(rows.get(1).youtubeLink());
    }

    @Test
    void keepsRowsWithoutTrack() throws IOException {
        List<SheetRow> rows = read("""
                Stellar Blade,Stellar Blade,,,,,,,,,
                Stellar Blade,,,,,,,,,,
                """);

        assertEquals(2, rows.size());
        assertEquals("Stellar Blade", rows.get(0).game());
        assertNull(rows.get(0).track());
        assertNull(rows.get(1).game());
    }

    @Test
    void skipsRowsWithoutFranchiseGameAndTrack() throws IOException {
        List<SheetRow> rows = read("""
                ,,,,,,,,,,
                ,,,,,,,,,1,0
                Stellar Blade,Stellar Blade,Dawn,,,,,,,,
                """);

        assertEquals(1, rows.size());
        assertEquals("Dawn", rows.get(0).track());
    }

    @Test
    void trimsCellsAndIgnoresBlankVotes() throws IOException {
        List<SheetRow> rows = read("""
                " Stellar Blade ", Stellar Blade ,Dawn,,,,,,,1," "
                """);

        assertEquals("Stellar Blade", rows.get(0).franchise());
        assertEquals("Stellar Blade", rows.get(0).game());
        assertEquals(Map.of("UserA", true), rows.get(0).votes());
    }

    @Test
    void keepsCaseAsIs() throws IOException {
        List<SheetRow> rows = read("""
                Stellar Blade,Stellar Blade,Dawn,,,,,,,,
                Stellar blade,Stellar Blade,Raven,,,,,,,,
                """);

        assertEquals("Stellar Blade", rows.get(0).franchise());
        assertEquals("Stellar blade", rows.get(1).franchise());
    }

    @Test
    void rejectsSemicolonExport() {
        String excelExport = "Titre;;;\nConsigne;;;\nConsigne;;;\nLicence;Jeu;OST;Noms Valides\n";

        assertThrows(IllegalArgumentException.class, () -> reader.read(new StringReader(excelExport)));
    }
}
