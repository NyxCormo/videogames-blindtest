package fr.insalan.blindtest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.CreateListenerRequest;
import fr.insalan.blindtest.dto.ListenerResponse;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.repository.ListenerRepository;

@RestController
@RequestMapping("/api/listeners")
public class ListenerController {

    private final ListenerRepository listenerRepository;

    public ListenerController(ListenerRepository listenerRepository) {
        this.listenerRepository = listenerRepository;
    }

    // Recherche approximative : simple correspondance "contient", insensible à la casse.
    // Sans aux fautes de frappe pour l'instant.
    @GetMapping
    public List<ListenerResponse> search(@RequestParam(defaultValue = "") String search) {
        return listenerRepository.findTop10ByNameContainingIgnoreCaseOrderByName(search).stream()
            .map(ListenerResponse::from)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListenerResponse create(@RequestBody CreateListenerRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire");
        }
        Listener listener = listenerRepository.save(new Listener(request.name()));
        return ListenerResponse.from(listener);
    }
}
