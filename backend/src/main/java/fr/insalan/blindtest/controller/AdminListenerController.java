package fr.insalan.blindtest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.insalan.blindtest.deletion.DeletionService;
import fr.insalan.blindtest.dto.ListenerResponse;
import fr.insalan.blindtest.dto.ListenerUsageResponse;
import fr.insalan.blindtest.dto.MergeRequest;
import fr.insalan.blindtest.merge.MergeService;
import fr.insalan.blindtest.repository.ListenerRepository;

@RestController
@RequestMapping("/api/admin/listeners")
public class AdminListenerController {

    private final ListenerRepository listenerRepository;
    private final MergeService mergeService;
    private final DeletionService deletionService;

    public AdminListenerController(ListenerRepository listenerRepository, MergeService mergeService, DeletionService deletionService) {
        this.listenerRepository = listenerRepository;
        this.mergeService = mergeService;
        this.deletionService = deletionService;
    }

    // Tous les pseudos avec leur nombre de votes et de blindtests, pour savoir lequel garder.
    @GetMapping
    public List<ListenerUsageResponse> list() {
        return listenerRepository.findAllWithUsage().stream()
            .map(ListenerUsageResponse::from)
            .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        deletionService.deleteListener(id);
    }

    @PostMapping("/{id}/merge")
    public ListenerResponse merge(@PathVariable Integer id, @RequestBody MergeRequest request) {
        mergeService.mergeListeners(id, request.targetId());
        return ListenerResponse.from(listenerRepository.findById(request.targetId()).orElseThrow());
    }
}
