package fr.insalan.blindtest.youtube;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class YoutubeLinks {

    private static final Set<String> PLAYLIST_PARAMETERS = Set.of("list", "index", "start_radio");

    private YoutubeLinks() {
    }

    public static String withoutPlaylist(String link) {
        if (link == null) {
            return null;
        }
        int queryStart = link.indexOf('?');
        if (queryStart < 0) {
            return link;
        }
        int fragmentStart = link.indexOf('#', queryStart);
        String query = fragmentStart < 0 ? link.substring(queryStart + 1) : link.substring(queryStart + 1, fragmentStart);
        String fragment = fragmentStart < 0 ? "" : link.substring(fragmentStart);
        String kept = Arrays.stream(query.split("&"))
            .filter(parameter -> !parameter.isEmpty() && !PLAYLIST_PARAMETERS.contains(parameter.split("=", 2)[0]))
            .collect(Collectors.joining("&"));
        return link.substring(0, queryStart) + (kept.isEmpty() ? "" : "?" + kept) + fragment;
    }
}
