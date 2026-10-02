package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.deletion.DeletePreview;
import fr.insalan.blindtest.deletion.DeletionService;
import fr.insalan.blindtest.dto.GameResponse;
import fr.insalan.blindtest.dto.MergeRequest;
import fr.insalan.blindtest.dto.RenameRequest;
import fr.insalan.blindtest.merge.MergeService;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.repository.GameRepository;

@RestController
@RequestMapping("/api/admin/games")
public class AdminGameController {

    private final GameRepository gameRepository;
    private final MergeService mergeService;
    private final DeletionService deletionService;

    public AdminGameController(GameRepository gameRepository, MergeService mergeService, DeletionService deletionService) {
        this.gameRepository = gameRepository;
        this.mergeService = mergeService;
        this.deletionService = deletionService;
    }

    @PatchMapping("/{id}")
    public GameResponse rename(@PathVariable Integer id, @RequestBody RenameRequest request) {
        Game game = gameRepository.findByIdWithFranchise(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jeu introuvable"));
        String name = AdminNames.require(request.name());
        if (gameRepository.existsByFranchiseAndNameIgnoreCaseAndIdNot(game.getFranchise(), name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le jeu « " + name + " » existe déjà dans cette franchise");
        }
        game.setName(name);
        gameRepository.save(game);
        return GameResponse.from(game);
    }

    @PostMapping("/{id}/merge")
    public GameResponse merge(@PathVariable Integer id, @RequestBody MergeRequest request) {
        mergeService.mergeGames(id, request.targetId());
        return GameResponse.from(gameRepository.findByIdWithFranchise(request.targetId()).orElseThrow());
    }

    @GetMapping("/{id}/delete-preview")
    public DeletePreview deletePreview(@PathVariable Integer id) {
        return deletionService.previewGame(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        deletionService.deleteGame(id);
    }
}
