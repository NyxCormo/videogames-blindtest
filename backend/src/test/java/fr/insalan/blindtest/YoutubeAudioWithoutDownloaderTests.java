package fr.insalan.blindtest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.TrackRepository;

class YoutubeAudioWithoutDownloaderTests extends IntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TrackRepository tracks;

    @Test
    void withoutDownloaderTheAdminSeesItAndNothingIsDownloaded() throws Exception {
        Track dawn = new Track("Dawn", stellarBlade());
        dawn.setYoutubeLink("https://www.youtube.com/watch?v=dawn");
        tracks.save(dawn);

        mockMvc.perform(get("/api/admin/youtube-audio").header("Authorization", adminToken()))
            .andExpect(jsonPath("$.available").value(false))
            .andExpect(jsonPath("$.tracks").value(1))
            .andExpect(jsonPath("$.downloaded").value(0));
        mockMvc.perform(post("/api/admin/youtube-audio").header("Authorization", adminToken()))
            .andExpect(status().isConflict());
        mockMvc.perform(get("/api/tracks/" + dawn.getId() + "/audio"))
            .andExpect(status().isNotFound());
    }
}
