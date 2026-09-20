package fr.insalan.blindtest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TrackRepository;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrackControllerTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	FranchiseRepository franchises;

	@Autowired
	GameRepository games;

	@Autowired
	TrackRepository tracks;

	@AfterEach
	void cleanDatabase() {
		tracks.deleteAll();
		games.deleteAll();
		franchises.deleteAll();
	}

	@Test
	void listIsEmptyWithoutData() throws Exception {
		mockMvc.perform(get("/api/tracks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void listTracksSortedWithGameAndFranchise() throws Exception {
		Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
		Game stellar2 = games.save(new Game("Stellar Blade: Blood Rain", stellar));
		tracks.save(new Track("Trailer", stellar2));
		tracks.save(new Track("Main Theme", stellar2));

		mockMvc.perform(get("/api/tracks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Main Theme"))
				.andExpect(jsonPath("$[0].gameName").value("Stellar Blade: Blood Rain"))
				.andExpect(jsonPath("$[0].franchiseName").value("Stellar Blade"))
				.andExpect(jsonPath("$[1].name").value("Trailer"));
	}
}
