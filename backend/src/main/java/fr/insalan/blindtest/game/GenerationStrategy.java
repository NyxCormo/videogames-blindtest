package fr.insalan.blindtest.game;

// Comment on choisit les musiques d'un palier quand plusieurs conviennent.
public enum GenerationStrategy {
    // Tirage au hasard, sans distinction. Le plus simple, essayé en premier.
    RANDOM,
    // Les musiques des jeux les moins représentés parmi les candidates passent en premier : laisse plus de
    // chances aux jeux communs (répartis sur plusieurs paliers) de ne pas épuiser leur plafond dans un seul palier.
    RARE_GAMES,
    // Même principe, par franchise plutôt que par jeu.
    RARE_FRANCHISES
}
