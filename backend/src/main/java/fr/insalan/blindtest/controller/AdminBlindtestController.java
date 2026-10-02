package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.insalan.blindtest.deletion.DeletionService;

@RestController
@RequestMapping("/api/admin/blindtests")
public class AdminBlindtestController {

    private final DeletionService deletionService;

    public AdminBlindtestController(DeletionService deletionService) {
        this.deletionService = deletionService;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        deletionService.deleteBlindtest(id);
    }
}
