package fr.insalan.blindtest.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.insalan.blindtest.dto.ServerTimeResponse;

@RestController
public class TimeController {

    @GetMapping("/api/time")
    public ServerTimeResponse now() {
        return new ServerTimeResponse(System.currentTimeMillis());
    }
}
