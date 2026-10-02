package fr.insalan.blindtest.repository;

// Projection : Spring Data remplit ces méthodes à partir des colonnes de même nom de la requête.
public interface ListenerUsage {

    Integer getId();

    String getName();

    Long getVotes();

    Long getBlindtests();
}
