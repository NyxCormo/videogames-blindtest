package fr.insalan.blindtest.game;

import java.util.Optional;

import fr.insalan.blindtest.model.Track;

// Résultat d'une tentative de réponse : "correct" dit si CETTE tentative précise était juste (le jeu
// pour guess(), la franchise pour guessFranchise()) ; "revealed" est présent si la musique vient d'être
// révélée, soit parce qu'elle est trouvée, soit parce que les essais sont épuisés (les deux sont possibles
// indépendamment : une franchise juste sur le dernier essai revient un résultat correct ET révélé).
public record GuessResult(boolean correct, Optional<Track> revealed) {
}
