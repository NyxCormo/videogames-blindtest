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

import fr.insalan.blindtest.dto.BlindtestResponse;
import fr.insalan.blindtest.dto.CreateBlindtestRequest;
import fr.insalan.blindtest.game.BlindtestGenerator;
import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.repository.BlindtestRepository;

@RestController 
@RequestMapping("/api/blindtests")
public class BlindtestController {
    
    private final BlindtestRepository blindtestRepository;
    private final BlindtestGenerator blindtestGenerator;

    public BlindtestController(
        BlindtestRepository blindtestRepository, 
        BlindtestGenerator blindtestGenerator
    ){
        this.blindtestRepository = blindtestRepository;
        this.blindtestGenerator = blindtestGenerator;
    }

    @GetMapping 
    public List<BlindtestResponse> list() {
        return blindtestRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(BlindtestResponse::from)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlindtestResponse create(@RequestBody CreateBlindtestRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire");
        }
        if (request.trackCount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nombre de musiques doit être positif");
        }
        if (request.difficulty() < 0 || request.difficulty() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La difficulté doit être comprise entre 0 et 100");
        }

        Blindtest blindtest;
        try {
            blindtest = blindtestGenerator.generate(request.name(), request.trackCount(), request.difficulty());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return BlindtestResponse.from(blindtest);
    }
}
