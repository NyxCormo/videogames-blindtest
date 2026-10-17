package fr.insalan.blindtest.controller.admin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.deletion.DeletePreview;
import fr.insalan.blindtest.deletion.DeletionService;
import fr.insalan.blindtest.dto.ListenerResponse;
import fr.insalan.blindtest.dto.admin.ListenerUsageResponse;
import fr.insalan.blindtest.dto.admin.MergeRequest;
import fr.insalan.blindtest.dto.admin.RenameRequest;
import fr.insalan.blindtest.listener.ListenerNames;
import fr.insalan.blindtest.merge.MergePreview;
import fr.insalan.blindtest.merge.MergeService;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.repository.ListenerRepository;

@RestController
@RequestMapping("/api/admin/listeners")
public class AdminListenerController {

    private final ListenerRepository listenerRepository;
    private final MergeService mergeService;
    private final DeletionService deletionService;
    private final ListenerNames listenerNames;

    public AdminListenerController(
        ListenerRepository listenerRepository,
        MergeService mergeService,
        DeletionService deletionService,
        ListenerNames listenerNames
    ) {
        this.listenerRepository = listenerRepository;
        this.mergeService = mergeService;
        this.deletionService = deletionService;
        this.listenerNames = listenerNames;
    }

    // Tous les pseudos avec leur nombre de votes et de blindtests, pour savoir lequel garder.
    @GetMapping
    public List<ListenerUsageResponse> list() {
        Map<Integer, Listener> listeners = listenerRepository.findAll().stream()
            .collect(Collectors.toMap(Listener::getId, listener -> listener));
        return listenerRepository.findAllWithUsage().stream()
            .map(usage -> ListenerUsageResponse.from(usage, listeners.get(usage.getId())))
            .toList();
    }

    @PatchMapping("/{id}")
    public ListenerResponse rename(@PathVariable Integer id, @RequestBody RenameRequest request) {
        Listener listener = listenerRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pseudo introuvable"));
        String name = listenerNames.validate(request.name(), id);
        listener.setName(name);
        listenerRepository.save(listener);
        return ListenerResponse.from(listener);
    }

    @GetMapping("/{id}/delete-preview")
    public DeletePreview deletePreview(@PathVariable Integer id) {
        return deletionService.previewListener(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        deletionService.deleteListener(id);
    }

    @GetMapping("/{id}/merge-preview")
    public MergePreview mergePreview(@PathVariable Integer id, @RequestParam Integer into) {
        return mergeService.previewListeners(id, into);
    }

    @PostMapping("/{id}/merge")
    public ListenerResponse merge(@PathVariable Integer id, @RequestBody MergeRequest request) {
        mergeService.mergeListeners(id, request.targetId());
        return ListenerResponse.from(listenerRepository.findById(request.targetId()).orElseThrow());
    }
}
