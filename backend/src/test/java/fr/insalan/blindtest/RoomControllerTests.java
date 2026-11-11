package fr.insalan.blindtest;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;

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

    private Blindtest blindtest;
    private Listener host;

    @BeforeEach
    void createBlindtest() {
        Track dawn = tracks.save(new Track("Dawn", stellarBlade()));
        blindtest = blindtests.save(new Blindtest("Stellar Blade", 50, 5));
        blindtestTracks.save(new BlindtestTrack(blindtest, dawn, 0));
        host = listeners.save(new Listener("Nyx"));
    }

    @Test
    void createsARoomWithTheHostAsFirstPlayer() throws Exception {
        mockMvc.perform(post("/api/rooms").param("listenerId", host.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"blindtestId": %d, "visibility": "PRIVATE", "soundMode": "MAIN_SCREEN", "scoringMode": "FIRST_COME", "answerSeconds": 20}
                    """.formatted(blindtest.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(matchesPattern("[A-HJ-NP-Z]{4}")))
            .andExpect(jsonPath("$.blindtestName").value("Stellar Blade"))
            .andExpect(jsonPath("$.trackCount").value(1))
            .andExpect(jsonPath("$.hostId").value(host.getId()))
            .andExpect(jsonPath("$.visibility").value("PRIVATE"))
            .andExpect(jsonPath("$.soundMode").value("MAIN_SCREEN"))
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
            .andExpect(jsonPath("$.soundMode").value("EVERY_DEVICE"))
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

    private String createRoom(String visibility) throws Exception {
        String body = mockMvc.perform(post("/api/rooms").param("listenerId", host.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blindtestId\": " + blindtest.getId() + ", \"visibility\": \"" + visibility + "\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode room = objectMapper.readTree(body);
        return room.get("code").asString();
    }
}
