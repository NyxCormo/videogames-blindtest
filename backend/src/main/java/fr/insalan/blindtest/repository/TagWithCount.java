package fr.insalan.blindtest.repository;

// Projection : Spring Data remplit ces méthodes à partir des colonnes de même nom de la requête.
public interface TagWithCount {

    Integer getId();

    String getName();

    String getTypeName();

    Long getTracks();
}
