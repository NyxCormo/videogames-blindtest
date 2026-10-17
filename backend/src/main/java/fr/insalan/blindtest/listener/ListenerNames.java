package fr.insalan.blindtest.listener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.text.TextSearch;

@Service
public class ListenerNames {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 20;
    private static final int SEARCH_LIMIT = 10;
    private static final int SIMILAR_LIMIT = 5;
    private static final Pattern ALLOWED = Pattern.compile("[\\p{L}\\p{M}\\p{N} ._'’-]+");

    private final ListenerRepository listenerRepository;
    private final Set<String> forbiddenWords;

    public ListenerNames(ListenerRepository listenerRepository) throws IOException {
        this.listenerRepository = listenerRepository;
        try (InputStream input = getClass().getResourceAsStream("/pseudos-interdits.txt")) {
            this.forbiddenWords = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(TextSearch::normalize)
                .collect(Collectors.toSet());
        }
    }

    public String validate(String rawName, Integer excludedId) {
        String name = checkRules(rawName);
        Optional<Listener> taken = findSame(name, excludedId);
        if (taken.isPresent()) {
            String existing = taken.get().getName();
            throw new ResponseStatusException(HttpStatus.CONFLICT, existing.equals(name)
                ? "Le pseudo « " + name + " » existe déjà"
                : "Le pseudo « " + name + " » existe déjà sous la forme « " + existing + " »");
        }
        return name;
    }

    public String checkRules(String rawName) {
        String name = clean(rawName);
        int length = name.codePointCount(0, name.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw badRequest("Un pseudo fait entre " + MIN_LENGTH + " et " + MAX_LENGTH + " caractères");
        }
        if (!ALLOWED.matcher(name).matches() || TextSearch.normalize(name).isEmpty()) {
            throw badRequest("Un pseudo ne contient que des lettres, des chiffres, des espaces et les signes - _ . '");
        }
        if (isForbidden(name)) {
            throw badRequest("Ce pseudo n'est pas accepté");
        }
        return name;
    }

    public List<Listener> search(String query) {
        List<String> words = TextSearch.words(query);
        if (words.isEmpty()) {
            return List.of();
        }
        String whole = String.join("", words);
        return listenerRepository.findAll().stream()
            .filter(listener -> TextSearch.matches(listener.getName(), words))
            .sorted(Comparator
                .comparing((Listener listener) -> !TextSearch.normalize(listener.getName()).equals(whole))
                .thenComparing(listener -> !TextSearch.normalize(listener.getName()).startsWith(words.get(0)))
                .thenComparing(Listener::getName, String.CASE_INSENSITIVE_ORDER))
            .limit(SEARCH_LIMIT)
            .toList();
    }

    public List<Listener> similar(String rawName) {
        String target = TextSearch.normalize(checkRules(rawName));
        return listenerRepository.findAll().stream()
            .filter(listener -> isSimilar(TextSearch.normalize(listener.getName()), target))
            .sorted(Comparator.comparingInt((Listener listener) -> distance(TextSearch.normalize(listener.getName()), target))
                .thenComparing(Listener::getName, String.CASE_INSENSITIVE_ORDER))
            .limit(SIMILAR_LIMIT)
            .toList();
    }

    private static String clean(String rawName) {
        if (rawName == null) {
            return "";
        }
        return rawName.replaceAll("\\s+", " ").replaceAll("[\\p{Cf}\\p{Cc}]", "").replaceAll(" {2,}", " ").strip();
    }

    private boolean isForbidden(String name) {
        return forbiddenWords.contains(TextSearch.normalize(name))
            || Arrays.stream(name.split("[^\\p{L}\\p{M}\\p{N}]+"))
                .map(TextSearch::normalize)
                .anyMatch(forbiddenWords::contains);
    }

    private Optional<Listener> findSame(String name, Integer excludedId) {
        String normalized = TextSearch.normalize(name);
        return listenerRepository.findAll().stream()
            .filter(listener -> !listener.getId().equals(excludedId))
            .filter(listener -> TextSearch.normalize(listener.getName()).equals(normalized))
            .findFirst();
    }

    private static boolean isSimilar(String existing, String target) {
        int shorter = Math.min(existing.length(), target.length());
        if (shorter >= 3 && (existing.contains(target) || target.contains(existing))) {
            return true;
        }
        return distance(existing, target) <= (shorter <= 5 ? 1 : 2);
    }

    private static int distance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int change = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + change);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
