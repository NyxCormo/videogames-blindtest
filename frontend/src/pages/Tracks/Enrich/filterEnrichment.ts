import type { Franchise } from '../../../api/franchises'
import type { Game } from '../../../api/games'
import { hasSource, type Track } from '../../../api/tracks'
import { matchesWords, searchWords } from '../../../textSearch'

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
  onlyTracksWithoutLinks: boolean,
): EnrichmentFranchise[] {
  const words = searchWords(query)
  const tracksByGame = new Map<number, Track[]>()
  for (const track of tracks) {
    const gameTracks = tracksByGame.get(track.gameId)
    if (gameTracks) {
      gameTracks.push(track)
    } else {
      tracksByGame.set(track.gameId, [track])
    }
  }

  const result: EnrichmentFranchise[] = []
  for (const franchise of franchises) {
    const franchiseGames = games.filter((game) => game.franchiseId === franchise.id)

    // Case à cocher active : seules les franchises sans jeu nous intéressent, les autres disparaissent.
    if (onlyFranchisesWithoutGames) {
      if (franchiseGames.length === 0 && matchesWords(franchise.name, words)) {
        result.push({ franchise, games: [] })
      }
      continue
    }

    // Sinon, une franchise sans jeu reste affichée par défaut (comme à l'étape précédente) : les
    // filtres sur les jeux ne s'appliquent pas, elle n'en a pas.
    if (franchiseGames.length === 0) {
      if (matchesWords(franchise.name, words)) {
        result.push({ franchise, games: [] })
      }
      continue
    }

    const matchingGames = franchiseGames.filter((game) => {
      const gameTracks = tracksByGame.get(game.id) ?? []
      if (onlyGamesWithoutTracks) {
        return gameTracks.length === 0 && matchesWords(`${franchise.name} ${game.name}`, words)
      }
      if (onlyTracksWithoutLinks && !gameTracks.some((track) => !hasSource(track))) return false
      return matchesWords(`${franchise.name} ${game.name}`, words)
    })

    if (matchingGames.length > 0) {
      result.push({ franchise, games: matchingGames })
    }
  }
  return result
}
