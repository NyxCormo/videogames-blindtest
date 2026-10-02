package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import fr.insalan.blindtest.audiolink.AudioLinkRefreshJob;
import fr.insalan.blindtest.security.TokenStore;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAudioLinkTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TokenStore tokenStore;

    @Autowired
    AudioLinkRefreshJob audioLinkRefreshJob;

    @Test
    void requiresAnAdminToken() throws Exception {
        mockMvc.perform(post("/api/admin/audio-links/refresh"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void startsARefreshInTheBackgroundAndReportsItWhenDone() throws Exception {
        String token = "Bearer " + tokenStore.create("admin");

        mockMvc.perform(post("/api/admin/audio-links/refresh").header("Authorization", token))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.startedAt").isNotEmpty());

        // Base de test vide : le balayage se termine presque tout de suite.
        for (int i = 0; i < 50 && audioLinkRefreshJob.status().running(); i++) {
            Thread.sleep(100);
        }
        assertThat(audioLinkRefreshJob.status().running()).isFalse();

        mockMvc.perform(get("/api/admin/audio-links/refresh").header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.running").value(false))
            .andExpect(jsonPath("$.finishedAt").isNotEmpty())
            .andExpect(jsonPath("$.lastReport.checked").value(0));
    }
}
