import type { Franchise } from '../../api/franchises'
import type { Game } from '../../api/games'
import type { Track } from '../../api/tracks'

// Normalzation : "Shaël" -> "shael"
function normalize(text: string): string {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}

export type EnrichmentFranchise = {
  franchise: Franchise
  games: Game[]
}

// Un jeu sans musique n'a rien à filtrer par nom de musique : il reste affiché tant que la franchise
// ou son propre nom correspond à la recherche. Pareil pour une franchise sans jeu.
export function filterEnrichment(
  franchises: Franchise[],
  games: Game[],
  tracks: Track[],
  query: string,
  onlyFranchisesWithoutGames: boolean,
  onlyGamesWithoutTracks: boolean,
  maxTracksPerGame: number,
): EnrichmentFranchise[] {
  const words = normalize(query).split(/\s+/).filter(Boolean)
  const trackCountByGame = new Map<number, number>()
  for (const track of tracks) {
    trackCountByGame.set(track.gameId, (trackCountByGame.get(track.gameId) ?? 0) + 1)
  }

  function matchesWords(text: string): boolean {
    const normalized = normalize(text)
    return words.every((word) => normalized.includes(word))
  }

  const result: EnrichmentFranchise[] = []
  for (const franchise of franchises) {
    const franchiseGames = games.filter((game) => game.franchiseId === franchise.id)

    // Case à cocher active : seules les franchises sans jeu nous intéressent, les autres disparaissent.
    if (onlyFranchisesWithoutGames) {
      if (franchiseGames.length === 0 && matchesWords(franchise.name)) {
        result.push({ franchise, games: [] })
      }
      continue
    }

    // Sinon, une franchise sans jeu reste affichée par défaut (comme à l'étape précédente) : les
    // filtres sur les jeux ne s'appliquent pas, elle n'en a pas.
    if (franchiseGames.length === 0) {
      if (matchesWords(franchise.name)) {
        result.push({ franchise, games: [] })
      }
      continue
    }

    const matchingGames = franchiseGames.filter((game) => {
      const trackCount = trackCountByGame.get(game.id) ?? 0
      if (onlyGamesWithoutTracks && trackCount !== 0) return false
      if (trackCount > maxTracksPerGame) return false
      return matchesWords(`${franchise.name} ${game.name}`)
    })

    if (matchingGames.length > 0) {
      result.push({ franchise, games: matchingGames })
    }
  }
  return result
}
