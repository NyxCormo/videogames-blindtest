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

// Inclut les franchises sans jeu, utilisées comme leurres dans la réponse à un blindtest.
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FranchiseResponse create(@RequestBody CreateFranchiseRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire");
        }
        if (franchiseRepository.findByName(request.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La franchise « " + request.name() + " » existe déjà");
        }
        return FranchiseResponse.from(franchiseRepository.save(new Franchise(request.name())));
    }
}
