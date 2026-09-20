package fr.insalan.blindtest.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.insalan.blindtest.dto.TrackResponse;
import fr.insalan.blindtest.repository.TrackRepository;

@RestController 
@RequestMapping("/api/tracks")
public class TrackController {

    private final TrackRepository trackRepository;

    public TrackController(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    @GetMapping 
    public List<TrackResponse> list() {
        return trackRepository.findAllWithGameAndFranchise().stream()
            .map(TrackResponse::from)
            .toList();
    }
}