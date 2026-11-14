package fr.insalan.blindtest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.BlindtestTrack;
import fr.insalan.blindtest.model.KnowledgeId;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.room.Room;
import fr.insalan.blindtest.room.RoomService;
import fr.insalan.blindtest.room.RoomTrack;

class RoomControllerTests extends IntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    BlindtestRepository blindtests;

    @Autowired
    BlindtestTrackRepository blindtestTracks;

    @Autowired
    TrackRepository tracks;

    @Autowired
    ListenerRepository listeners;

    @Autowired
    KnowledgeRepository knowledges;

    @Autowired
    RoomService roomService;

    private Blindtest blindtest;
    private Listener host;
    private Track dawn;

    @BeforeEach
    void createBlindtest() {
        dawn = tracks.save(new Track("Dawn", stellarBlade()));
        blindtest = blindtests.save(new Blindtest("Stellar Blade", 50, 5));
        blindtestTracks.save(new BlindtestTrack(blindtest, dawn, 0));
        host = listeners.save(new Listener("Nyx"));
    }

    @Test
    void createsARoomWithTheHostAsFirstPlayer() throws Exception {
        mockMvc.perform(post("/api/rooms").param("listenerId", host.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"blindtestId": %d, "visibility": "PRIVATE", "mode": "MULTI", "playersCanSeek": false, "scoringMode": "FIRST_COME", "answerSeconds": 20}
                    """.formatted(blindtest.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(matchesPattern("[A-HJ-NP-Z]{4}")))
            .andExpect(jsonPath("$.blindtestName").value("Stellar Blade"))
            .andExpect(jsonPath("$.trackCount").value(1))
            .andExpect(jsonPath("$.hostId").value(host.getId()))
            .andExpect(jsonPath("$.hostName").value("Nyx"))
            .andExpect(jsonPath("$.visibility").value("PRIVATE"))
            .andExpect(jsonPath("$.mode").value("MULTI"))
            .andExpect(jsonPath("$.playersCanSeek").value(false))
            .andExpect(jsonPath("$.scoringMode").value("FIRST_COME"))
            .andExpect(jsonPath("$.answerSeconds").value(20))
            .andExpect(jsonPath("$.players[0].name").value("Nyx"));
    }

    @Test
    void usesDefaultSettingsWhenTheyAreMissing() throws Exception {
        mockMvc.perform(post("/api/rooms").param("listenerId", host.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blindtestId\": " + blindtest.getId() + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.visibility").value("PUBLIC"))
            .andExpect(jsonPath("$.mode").value("MULTI"))
            .andExpect(jsonPath("$.playersCanSeek").value(true))
            .andExpect(jsonPath("$.scoringMode").value("SPEED"))
            .andExpect(jsonPath("$.answerSeconds").value(30));
    }

    @Test
    void playersJoinWithTheCodeOnlyOnce() throws Exception {
        String code = createRoom("PRIVATE");
        Listener raven = listeners.save(new Listener("Raven"));

        mockMvc.perform(post("/api/rooms/" + code.toLowerCase() + "/players").param("listenerId", raven.getId().toString()))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/rooms/" + code + "/players").param("listenerId", raven.getId().toString()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/rooms/" + code))
            .andExpect(jsonPath("$.players.length()").value(2))
            .andExpect(jsonPath("$.players[0].name").value("Nyx"))
            .andExpect(jsonPath("$.players[1].name").value("Raven"));
    }

    @Test
    void theEventStreamSendsTheRoomAndEachNewPlayer() throws Exception {
        String code = createRoom("PRIVATE");
        Listener raven = listeners.save(new Listener("Raven"));

        MvcResult events = mockMvc.perform(get("/api/rooms/" + code + "/events").accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(request().asyncStarted())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
            .andReturn();
        String firstEvent = events.getResponse().getContentAsString();
        assertTrue(firstEvent.startsWith("event:room\ndata:{"));
        assertTrue(firstEvent.contains("\"Nyx\""));
        assertFalse(firstEvent.contains("\"Raven\""));

        mockMvc.perform(post("/api/rooms/" + code + "/players").param("listenerId", raven.getId().toString()));
        mockMvc.perform(post("/api/rooms/" + code + "/players").param("listenerId", raven.getId().toString()));

        String allEvents = events.getResponse().getContentAsString();
        assertEquals(2, allEvents.split("event:room").length - 1);
        assertTrue(allEvents.substring(firstEvent.length()).contains("\"Raven\""));
    }

    @Test
    void theEventStreamOfAnUnknownRoomIsNotFound() throws Exception {
        mockMvc.perform(get("/api/rooms/ZZZZ/events").accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isNotFound());
    }

    @Test
    void givesTheServerTime() throws Exception {
        long before = System.currentTimeMillis();
        String body = mockMvc.perform(get("/api/time"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        long now = objectMapper.readTree(body).get("now").asLong();
        assertTrue(now >= before && now <= System.currentTimeMillis());
    }

    @Test
    void onlyTheHostStartsTheGameWithoutGivingTheAnswer() throws Exception {
        String code = createRoom("PRIVATE");
        Listener raven = listeners.save(new Listener("Raven"));
        mockMvc.perform(post("/api/rooms/" + code + "/players").param("listenerId", raven.getId().toString()));

        mockMvc.perform(post("/api/rooms/" + code + "/start").param("listenerId", raven.getId().toString()))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/rooms/" + code + "/start").param("listenerId", host.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phase").value("PLAYING"))
            .andExpect(jsonPath("$.currentTrack.position").value(0))
            .andExpect(jsonPath("$.currentTrack.trackId").value(dawn.getId()))
            .andExpect(jsonPath("$.currentTrack.startsAt").exists())
            .andExpect(jsonPath("$.currentTrack.reveal").value(nullValue()))
            .andExpect(content().string(not(containsString("gameName"))));
        mockMvc.perform(post("/api/rooms/" + code + "/start").param("listenerId", host.getId().toString()))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/rooms/ZZZZ/start").param("listenerId", host.getId().toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    void theEventStreamAnnouncesTheStart() throws Exception {
        String code = createRoom("PRIVATE");
        MvcResult events = mockMvc.perform(get("/api/rooms/" + code + "/events").accept(MediaType.TEXT_EVENT_STREAM))
            .andReturn();

        mockMvc.perform(post("/api/rooms/" + code + "/start").param("listenerId", host.getId().toString()));

        String allEvents = events.getResponse().getContentAsString();
        assertTrue(allEvents.contains("\"phase\":\"WAITING\""));
        assertTrue(allEvents.contains("\"phase\":\"PLAYING\""));
    }

    @Test
    void playersAnswerOnceAndTheTrackIsRevealedWhenEveryoneIsDone() throws Exception {
        String code = createRoom("PRIVATE");
        Listener raven = join(code, "Raven");
        Room room = musicAlreadyPlaying(code);

        mockMvc.perform(post("/api/rooms/" + code + "/guess-franchise").param("listenerId", raven.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"franchiseId\": " + dawn.getGame().getFranchise().getId() + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.foundFranchiseId").value(dawn.getGame().getFranchise().getId()))
            .andExpect(jsonPath("$.roundPoints").value(250))
            .andExpect(jsonPath("$.done").value(false));
        mockMvc.perform(post("/api/rooms/" + code + "/guess").param("listenerId", host.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameId\": " + dawn.getGame().getId() + ", \"trackId\": " + dawn.getId() + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameFound").value(true))
            .andExpect(jsonPath("$.bonusFound").value(true))
            .andExpect(jsonPath("$.done").value(true));
        mockMvc.perform(post("/api/rooms/" + code + "/pass").param("listenerId", host.getId().toString()))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/rooms/" + code))
            .andExpect(jsonPath("$.phase").value("PLAYING"))
            .andExpect(jsonPath("$.players[0].done").value(true))
            .andExpect(jsonPath("$.players[0].roundPoints").value(nullValue()));

        mockMvc.perform(post("/api/rooms/" + code + "/pass").param("listenerId", raven.getId().toString()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/rooms/" + code))
            .andExpect(jsonPath("$.phase").value("REVEAL"))
            .andExpect(jsonPath("$.currentTrack.reveal.gameName").value("Stellar Blade"))
            .andExpect(jsonPath("$.players[0].score").value(room.round(host.getId()).score()))
            .andExpect(jsonPath("$.players[1].score").value(250))
            .andExpect(jsonPath("$.players[1].roundPoints").value(250));
        assertTrue(knowledges.findById(new KnowledgeId(host.getId(), dawn.getId())).orElseThrow().isKnows());
        assertFalse(knowledges.findById(new KnowledgeId(raven.getId(), dawn.getId())).orElseThrow().isKnows());
    }

    @Test
    void refusesAnswersOutsideTheRules() throws Exception {
        String code = createRoom("PRIVATE");
        Listener stranger = listeners.save(new Listener("Raven"));

        mockMvc.perform(post("/api/rooms/" + code + "/pass").param("listenerId", host.getId().toString()))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/rooms/" + code + "/start").param("listenerId", host.getId().toString()));
        mockMvc.perform(post("/api/rooms/" + code + "/pass").param("listenerId", host.getId().toString()))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/rooms/" + code + "/pass").param("listenerId", stranger.getId().toString()))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/rooms/" + code + "/guess").param("listenerId", host.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/rooms/" + code + "/round").param("listenerId", host.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.attemptsRemaining").value(5));
        assertTrue(knowledges.findById(new KnowledgeId(host.getId(), dawn.getId())).isEmpty());
    }

    @Test
    void onlyTheHostStopsTheGame() throws Exception {
        String code = createRoom("PUBLIC");
        Listener raven = join(code, "Raven");
        musicAlreadyPlaying(code);

        mockMvc.perform(post("/api/rooms/" + code + "/stop").param("listenerId", raven.getId().toString()))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/rooms/" + code + "/stop").param("listenerId", host.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phase").value("FINISHED"))
            .andExpect(jsonPath("$.currentTrack").value(nullValue()));
        mockMvc.perform(post("/api/rooms/" + code + "/stop").param("listenerId", host.getId().toString()))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/rooms/" + code + "/pass").param("listenerId", raven.getId().toString()))
            .andExpect(status().isConflict());
        mockMvc.perform(get("/api/rooms"))
            .andExpect(jsonPath("$[*].code").value(not(hasItem(code))));
    }

    @Test
    void inPresentationTheHostRevealsAndMovesOnByHand() throws Exception {
        String code = createRoom("PUBLIC", "PRESENTATION");
        mockMvc.perform(post("/api/rooms/" + code + "/players").param("listenerId", host.getId().toString()))
            .andExpect(jsonPath("$.players.length()").value(0));
        Listener raven = join(code, "Raven");
        musicAlreadyPlaying(code);

        mockMvc.perform(post("/api/rooms/" + code + "/pass").param("listenerId", host.getId().toString()))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/rooms/" + code + "/pass").param("listenerId", raven.getId().toString()))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/rooms/" + code))
            .andExpect(jsonPath("$.phase").value("PLAYING"));

        mockMvc.perform(post("/api/rooms/" + code + "/reveal").param("listenerId", raven.getId().toString()))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/rooms/" + code + "/next").param("listenerId", host.getId().toString()))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/rooms/" + code + "/reveal").param("listenerId", host.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phase").value("REVEAL"))
            .andExpect(jsonPath("$.currentTrack.reveal.trackName").value("Dawn"));
        mockMvc.perform(post("/api/rooms/" + code + "/reveal").param("listenerId", host.getId().toString()))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/rooms/" + code + "/next").param("listenerId", host.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phase").value("FINISHED"));
    }

    @Test
    void aMultiRoomIsNotDrivenByHand() throws Exception {
        String code = createRoom("PUBLIC");
        musicAlreadyPlaying(code);

        mockMvc.perform(post("/api/rooms/" + code + "/reveal").param("listenerId", host.getId().toString()))
            .andExpect(status().isConflict());
        mockMvc.perform(get("/api/rooms/" + code))
            .andExpect(jsonPath("$.phase").value("PLAYING"));
    }

    @Test
    void theTrackIsRevealedWhenTheLastPlayerStillAnsweringLeaves() throws Exception {
        String code = createRoom("PRIVATE");
        Listener raven = join(code, "Raven");
        musicAlreadyPlaying(code);

        mockMvc.perform(post("/api/rooms/" + code + "/pass").param("listenerId", host.getId().toString()))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/rooms/" + code))
            .andExpect(jsonPath("$.phase").value("PLAYING"))
            .andExpect(jsonPath("$.players[1].present").value(true));
        mockMvc.perform(post("/api/rooms/" + code + "/leave").param("listenerId", raven.getId().toString()))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/rooms/" + code))
            .andExpect(jsonPath("$.phase").value("REVEAL"))
            .andExpect(jsonPath("$.players[1].present").value(false))
            .andExpect(jsonPath("$.players[1].tracksHeard").value(0))
            .andExpect(jsonPath("$.players[0].tracksHeard").value(1));

        mockMvc.perform(post("/api/rooms/" + code + "/presence").param("listenerId", raven.getId().toString()))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/rooms/" + code))
            .andExpect(jsonPath("$.players[1].present").value(true));
        mockMvc.perform(post("/api/rooms/" + code + "/presence").param("listenerId", String.valueOf(raven.getId() + 1000)))
            .andExpect(status().isForbidden());
    }

    @Test
    void theFinishedRoomListsThePlayedTracksAndTheTotals() throws Exception {
        String code = createRoom("PRIVATE");
        musicAlreadyPlaying(code);

        mockMvc.perform(get("/api/rooms/" + code))
            .andExpect(jsonPath("$.playedTracks.length()").value(0));
        mockMvc.perform(post("/api/rooms/" + code + "/guess").param("listenerId", host.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameId\": " + dawn.getGame().getId() + ", \"trackId\": " + dawn.getId() + "}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/rooms/" + code))
            .andExpect(jsonPath("$.phase").value("REVEAL"))
            .andExpect(jsonPath("$.playedTracks.length()").value(0));
        mockMvc.perform(post("/api/rooms/" + code + "/stop").param("listenerId", host.getId().toString()))
            .andExpect(jsonPath("$.players[0].gamesFound").value(1))
            .andExpect(jsonPath("$.players[0].franchisesFound").value(0))
            .andExpect(jsonPath("$.players[0].bonusesFound").value(1))
            .andExpect(jsonPath("$.playedTracks.length()").value(1))
            .andExpect(jsonPath("$.playedTracks[0].trackName").value("Dawn"))
            .andExpect(jsonPath("$.playedTracks[0].gameName").value("Stellar Blade"));
    }

    @Test
    void finishedRoomsAreNotListedAndOnlyShowTheirResults() throws Exception {
        String code = createRoom("PUBLIC");
        Room room = musicAlreadyPlaying(code);
        room.reveal(0);
        room.next(0, Instant.now());
        Listener raven = listeners.save(new Listener("Raven"));

        mockMvc.perform(get("/api/rooms"))
            .andExpect(jsonPath("$[*].code").value(not(hasItem(code))));
        mockMvc.perform(post("/api/rooms/" + code + "/players").param("listenerId", raven.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phase").value("FINISHED"))
            .andExpect(jsonPath("$.players.length()").value(1));
    }

    @Test
    void onlyPublicRoomsAreListed() throws Exception {
        String publicCode = createRoom("PUBLIC");
        String privateCode = createRoom("PRIVATE");

        mockMvc.perform(get("/api/rooms"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].code").value(hasItem(publicCode)))
            .andExpect(jsonPath("$[*].code").value(not(hasItem(privateCode))));
    }

    @Test
    void refusesInvalidRooms() throws Exception {
        Blindtest empty = blindtests.save(new Blindtest("Vide", 50, 5));

        mockMvc.perform(post("/api/rooms").param("listenerId", host.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blindtestId\": " + empty.getId() + "}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/rooms").param("listenerId", host.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blindtestId\": " + blindtest.getId() + ", \"answerSeconds\": 5}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/rooms").param("listenerId", host.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blindtestId\": " + blindtest.getId() + ", \"scoringMode\": \"LOTERIE\"}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/rooms").param("listenerId", String.valueOf(host.getId() + 1000))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blindtestId\": " + blindtest.getId() + "}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/rooms/ZZZZZ"))
            .andExpect(status().isNotFound());
    }

    private Listener join(String code, String name) throws Exception {
        Listener listener = listeners.save(new Listener(name));
        mockMvc.perform(post("/api/rooms/" + code + "/players").param("listenerId", listener.getId().toString()))
            .andExpect(status().isOk());
        return listener;
    }

    private Room musicAlreadyPlaying(String code) {
        Room room = roomService.find(code).orElseThrow();
        room.start(List.of(RoomTrack.from(dawn)), Instant.now().minus(Room.LEAD_TIME));
        return room;
    }

    private String createRoom(String visibility) throws Exception {
        return createRoom(visibility, "MULTI");
    }

    private String createRoom(String visibility, String mode) throws Exception {
        String body = mockMvc.perform(post("/api/rooms").param("listenerId", host.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blindtestId\": " + blindtest.getId() + ", \"visibility\": \"" + visibility + "\", \"mode\": \"" + mode + "\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode room = objectMapper.readTree(body);
        return room.get("code").asString();
    }
}
