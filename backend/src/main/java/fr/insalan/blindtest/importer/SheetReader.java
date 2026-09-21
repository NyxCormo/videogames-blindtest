package fr.insalan.blindtest.importer;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;


/**
 * Lit l'export CSV du Google Sheet. Le format est décrit dans docs/import-gsheet.md.
 */
public class SheetReader {

    // Colonnes lues par position (A = 0)
    private static final int FRANCHISE = 0;
    private static final int GAME = 1;
    private static final int TRACK = 2;
    private static final int KHINSIDER_LINK = 4;
    private static final int AUDIO_LINK = 6;
    private static final int FIRST_VOTER = 9;

    private static final int HEADER_INDEX = 3;
    
    private static final Pattern YOUTUBE = Pattern.compile(
        "^https?://([^/]+\\.)?(youtube\\.com|youtu\\.be)/", Pattern.CASE_INSENSITIVE
    );

    public List<SheetRow> read(Reader reader) throws IOException {
        List<CSVRecord> records;
        try (CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {
            records = parser.getRecords();
        }
        if (records.size() <= HEADER_INDEX || records.get(HEADER_INDEX).size() <= FIRST_VOTER) {
            throw new IllegalArgumentException("Format inattendu");
        }
        
        CSVRecord header = records.get(HEADER_INDEX);
        List<SheetRow> rows = new ArrayList<>();
        for (CSVRecord record : records.subList(HEADER_INDEX + 1, records.size())) {
            String franchise = cell(record, FRANCHISE);
            String game = cell(record, GAME);
            String track = cell(record, TRACK);
            if (franchise == null && game == null && track == null) {
                continue;
            }
            rows.add(new SheetRow(
                franchise, 
                game, 
                track,
                cell(record, KHINSIDER_LINK),
                youtubeLink(record),
                votes(header, record)
            ));
        }
        return rows;
    }

    //Seuls les liens YouTube sont repris
    private String youtubeLink(CSVRecord record) {
        String link = cell(record, AUDIO_LINK);
        if (link == null || !YOUTUBE.matcher(link).find()) {
            return null;
        }
        return link;
    }

    private Map<String, Boolean> votes(CSVRecord header, CSVRecord record) {
        Map<String, Boolean> votes = new LinkedHashMap<>();
        for (int column = FIRST_VOTER; column < header.size(); column++) {
            String voter = cell(header, column);
            String vote = cell(record, column);
            if (voter != null && vote != null) {
                if (vote.equals("1")) {
                    votes.put(voter, true);
                } else if (vote.equals("0")) {
                    votes.put(voter, false);
                }
            }
        }
        return votes;
    }

    private String cell(CSVRecord record, int column) {
        if (column >= record.size()) {
            return null;
        }
        String value = record.get(column).strip();
        return value.isEmpty() ? null : value;
    }
}
