package fr.insalan.blindtest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.CreateFranchiseRequest;
import fr.insalan.blindtest.dto.FranchiseResponse;
import fr.insalan.blindtest.model.Franchise;
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

    // Retrouve la franchise si son nom existe déjà (même principe que l'import du Google Sheet), sinon la crée.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FranchiseResponse create(@RequestBody CreateFranchiseRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire");
        }
        Franchise franchise = franchiseRepository.findByName(request.name())
            .orElseGet(() -> franchiseRepository.save(new Franchise(request.name())));
        return FranchiseResponse.from(franchise);
    }
}
