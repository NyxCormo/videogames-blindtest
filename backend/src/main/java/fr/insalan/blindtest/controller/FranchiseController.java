package fr.insalan.blindtest.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.insalan.blindtest.dto.FranchiseResponse;
import fr.insalan.blindtest.dto.GameResponse;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;

@RestController
@RequestMapping("/api/franchises")
public class FranchiseController {

    private final FranchiseRepository franchiseRepository;
    private final GameRepository gameRepository;

    public FranchiseController(FranchiseRepository franchiseRepository, GameRepository gameRepository) {
        this.franchiseRepository = franchiseRepository;
        this.gameRepository = gameRepository;
    }

    @GetMapping
    public List<FranchiseResponse> list() {
        return franchiseRepository.findAllByOrderByNameAsc().stream()
            .map(FranchiseResponse::from)
            .toList();
    }

    @GetMapping("/{id}/games")
    public List<GameResponse> games(@PathVariable Integer id) {
        return gameRepository.findByFranchiseIdOrderByNameAsc(id).stream()
            .map(GameResponse::from)
            .toList();
    }
}
