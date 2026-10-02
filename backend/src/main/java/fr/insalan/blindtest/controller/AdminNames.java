package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class AdminNames {

    private AdminNames() {
    }

    static String require(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire");
        }
        return name.trim();
    }
}
