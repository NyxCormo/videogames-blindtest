package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.ObjectMapper;

import fr.insalan.blindtest.dto.CreateTrackRequest;
import fr.insalan.blindtest.dto.SetLinkRequest;
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
	ObjectMapper objectMapper;

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
		dawn.setAudioLink("https://jetta.vgmtreasurechest.com/soundtracks/stellar-blade-soundtrack-2024/ybomulwy/62.%20Dawn.mp3");
		tracks.save(dawn);
		tracks.save(new Track("Raven", game));

		mockMvc.perform(get("/api/tracks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Dawn"))
				.andExpect(jsonPath("$[0].khinsiderLink").value(dawn.getKhinsiderLink()))
				.andExpect(jsonPath("$[0].youtubeLink").doesNotExist())
				.andExpect(jsonPath("$[0].audioLink").value(dawn.getAudioLink()))
				.andExpect(jsonPath("$[1].name").value("Raven"))
				.andExpect(jsonPath("$[1].khinsiderLink").doesNotExist())
				.andExpect(jsonPath("$[1].audioLink").doesNotExist());
 	}

	@Test
	void getReturnsOneTrack() throws Exception {
		Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
		Game game = games.save(new Game("Stellar Blade", stellar));
		Track dawn = tracks.save(new Track("Dawn", game));

		mockMvc.perform(get("/api/tracks/" + dawn.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Dawn"))
				.andExpect(jsonPath("$.gameName").value("Stellar Blade"));
	}

	@Test
	void getReturnsNotFoundForAnUnknownTrack() throws Exception {
		mockMvc.perform(get("/api/tracks/999999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void createsANewTrackInAGame() throws Exception {
		Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
		Game game = games.save(new Game("Stellar Blade", stellar));
		CreateTrackRequest request = new CreateTrackRequest("Nouvelle musique", game.getId());

		mockMvc.perform(post("/api/tracks")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Nouvelle musique"))
				.andExpect(jsonPath("$.gameId").value(game.getId()))
				.andExpect(jsonPath("$.franchiseName").value("Stellar Blade"));

		assertThat(tracks.findByGameAndName(game, "Nouvelle musique")).isPresent();
	}

	@Test
	void rejectsATrackThatAlreadyExistsInTheSameGame() throws Exception {
		Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
		Game game = games.save(new Game("Stellar Blade", stellar));
		tracks.save(new Track("Dawn", game));
		CreateTrackRequest request = new CreateTrackRequest("Dawn", game.getId());

		mockMvc.perform(post("/api/tracks")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict());

		assertThat(tracks.count()).isEqualTo(1);
	}

	@Test
	void rejectsAnUnknownGame() throws Exception {
		CreateTrackRequest request = new CreateTrackRequest("Nouvelle musique", 999999);

		mockMvc.perform(post("/api/tracks")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void setsYoutubeLink() throws Exception {
		Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
		Game game = games.save(new Game("Stellar Blade", stellar));
		Track dawn = tracks.save(new Track("Dawn", game));
		SetLinkRequest request = new SetLinkRequest("https://www.youtube.com/watch?v=exemple");

		mockMvc.perform(post("/api/tracks/" + dawn.getId() + "/youtube-link")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.youtubeLink").value(request.link()));

		assertThat(tracks.findById(dawn.getId()).orElseThrow().getYoutubeLink()).isEqualTo(request.link());
	}

	@Test
	void rejectsOverwritingAnExistingYoutubeLink() throws Exception {
		Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
		Game game = games.save(new Game("Stellar Blade", stellar));
		Track dawn = new Track("Dawn", game);
		dawn.setYoutubeLink("https://www.youtube.com/watch?v=ancien");
		tracks.save(dawn);
		SetLinkRequest request = new SetLinkRequest("https://www.youtube.com/watch?v=nouveau");

		mockMvc.perform(post("/api/tracks/" + dawn.getId() + "/youtube-link")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict());

		assertThat(tracks.findById(dawn.getId()).orElseThrow().getYoutubeLink()).isEqualTo("https://www.youtube.com/watch?v=ancien");
	}

	@Test
	void rejectsABlankLink() throws Exception {
		Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
		Game game = games.save(new Game("Stellar Blade", stellar));
		Track dawn = tracks.save(new Track("Dawn", game));
		SetLinkRequest request = new SetLinkRequest("  ");

		mockMvc.perform(post("/api/tracks/" + dawn.getId() + "/youtube-link")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsALinkThatIsNotHttps() throws Exception {
		Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
		Game game = games.save(new Game("Stellar Blade", stellar));
		Track dawn = tracks.save(new Track("Dawn", game));
		SetLinkRequest request = new SetLinkRequest("javascript:alert(1)");

		mockMvc.perform(post("/api/tracks/" + dawn.getId() + "/youtube-link")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());

		assertThat(tracks.findById(dawn.getId()).orElseThrow().getYoutubeLink()).isNull();
	}

	@Test
	void rejectsAYoutubeLinkFromAnotherDomain() throws Exception {
		Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
		Game game = games.save(new Game("Stellar Blade", stellar));
		Track dawn = tracks.save(new Track("Dawn", game));
		SetLinkRequest request = new SetLinkRequest("https://downloads.khinsider.com/pas-youtube");

		mockMvc.perform(post("/api/tracks/" + dawn.getId() + "/youtube-link")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());

		assertThat(tracks.findById(dawn.getId()).orElseThrow().getYoutubeLink()).isNull();
	}

	@Test
	void acceptsAShortYoutubeLink() throws Exception {
		Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
		Game game = games.save(new Game("Stellar Blade", stellar));
		Track dawn = tracks.save(new Track("Dawn", game));
		SetLinkRequest request = new SetLinkRequest("https://youtu.be/exemple");

		mockMvc.perform(post("/api/tracks/" + dawn.getId() + "/youtube-link")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());
	}

	@Test
	void rejectsAKhinsiderLinkFromAnotherDomain() throws Exception {
		Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
		Game game = games.save(new Game("Stellar Blade", stellar));
		Track dawn = tracks.save(new Track("Dawn", game));
		SetLinkRequest request = new SetLinkRequest("https://www.youtube.com/watch?v=pas-khinsider");

		mockMvc.perform(post("/api/tracks/" + dawn.getId() + "/khinsider-link")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());

		assertThat(tracks.findById(dawn.getId()).orElseThrow().getKhinsiderLink()).isNull();
	}

	@Test
	void setsKhinsiderLinkAndResolvesTheAudioLinkImmediately() throws Exception {
		// "khinsider.com" dans le chemin, pour passer la vérification de domaine sur ce serveur local de test.
		HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/audio", exchange -> {
			byte[] bytes = "peu importe".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		server.createContext("/khinsider.com/khinsider", exchange -> {
			String audioUrl = "http://localhost:" + exchange.getLocalAddress().getPort() + "/audio";
			byte[] bytes = ("<html><body><audio id=\"audio\" src=\"" + audioUrl + "\"></audio></body></html>")
				.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		server.start();
		try {
			Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
			Game game = games.save(new Game("Stellar Blade", stellar));
			Track dawn = tracks.save(new Track("Dawn", game));
			String khinsiderUrl = "http://localhost:" + server.getAddress().getPort() + "/khinsider.com/khinsider";
			SetLinkRequest request = new SetLinkRequest(khinsiderUrl);

			mockMvc.perform(post("/api/tracks/" + dawn.getId() + "/khinsider-link")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.khinsiderLink").value(khinsiderUrl))
					.andExpect(jsonPath("$.audioLink").value("http://localhost:" + server.getAddress().getPort() + "/audio"));

			Track updated = tracks.findById(dawn.getId()).orElseThrow();
			assertThat(updated.getKhinsiderLink()).isEqualTo(khinsiderUrl);
			assertThat(updated.getAudioLink()).isNotNull();
		} finally {
			server.stop(0);
		}
	}

	@Test
	void rejectsOverwritingAnExistingKhinsiderLink() throws Exception {
		Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
		Game game = games.save(new Game("Stellar Blade", stellar));
		Track dawn = new Track("Dawn", game);
		dawn.setKhinsiderLink("https://downloads.khinsider.com/ancien");
		tracks.save(dawn);
		SetLinkRequest request = new SetLinkRequest("https://downloads.khinsider.com/nouveau");

		mockMvc.perform(post("/api/tracks/" + dawn.getId() + "/khinsider-link")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict());

		assertThat(tracks.findById(dawn.getId()).orElseThrow().getKhinsiderLink()).isEqualTo("https://downloads.khinsider.com/ancien");
	}

	@Test
	void linkEndpointsReturnNotFoundForAnUnknownTrack() throws Exception {
		SetLinkRequest request = new SetLinkRequest("https://downloads.khinsider.com/quelque-chose");

		mockMvc.perform(post("/api/tracks/999999/khinsider-link")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}

}
