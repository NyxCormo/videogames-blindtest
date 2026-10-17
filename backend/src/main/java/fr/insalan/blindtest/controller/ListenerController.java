package fr.insalan.blindtest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.CreateListenerRequest;
import fr.insalan.blindtest.dto.ListenerResponse;
import fr.insalan.blindtest.listener.ListenerNames;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.repository.ListenerRepository;

@RestController
@RequestMapping("/api/listeners")
public class ListenerController {

    private final ListenerRepository listenerRepository;
    private final ListenerNames listenerNames;

    public ListenerController(ListenerRepository listenerRepository, ListenerNames listenerNames) {
        this.listenerRepository = listenerRepository;
        this.listenerNames = listenerNames;
    }

    @GetMapping
    public List<ListenerResponse> search(@RequestParam(defaultValue = "") String search) {
        return listenerNames.search(search).stream()
            .map(ListenerResponse::from)
            .toList();
    }

    @GetMapping("/similar")
    public List<ListenerResponse> similar(@RequestParam(defaultValue = "") String name) {
        return listenerNames.similar(name).stream()
            .map(ListenerResponse::from)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListenerResponse create(@RequestBody CreateListenerRequest request) {
        String name = listenerNames.validate(request.name(), null);
        Listener listener = listenerRepository.save(new Listener(name));
        return ListenerResponse.from(listener);
    }

    @PostMapping("/{id}/session")
    public ListenerResponse openSession(@PathVariable Integer id) {
        Listener listener = listenerRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pseudo introuvable"));
        listener.markSeen();
        return ListenerResponse.from(listenerRepository.save(listener));
    }
}
