package fr.insalan.blindtest.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import jakarta.servlet.http.HttpServletResponse;

// Réexporte toute la base dans le même format (colonnes) que le Google Sheet d'origine (voir
// docs/import-gsheet.md), pour garder une copie durable des données en dehors de la base.
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final FranchiseRepository franchiseRepository;
    private final GameRepository gameRepository;
    private final TrackRepository trackRepository;
    private final ListenerRepository listenerRepository;
    private final KnowledgeRepository knowledgeRepository;

    public ExportController(
        FranchiseRepository franchiseRepository,
        GameRepository gameRepository,
        TrackRepository trackRepository,
        ListenerRepository listenerRepository,
        KnowledgeRepository knowledgeRepository
    ) {
        this.franchiseRepository = franchiseRepository;
        this.gameRepository = gameRepository;
        this.trackRepository = trackRepository;
        this.listenerRepository = listenerRepository;
        this.knowledgeRepository = knowledgeRepository;
    }

    @GetMapping("/sheet")
    public void exportSheet(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(
            "Content-Disposition",
            "attachment; filename=\"blindtest-export-" + LocalDate.now() + ".csv\""
        );

        List<Franchise> franchises = franchiseRepository.findAllByOrderByNameAsc();
        List<Listener> listeners = listenerRepository.findAllByOrderByNameAsc();
        Map<Integer, List<Game>> gamesByFranchise = gameRepository.findAllWithFranchise().stream()
            .collect(Collectors.groupingBy(game -> game.getFranchise().getId()));
        Map<Integer, List<Track>> tracksByGame = trackRepository.findAllWithGameAndFranchise().stream()
            .collect(Collectors.groupingBy(track -> track.getGame().getId()));
        Map<Integer, Map<Integer, Boolean>> votesByTrack = new HashMap<>();
        for (Knowledge knowledge : knowledgeRepository.findAll()) {
            votesByTrack
                .computeIfAbsent(knowledge.getTrack().getId(), id -> new HashMap<>())
                .put(knowledge.getListener().getId(), knowledge.isKnows());
        }

        try (CSVPrinter printer = new CSVPrinter(response.getWriter(), CSVFormat.DEFAULT)) {
            printer.printRecord(header(listeners));
            for (Franchise franchise : franchises) {
                List<Game> games = gamesByFranchise.getOrDefault(franchise.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(Game::getName))
                    .toList();
                if (games.isEmpty()) {
                    printer.printRecord(row(franchise.getName(), null, null, null, null, listeners, Map.of()));
                    continue;
                }
                for (Game game : games) {
                    List<Track> tracks = tracksByGame.getOrDefault(game.getId(), List.of()).stream()
                        .sorted(Comparator.comparing(Track::getName))
                        .toList();
                    if (tracks.isEmpty()) {
                        printer.printRecord(row(franchise.getName(), game.getName(), null, null, null, listeners, Map.of()));
                        continue;
                    }
                    for (Track track : tracks) {
                        Map<Integer, Boolean> votes = votesByTrack.getOrDefault(track.getId(), Map.of());
                        printer.printRecord(row(
                            franchise.getName(), game.getName(), track.getName(),
                            track.getKhinsiderLink(), track.getYoutubeLink(), listeners, votes
                        ));
                    }
                }
            }
        }
    }

    private List<String> header(List<Listener> listeners) {
        List<String> header = new ArrayList<>(List.of(
            "Licence", "Jeu", "OST", "Noms Valides", "Lien page web", "", "Lien audio", "Nb de vote", "Vote moyenne"
        ));
        listeners.forEach(listener -> header.add(listener.getName()));
        return header;
    }

    // "game"/"track" à null : ligne franchise seule ou franchise+jeu seuls, comme dans le Sheet d'origine.
    private List<String> row(
        String franchise, String game, String track,
        String khinsiderLink, String youtubeLink,
        List<Listener> listeners, Map<Integer, Boolean> votes
    ) {
        long known = votes.values().stream().filter(Boolean::booleanValue).count();
        List<String> cells = new ArrayList<>(List.of(
            franchise,
            game == null ? "" : game,
            track == null ? "" : track,
            "",
            khinsiderLink == null ? "" : khinsiderLink,
            "",
            youtubeLink == null ? "" : youtubeLink,
            String.valueOf(votes.size()),
            votes.isEmpty() ? "" : String.format(Locale.US, "%.2f", (double) known / votes.size())
        ));
        for (Listener listener : listeners) {
            Boolean knows = votes.get(listener.getId());
            cells.add(knows == null ? "" : (knows ? "1" : "0"));
        }
        return cells;
    }
}
