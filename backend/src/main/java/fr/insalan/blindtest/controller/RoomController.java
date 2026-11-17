package fr.insalan.blindtest.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import fr.insalan.blindtest.dto.blindtest.GuessFranchiseRequest;
import fr.insalan.blindtest.dto.blindtest.GuessRequest;
import fr.insalan.blindtest.dto.room.CreateRoomRequest;
import fr.insalan.blindtest.dto.room.RoomResponse;
import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.room.DrawMode;
import fr.insalan.blindtest.room.InfinitePicker;
import fr.insalan.blindtest.room.InfiniteSettings;
import fr.insalan.blindtest.room.Room;
import fr.insalan.blindtest.room.RoomEvents;
import fr.insalan.blindtest.room.RoomMode;
import fr.insalan.blindtest.room.RoomPlayer;
import fr.insalan.blindtest.room.RoomPhase;
import fr.insalan.blindtest.room.RoomRound;
import fr.insalan.blindtest.room.RoomService;
import fr.insalan.blindtest.room.RoomTimer;
import fr.insalan.blindtest.room.RoomTrack;
import fr.insalan.blindtest.room.RoomVisibility;
import fr.insalan.blindtest.room.ScoringMode;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private static final int DEFAULT_ANSWER_SECONDS = 30;
    private static final int MIN_ANSWER_SECONDS = 10;
    private static final int MAX_ANSWER_SECONDS = 120;
    private static final String INFINITE_NAME = "Salle infinie";
    private static final int DEFAULT_ATTEMPTS = 5;
    private static final int MAX_ATTEMPTS = 10;
    private static final int DEFAULT_FRANCHISE_GAP = 10;
    private static final int MAX_FRANCHISE_GAP = 50;

    private final RoomService roomService;
    private final RoomEvents roomEvents;
    private final RoomTimer roomTimer;
    private final BlindtestRepository blindtestRepository;
    private final BlindtestTrackRepository blindtestTrackRepository;
    private final ListenerRepository listenerRepository;
    private final TrackRepository trackRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final InfinitePicker infinitePicker;

    public RoomController(
        RoomService roomService,
        RoomEvents roomEvents,
        RoomTimer roomTimer,
        BlindtestRepository blindtestRepository,
        BlindtestTrackRepository blindtestTrackRepository,
        ListenerRepository listenerRepository,
        TrackRepository trackRepository,
        KnowledgeRepository knowledgeRepository,
        InfinitePicker infinitePicker
    ) {
        this.roomService = roomService;
        this.roomEvents = roomEvents;
        this.roomTimer = roomTimer;
        this.blindtestRepository = blindtestRepository;
        this.blindtestTrackRepository = blindtestTrackRepository;
        this.listenerRepository = listenerRepository;
        this.trackRepository = trackRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.infinitePicker = infinitePicker;
    }

    @GetMapping
    public List<RoomResponse> publicRooms() {
        return roomService.publicRooms().stream()
            .map(RoomResponse::from)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@RequestParam Integer listenerId, @RequestBody CreateRoomRequest request) {
        RoomPlayer host = player(listenerId);
        Integer blindtestId = null;
        String name = INFINITE_NAME;
        long trackCount = 0;
        int maxAttempts;
        InfiniteSettings infinite = null;
        if (Boolean.TRUE.equals(request.infinite())) {
            maxAttempts = between(request.maxAttempts(), DEFAULT_ATTEMPTS, 1, MAX_ATTEMPTS,
                "Le nombre d'essais doit être compris entre 1 et " + MAX_ATTEMPTS);
            infinite = new InfiniteSettings(
                request.drawMode() == null ? DrawMode.TRACK : request.drawMode(),
                between(request.franchiseGap(), DEFAULT_FRANCHISE_GAP, 0, MAX_FRANCHISE_GAP,
                    "Le délai avant de retrouver une franchise doit être compris entre 0 et " + MAX_FRANCHISE_GAP + " musiques")
            );
        } else {
            if (request.blindtestId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le blindtest est obligatoire");
            }
            Blindtest blindtest = blindtestRepository.findById(request.blindtestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blindtest introuvable"));
            trackCount = blindtestTrackRepository.countByBlindtestId(blindtest.getId());
            if (trackCount == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce blindtest n'a aucune musique");
            }
            blindtestId = blindtest.getId();
            name = blindtest.getName();
            maxAttempts = blindtest.getMaxAttempts();
        }
        int answerSeconds = between(request.answerSeconds(), DEFAULT_ANSWER_SECONDS, MIN_ANSWER_SECONDS, MAX_ANSWER_SECONDS,
            "Le temps de réponse doit être compris entre " + MIN_ANSWER_SECONDS + " et " + MAX_ANSWER_SECONDS + " secondes");
        Room room = roomService.create(
            blindtestId,
            name,
            trackCount,
            host,
            request.visibility() == null ? RoomVisibility.PUBLIC : request.visibility(),
            request.mode() == null ? RoomMode.MULTI : request.mode(),
            request.playersCanSeek() == null || request.playersCanSeek(),
            request.scoringMode() == null ? ScoringMode.SPEED : request.scoringMode(),
            answerSeconds,
            maxAttempts,
            infinite
        );
        roomTimer.closeIfNeverStarted(room);
        return RoomResponse.from(room);
    }

    @GetMapping("/{code}")
    public RoomResponse get(@PathVariable String code) {
        return RoomResponse.from(room(code));
    }

    @PostMapping("/{code}/players")
    public RoomResponse join(@PathVariable String code, @RequestParam Integer listenerId) {
        Room room = room(code);
        if (room.getPhase() == RoomPhase.FINISHED) {
            return RoomResponse.from(room);
        }
        if (room.join(player(listenerId), Instant.now())) {
            roomEvents.publish(room);
        }
        return RoomResponse.from(room);
    }

    @PostMapping("/{code}/start")
    public RoomResponse start(@PathVariable String code, @RequestParam Integer listenerId) {
        Room room = room(code);
        if (!room.getHostId().equals(listenerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seule la personne qui a créé la salle peut lancer la partie");
        }
        List<RoomTrack> tracks = room.isInfinite() ? firstInfiniteTrack(room) : blindtestTracks(room);
        if (tracks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune musique à jouer");
        }
        try {
            roomTimer.start(room, tracks);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return RoomResponse.from(room);
    }

    @PostMapping("/{code}/stop")
    public RoomResponse stop(@PathVariable String code, @RequestParam Integer listenerId) {
        Room room = room(code);
        if (!room.getHostId().equals(listenerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seule la personne qui a créé la salle peut arrêter la partie");
        }
        if (!roomTimer.stop(room)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La partie est déjà terminée");
        }
        return RoomResponse.from(room);
    }

    @PostMapping("/{code}/reveal")
    public RoomResponse reveal(@PathVariable String code, @RequestParam Integer listenerId) {
        Room room = presentedBy(code, listenerId);
        if (!roomTimer.reveal(room, room.getPosition())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucune musique à révéler");
        }
        return RoomResponse.from(room);
    }

    @PostMapping("/{code}/next")
    public RoomResponse next(@PathVariable String code, @RequestParam Integer listenerId) {
        Room room = presentedBy(code, listenerId);
        if (!roomTimer.next(room, room.getPosition())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La musique n'a pas encore été révélée");
        }
        return RoomResponse.from(room);
    }

    @PostMapping("/{code}/presence")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void presence(@PathVariable String code, @RequestParam Integer listenerId) {
        Room room = room(code, listenerId);
        if (room.seen(listenerId, Instant.now())) {
            roomTimer.presenceChanged(room);
        }
    }

    @PostMapping("/{code}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable String code, @RequestParam Integer listenerId) {
        Room room = room(code);
        if (room.leave(listenerId)) {
            roomTimer.presenceChanged(room);
        }
    }

    @GetMapping("/{code}/round")
    public RoomRound round(@PathVariable String code, @RequestParam Integer listenerId) {
        return room(code, listenerId).round(listenerId);
    }

    @PostMapping("/{code}/guess")
    public RoomRound guess(@PathVariable String code, @RequestParam Integer listenerId, @RequestBody GuessRequest request) {
        if (request.gameId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le jeu est obligatoire");
        }
        Room room = room(code, listenerId);
        try {
            return answered(room, listenerId, room.guess(listenerId, request.gameId(), request.trackId(), Instant.now()));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/{code}/guess-franchise")
    public RoomRound guessFranchise(
        @PathVariable String code,
        @RequestParam Integer listenerId,
        @RequestBody GuessFranchiseRequest request
    ) {
        if (request.franchiseId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La franchise est obligatoire");
        }
        Room room = room(code, listenerId);
        try {
            return answered(room, listenerId, room.guessFranchise(listenerId, request.franchiseId(), Instant.now()));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/{code}/pass")
    public RoomRound pass(@PathVariable String code, @RequestParam Integer listenerId) {
        Room room = room(code, listenerId);
        try {
            return answered(room, listenerId, room.pass(listenerId, Instant.now()));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @GetMapping(path = "/{code}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String code) {
        return roomEvents.subscribe(room(code));
    }

    private List<RoomTrack> firstInfiniteTrack(Room room) {
        RoomTrack first = infinitePicker.pick(room);
        return first == null ? List.of() : List.of(first);
    }

    private List<RoomTrack> blindtestTracks(Room room) {
        return blindtestTrackRepository.findWithTrackByBlindtestId(room.getBlindtestId()).stream()
            .map(blindtestTrack -> RoomTrack.from(blindtestTrack.getTrack()))
            .toList();
    }

    private static int between(Integer value, int byDefault, int min, int max, String error) {
        int result = value == null ? byDefault : value;
        if (result < min || result > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
        return result;
    }

    private Room room(String code) {
        return roomService.find(code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salle introuvable"));
    }

    private Room room(String code, Integer listenerId) {
        Room room = room(code);
        if (!room.hasPlayer(listenerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Il faut d'abord rejoindre la salle");
        }
        return room;
    }

    private Room presentedBy(String code, Integer listenerId) {
        Room room = room(code);
        if (!room.getHostId().equals(listenerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seule la personne qui anime la salle peut faire ça");
        }
        if (room.getMode() != RoomMode.PRESENTATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La salle n'est pas en mode présentation");
        }
        return room;
    }

    private RoomRound answered(Room room, Integer listenerId, RoomRound round) {
        if (round.done()) {
            recordKnowledge(listenerId, round.trackId(), round.gameFound());
            roomEvents.publish(room);
            roomTimer.revealIfEveryoneDone(room);
        }
        return round;
    }

    private void recordKnowledge(Integer listenerId, Integer trackId, boolean knows) {
        listenerRepository.findById(listenerId).ifPresent(listener ->
            trackRepository.findById(trackId).ifPresent(track ->
                knowledgeRepository.save(new Knowledge(listener, track, knows))));
    }

    private RoomPlayer player(Integer listenerId) {
        Listener listener = listenerRepository.findById(listenerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pseudo introuvable"));
        return new RoomPlayer(listener.getId(), listener.getName());
    }
}
