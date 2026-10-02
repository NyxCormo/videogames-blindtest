package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.FranchiseResponse;
import fr.insalan.blindtest.dto.MergeRequest;
import fr.insalan.blindtest.dto.RenameRequest;
import fr.insalan.blindtest.merge.MergeService;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.repository.FranchiseRepository;

@RestController
@RequestMapping("/api/admin/franchises")
public class AdminFranchiseController {

    private final FranchiseRepository franchiseRepository;
    private final MergeService mergeService;

    public AdminFranchiseController(FranchiseRepository franchiseRepository, MergeService mergeService) {
        this.franchiseRepository = franchiseRepository;
        this.mergeService = mergeService;
    }

    @PatchMapping("/{id}")
    public FranchiseResponse rename(@PathVariable Integer id, @RequestBody RenameRequest request) {
        Franchise franchise = franchiseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Franchise introuvable"));
        String name = AdminNames.require(request.name());
        if (franchiseRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La franchise « " + name + " » existe déjà");
        }
        franchise.setName(name);
        franchiseRepository.save(franchise);
        return FranchiseResponse.from(franchise);
    }

    @PostMapping("/{id}/merge")
    public FranchiseResponse merge(@PathVariable Integer id, @RequestBody MergeRequest request) {
        mergeService.mergeFranchises(id, request.targetId());
        return FranchiseResponse.from(franchiseRepository.findById(request.targetId()).orElseThrow());
    }
}
