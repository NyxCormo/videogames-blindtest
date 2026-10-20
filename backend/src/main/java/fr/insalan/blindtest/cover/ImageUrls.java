package fr.insalan.blindtest.cover;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ImageUrls {

    private ImageUrls() {
    }

    public static String require(String raw) {
        String url = raw == null ? "" : raw.strip();
        if (!isWebAddress(url)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'adresse de l'image doit commencer par http:// ou https://");
        }
        return url;
    }

    private static boolean isWebAddress(String url) {
        try {
            URI uri = new URI(url);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
