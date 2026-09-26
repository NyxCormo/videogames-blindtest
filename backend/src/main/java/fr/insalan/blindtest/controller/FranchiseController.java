package fr.insalan.blindtest.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.insalan.blindtest.dto.FranchiseResponse;
import fr.insalan.blindtest.repository.FranchiseRepository;

// Toutes les franchises, y compris celles sans jeu : la case "je sais juste la franchise" de la réponse
// à un blindtest en a besoin comme leurres (demande de Clément, 26/09) — une franchise sans jeu ne peut
// jamais être la bonne réponse, mais le joueur ne le sait pas, ce qui rend le hasard moins payant.
@RestController
@RequestMapping("/api/franchises")
public class FranchiseController {

    private final FranchiseRepository franchiseRepository;

    public FranchiseController(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    @GetMapping
    public List<FranchiseResponse> list() {
        return franchiseRepository.findAllByOrderByNameAsc().stream()
            .map(FranchiseResponse::from)
            .toList();
    }
}
