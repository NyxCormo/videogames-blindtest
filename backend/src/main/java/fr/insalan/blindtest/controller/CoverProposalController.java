package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.cover.ImageUrls;
import fr.insalan.blindtest.dto.CoverProposalRequest;
import fr.insalan.blindtest.model.CoverProposal;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.repository.CoverProposalRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.ListenerRepository;

@RestController
@RequestMapping("/api/games/{id}/cover-proposal")
public class CoverProposalController {

    private final GameRepository gameRepository;
    private final ListenerRepository listenerRepository;
    private final CoverProposalRepository coverProposalRepository;

    public CoverProposalController(
        GameRepository gameRepository,
        ListenerRepository listenerRepository,
        CoverProposalRepository coverProposalRepository
    ) {
        this.gameRepository = gameRepository;
        this.listenerRepository = listenerRepository;
        this.coverProposalRepository = coverProposalRepository;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void propose(@PathVariable Integer id, @RequestParam Integer listenerId, @RequestBody CoverProposalRequest request) {
        Game game = gameRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jeu introuvable"));
        Listener listener = listenerRepository.findById(listenerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Joueur inconnu"));
        boolean withImage = request.imageUrl() != null && !request.imageUrl().isBlank();
        if (!withImage && game.getCoverUrl() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce jeu n'a pas encore d'image : il faut en proposer une");
        }
        coverProposalRepository.save(new CoverProposal(game, listener, withImage ? ImageUrls.require(request.imageUrl()) : null));
    }
}
