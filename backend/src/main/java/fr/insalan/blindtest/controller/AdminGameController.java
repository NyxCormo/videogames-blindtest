package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.GameResponse;
import fr.insalan.blindtest.dto.RenameRequest;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.repository.GameRepository;

@RestController
@RequestMapping("/api/admin/games")
public class AdminGameController {

    private final GameRepository gameRepository;

    public AdminGameController(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
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
}
