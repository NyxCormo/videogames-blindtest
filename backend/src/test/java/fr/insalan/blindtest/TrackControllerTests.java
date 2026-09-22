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

	@Test
	void returnsTheLinksOfEachTrack() throws Exception {
		Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
		Game game = games.save(new Game("Stellar Blade", stellar));
		Track dawn = new Track("Dawn", game);
		dawn.setKhinsiderLink("https://downloads.khinsider.com/game-soundtracks/album/stellar-blade-soundtrack-2024/62.%2520Dawn.mp3");
		tracks.save(dawn);
		tracks.save(new Track("Raven", game));

		mockMvc.perform(get("/api/tracks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Dawn"))
				.andExpect(jsonPath("$[0].khinsiderLink").value(dawn.getKhinsiderLink()))
				.andExpect(jsonPath("$[0].youtubeLink").doesNotExist())
				.andExpect(jsonPath("$[1].name").value("Raven"))
				.andExpect(jsonPath("$[1].khinsiderLink").doesNotExist());
	}

}
