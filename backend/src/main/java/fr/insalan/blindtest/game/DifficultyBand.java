package fr.insalan.blindtest.game;

// Un palier de difficulté : la proportion (en %) des musiques du blindtest dont le ratio de connaissance
// doit tomber entre minDifficulty et maxDifficulty (0 = musiques les plus connues, 100 = les moins connues).
public record DifficultyBand(int minDifficulty, int maxDifficulty, int proportion) {
}
