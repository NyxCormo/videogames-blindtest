package fr.insalan.blindtest.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.cover.AlbumImport;
import fr.insalan.blindtest.cover.CoverService;
import fr.insalan.blindtest.khinsider.KhinsiderClient;

@RestController
@RequestMapping("/api/khinsider")
public class AlbumPageController {

    private final CoverService coverService;
    private final String importToken;

    public AlbumPageController(CoverService coverService, @Value("${khinsider.import-token:}") String importToken) {
        this.coverService = coverService;
        this.importToken = importToken;
    }

    @PostMapping(value = "/album-page", consumes = MediaType.TEXT_HTML_VALUE)
    public AlbumImport importAlbumPage(
        @RequestHeader("X-Album-Url") String albumUrl,
        @RequestHeader("X-Import-Token") String token,
        @RequestBody String html
    ) {
        if (importToken.isBlank() || !importToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Jeton d'import invalide");
        }
        if (!albumUrl.startsWith(KhinsiderClient.ALBUM_PAGES)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce n'est pas une page d'album KHInsider");
        }
        return coverService.importAlbumPage(albumUrl, html)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune musique jouable de la base ne vient de cet album"));
    }
}
