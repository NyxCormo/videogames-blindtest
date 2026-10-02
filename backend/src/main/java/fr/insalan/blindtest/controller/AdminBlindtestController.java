package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.deletion.DeletePreview;
import fr.insalan.blindtest.deletion.DeletionService;
import fr.insalan.blindtest.dto.BlindtestResponse;
import fr.insalan.blindtest.dto.RenameRequest;
import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.repository.BlindtestRepository;

@RestController
@RequestMapping("/api/admin/blindtests")
public class AdminBlindtestController {

    private final BlindtestRepository blindtestRepository;
    private final DeletionService deletionService;

    public AdminBlindtestController(BlindtestRepository blindtestRepository, DeletionService deletionService) {
        this.blindtestRepository = blindtestRepository;
        this.deletionService = deletionService;
    }

    // Pas de contrôle de doublon : deux blindtests peuvent porter le même nom, ils sont repérés par leur id.
    @PatchMapping("/{id}")
    public BlindtestResponse rename(@PathVariable Integer id, @RequestBody RenameRequest request) {
        Blindtest blindtest = blindtestRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blindtest introuvable"));
        blindtest.setName(AdminNames.require(request.name()));
        blindtestRepository.save(blindtest);
        return BlindtestResponse.from(blindtest);
    }

    @GetMapping("/{id}/delete-preview")
    public DeletePreview deletePreview(@PathVariable Integer id) {
        return deletionService.previewBlindtest(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        deletionService.deleteBlindtest(id);
    }
}
