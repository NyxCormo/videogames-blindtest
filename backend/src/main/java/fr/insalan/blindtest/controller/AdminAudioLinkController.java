package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.audiolink.AudioLinkRefreshJob;
import fr.insalan.blindtest.audiolink.AudioLinkRefreshStatus;

@RestController
@RequestMapping("/api/admin/audio-links/refresh")
public class AdminAudioLinkController {

    private final AudioLinkRefreshJob audioLinkRefreshJob;

    public AdminAudioLinkController(AudioLinkRefreshJob audioLinkRefreshJob) {
        this.audioLinkRefreshJob = audioLinkRefreshJob;
    }

    // 202 : le balayage prend plusieurs minutes, on répond tout de suite et le front suit l'avancement avec le GET.
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AudioLinkRefreshStatus start() {
        if (!audioLinkRefreshJob.startInBackground()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un rafraîchissement est déjà en cours");
        }
        return audioLinkRefreshJob.status();
    }

    @GetMapping
    public AudioLinkRefreshStatus status() {
        return audioLinkRefreshJob.status();
    }
}
