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
import fr.insalan.blindtest.room.Room;
import fr.insalan.blindtest.room.RoomEvents;
import fr.insalan.blindtest.room.RoomPlayer;
import fr.insalan.blindtest.room.RoomPhase;
import fr.insalan.blindtest.room.RoomRound;
import fr.insalan.blindtest.room.RoomService;
import fr.insalan.blindtest.room.RoomTimer;
import fr.insalan.blindtest.room.RoomTrack;
import fr.insalan.blindtest.room.RoomVisibility;
import fr.insalan.blindtest.room.ScoringMode;
import fr.insalan.blindtest.room.SoundMode;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private static final int DEFAULT_ANSWER_SECONDS = 30;
    private static final int MIN_ANSWER_SECONDS = 10;
    private static final int MAX_ANSWER_SECONDS = 120;

    private final RoomService roomService;
    private final RoomEvents roomEvents;
    private final RoomTimer roomTimer;
    private final BlindtestRepository blindtestRepository;
    private final BlindtestTrackRepository blindtestTrackRepository;
    private final ListenerRepository listenerRepository;
    private final TrackRepository trackRepository;
    private final KnowledgeRepository knowledgeRepository;

    public RoomController(
        RoomService roomService,
        RoomEvents roomEvents,
        RoomTimer roomTimer,
        BlindtestRepository blindtestRepository,
        BlindtestTrackRepository blindtestTrackRepository,
        ListenerRepository listenerRepository,
        TrackRepository trackRepository,
        KnowledgeRepository knowledgeRepository
    ) {
        this.roomService = roomService;
        this.roomEvents = roomEvents;
        this.roomTimer = roomTimer;
        this.blindtestRepository = blindtestRepository;
        this.blindtestTrackRepository = blindtestTrackRepository;
        this.listenerRepository = listenerRepository;
        this.trackRepository = trackRepository;
        this.knowledgeRepository = knowledgeRepository;
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
        if (request.blindtestId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le blindtest est obligatoire");
        }
        Blindtest blindtest = blindtestRepository.findById(request.blindtestId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blindtest introuvable"));
        long trackCount = blindtestTrackRepository.countByBlindtestId(blindtest.getId());
        if (trackCount == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce blindtest n'a aucune musique");
        }
        int answerSeconds = request.answerSeconds() == null ? DEFAULT_ANSWER_SECONDS : request.answerSeconds();
        if (answerSeconds < MIN_ANSWER_SECONDS || answerSeconds > MAX_ANSWER_SECONDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Le temps de réponse doit être compris entre " + MIN_ANSWER_SECONDS + " et " + MAX_ANSWER_SECONDS + " secondes");
        }
        Room room = roomService.create(
            blindtest.getId(),
            blindtest.getName(),
            trackCount,
            host,
            request.visibility() == null ? RoomVisibility.PUBLIC : request.visibility(),
            request.soundMode() == null ? SoundMode.EVERY_DEVICE : request.soundMode(),
            request.scoringMode() == null ? ScoringMode.SPEED : request.scoringMode(),
            answerSeconds,
            blindtest.getMaxAttempts()
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La partie est terminée");
        }
        if (room.join(player(listenerId))) {
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
        List<RoomTrack> tracks = blindtestTrackRepository.findWithTrackByBlindtestId(room.getBlindtestId()).stream()
            .map(blindtestTrack -> RoomTrack.from(blindtestTrack.getTrack()))
            .toList();
        if (tracks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce blindtest n'a aucune musique");
        }
        try {
            roomTimer.start(room, tracks);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return RoomResponse.from(room);
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

    private Room room(String code) {
        return roomService.find(code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salle introuvable"));
    }

    private Room room(String code, Integer listenerId) {
        Room room = room(code);
        if (!room.hasPlayer(listenerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Il faut rejoindre la salle pour répondre");
        }
        return room;
    }

    private RoomRound answered(Room room, Integer listenerId, RoomRound round) {
        if (round.done()) {
            recordKnowledge(listenerId, round.trackId(), round.gameFound());
            roomEvents.publish(room);
            if (room.everyoneDone()) {
                roomTimer.reveal(room, round.position());
            }
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
