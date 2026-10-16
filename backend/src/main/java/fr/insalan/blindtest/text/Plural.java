package fr.insalan.blindtest.text;

public final class Plural {

    private Plural() {
    }

    public static String plural(long count, String singular, String pluralForm) {
        return count + " " + (count > 1 ? pluralForm : singular);
    }
}
