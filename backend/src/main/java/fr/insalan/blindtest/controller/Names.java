package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Names {

    private Names() {
    }

    public static String require(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire");
        }
        return name.trim();
    }
}
