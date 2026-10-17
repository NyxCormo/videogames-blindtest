package fr.insalan.blindtest.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextSearchTests {

    @Test
    void keepsOnlyLettersAndDigitsWithoutAccents() {
        assertThat(TextSearch.normalize("Shaël : Stellar-Blade 2")).isEqualTo("shaelstellarblade2");
        assertThat(TextSearch.normalize("Cœur")).isEqualTo("coeur");
    }

    @Test
    void cutsTheQueryIntoCleanWords() {
        assertThat(TextSearch.words("  Shaël,  DAWN - ")).containsExactly("shael", "dawn");
    }

    @Test
    void needsEveryWordInAnyOrder() {
        assertThat(TextSearch.matches("Stellar Blade: Dawn", TextSearch.words("dawn stellar"))).isTrue();
        assertThat(TextSearch.matches("Stellar Blade: Dawn", TextSearch.words("dawn raven"))).isFalse();
    }
}
