package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.FranchiseResponse;
import fr.insalan.blindtest.dto.RenameRequest;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.repository.FranchiseRepository;

@RestController
@RequestMapping("/api/admin/franchises")
public class AdminFranchiseController {

    private final FranchiseRepository franchiseRepository;

    public AdminFranchiseController(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
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
}
