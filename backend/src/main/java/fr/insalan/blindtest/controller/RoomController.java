package fr.insalan.blindtest.controller;

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

import fr.insalan.blindtest.dto.room.CreateRoomRequest;
import fr.insalan.blindtest.dto.room.RoomResponse;
import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.room.Room;
import fr.insalan.blindtest.room.RoomEvents;
import fr.insalan.blindtest.room.RoomPlayer;
import fr.insalan.blindtest.room.RoomService;
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
    private final BlindtestRepository blindtestRepository;
    private final BlindtestTrackRepository blindtestTrackRepository;
    private final ListenerRepository listenerRepository;

    public RoomController(
        RoomService roomService,
        RoomEvents roomEvents,
        BlindtestRepository blindtestRepository,
        BlindtestTrackRepository blindtestTrackRepository,
        ListenerRepository listenerRepository
    ) {
        this.roomService = roomService;
        this.roomEvents = roomEvents;
        this.blindtestRepository = blindtestRepository;
        this.blindtestTrackRepository = blindtestTrackRepository;
        this.listenerRepository = listenerRepository;
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
            answerSeconds
        );
        return RoomResponse.from(room);
    }

    @GetMapping("/{code}")
    public RoomResponse get(@PathVariable String code) {
        return RoomResponse.from(room(code));
    }

    @PostMapping("/{code}/players")
    public RoomResponse join(@PathVariable String code, @RequestParam Integer listenerId) {
        Room room = room(code);
        if (room.join(player(listenerId))) {
            roomEvents.publish(room);
        }
        return RoomResponse.from(room);
    }

    @GetMapping(path = "/{code}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String code) {
        return roomEvents.subscribe(room(code));
    }

    private Room room(String code) {
        return roomService.find(code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salle introuvable"));
    }

    private RoomPlayer player(Integer listenerId) {
        Listener listener = listenerRepository.findById(listenerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pseudo introuvable"));
        return new RoomPlayer(listener.getId(), listener.getName());
    }
}
