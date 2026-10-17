package fr.insalan.blindtest.text;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class TextSearch {

    private TextSearch() {
    }

    // "Pokémon : Rouge" -> "pokemonrouge", même règle que frontend/src/textSearch.ts.
    public static String normalize(String text) {
        String lower = text.toLowerCase(Locale.ROOT).replace("œ", "oe").replace("æ", "ae").replace("ß", "ss");
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    public static List<String> words(String query) {
        return Arrays.stream(query.split("\\s+"))
            .map(TextSearch::normalize)
            .filter(word -> !word.isEmpty())
            .toList();
    }

    public static boolean matches(String text, List<String> words) {
        String normalized = normalize(text);
        return words.stream().allMatch(normalized::contains);
    }
}
