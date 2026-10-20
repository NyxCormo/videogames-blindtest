package fr.insalan.blindtest.controller.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.cover.AlbumImages;
import fr.insalan.blindtest.cover.CoverFillJob;
import fr.insalan.blindtest.cover.CoverFillMode;
import fr.insalan.blindtest.cover.CoverFillStatus;
import fr.insalan.blindtest.cover.CoverService;
import fr.insalan.blindtest.cover.CoverStore;
import fr.insalan.blindtest.cover.ImageUrls;
import fr.insalan.blindtest.dto.admin.AdminCoverResponse;
import fr.insalan.blindtest.dto.admin.CoverProposalResponse;
import fr.insalan.blindtest.dto.admin.CoverRequest;
import fr.insalan.blindtest.dto.admin.GameCoverEntryResponse;
import fr.insalan.blindtest.model.CoverProposal;
import fr.insalan.blindtest.model.CoverProposalId;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.repository.CoverProposalRepository;
import fr.insalan.blindtest.repository.GameRepository;

@RestController
@RequestMapping("/api/admin")
public class AdminCoverController {

    private final GameRepository gameRepository;
    private final CoverProposalRepository coverProposalRepository;
    private final CoverService coverService;
    private final CoverStore coverStore;
    private final CoverFillJob coverFillJob;

    public AdminCoverController(
        GameRepository gameRepository,
        CoverProposalRepository coverProposalRepository,
        CoverService coverService,
        CoverStore coverStore,
        CoverFillJob coverFillJob
    ) {
        this.gameRepository = gameRepository;
        this.coverProposalRepository = coverProposalRepository;
        this.coverService = coverService;
        this.coverStore = coverStore;
        this.coverFillJob = coverFillJob;
    }

    @GetMapping("/covers")
    public List<GameCoverEntryResponse> covers() {
        return gameRepository.findByCoverUrlIsNotNull().stream()
            .map(GameCoverEntryResponse::from)
            .toList();
    }

    @GetMapping("/games/{id}/cover")
    public AdminCoverResponse cover(@PathVariable Integer id) {
        return response(game(id));
    }

    @GetMapping("/games/{id}/cover/candidates")
    public List<AlbumImages> candidates(@PathVariable Integer id, @RequestParam(defaultValue = "false") boolean rescan) {
        return coverService.candidates(game(id).getId(), rescan);
    }

    @PutMapping("/games/{id}/cover")
    public AdminCoverResponse save(@PathVariable Integer id, @RequestBody CoverRequest request) {
        Game game = game(id);
        coverService.setCover(game, ImageUrls.require(request.coverUrl()));
        return response(game);
    }

    @DeleteMapping("/games/{id}/cover")
    public AdminCoverResponse remove(@PathVariable Integer id) {
        Game game = game(id);
        coverService.setCover(game, null);
        return response(game);
    }

    @GetMapping("/cover-proposals")
    public List<CoverProposalResponse> proposals() {
        return coverProposalRepository.findAllWithGameAndListener().stream()
            .map(CoverProposalResponse::from)
            .toList();
    }

    @PostMapping("/cover-proposals/{gameId}/{listenerId}/accept")
    public AdminCoverResponse accept(@PathVariable Integer gameId, @PathVariable Integer listenerId) {
        CoverProposal proposal = proposal(gameId, listenerId);
        if (proposal.getImageUrl() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette proposition n'a pas d'image : en choisir une soi-même");
        }
        Game game = game(gameId);
        coverService.setCover(game, proposal.getImageUrl());
        coverProposalRepository.deleteByGameIdAndImageUrl(gameId, proposal.getImageUrl());
        return response(game);
    }

    @DeleteMapping("/cover-proposals/{gameId}/{listenerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Integer gameId, @PathVariable Integer listenerId) {
        coverProposalRepository.delete(proposal(gameId, listenerId));
    }

    @PostMapping("/covers/fill")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CoverFillStatus startFill(@RequestParam(defaultValue = "NEW") CoverFillMode mode) {
        if (!coverFillJob.startInBackground(mode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une recherche d'images est déjà en cours");
        }
        return coverFillJob.status();
    }

    @GetMapping("/covers/fill")
    public CoverFillStatus fillStatus() {
        return coverFillJob.status();
    }

    private Game game(Integer id) {
        return gameRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jeu introuvable"));
    }

    private CoverProposal proposal(Integer gameId, Integer listenerId) {
        return coverProposalRepository.findById(new CoverProposalId(gameId, listenerId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposition introuvable"));
    }

    private AdminCoverResponse response(Game game) {
        return AdminCoverResponse.from(game, coverStore.find(game.getId()).isPresent());
    }
}
