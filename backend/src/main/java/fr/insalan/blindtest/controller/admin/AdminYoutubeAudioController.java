package fr.insalan.blindtest.controller.admin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.youtube.YoutubeAudioJob;
import fr.insalan.blindtest.youtube.YoutubeAudioService;
import fr.insalan.blindtest.youtube.YoutubeAudioStatus;

@RestController
@RequestMapping("/api/admin/youtube-audio")
public class AdminYoutubeAudioController {

    private final YoutubeAudioJob youtubeAudioJob;
    private final YoutubeAudioService youtubeAudioService;

    public AdminYoutubeAudioController(YoutubeAudioJob youtubeAudioJob, YoutubeAudioService youtubeAudioService) {
        this.youtubeAudioJob = youtubeAudioJob;
        this.youtubeAudioService = youtubeAudioService;
    }

    @GetMapping
    public YoutubeAudioStatus status() {
        return youtubeAudioJob.status();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public YoutubeAudioStatus start() {
        if (!youtubeAudioService.available()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucun téléchargeur YouTube sur ce serveur");
        }
        if (!youtubeAudioJob.startInBackground()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un téléchargement est déjà en cours");
        }
        return youtubeAudioJob.status();
    }
}
