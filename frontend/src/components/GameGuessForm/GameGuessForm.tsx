import { useEffect, useState, type FormEvent } from 'react'
import { fetchFranchises, type Franchise } from '../../api/franchises'
import { fetchGameTracks, fetchGames, type Game } from '../../api/games'
import type { Track } from '../../api/tracks'
import { NamePicker } from '../NamePicker/NamePicker'
import './GameGuessForm.css'

type Props = {
  onSubmit: (gameId: number, trackId: number | null) => void
  onGuessFranchise: (franchiseId: number) => Promise<{ correct: boolean; revealed: boolean }>
  onPass: () => void
  disabled?: boolean
}

// Jeu (affiché avec sa franchise, pour les rares doublons de nom) puis musique (bonus, optionnelle) :
// choisis dans une liste, jamais tapés en texte libre. Chemin de secours : valider juste la franchise
// pour un point partiel quand on est bloqué, sans avoir à trouver le jeu exact. La liste de franchises
// proposées est celle de toutes les franchises (même sans jeu) : ça sert de leurres, un joueur qui ne
// connaît pas la bonne réponse ne peut pas deviner que certaines ne sont jamais la bonne réponse.
export function GameGuessForm({ onSubmit, onGuessFranchise, onPass, disabled = false }: Props) {
  const [games, setGames] = useState<Game[]>([])
  const [game, setGame] = useState<Game | null>(null)
  const [tracks, setTracks] = useState<Track[]>([])
  const [track, setTrack] = useState<Track | null>(null)

  const [franchises, setFranchises] = useState<Franchise[]>([])
  const [franchiseHelp, setFranchiseHelp] = useState(false)
  const [franchise, setFranchise] = useState<Franchise | null>(null)
  const [confirmedFranchise, setConfirmedFranchise] = useState<Franchise | null>(null)
  const [franchiseSubmitting, setFranchiseSubmitting] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    fetchGames(controller.signal)
      .then(setGames)
      .catch(() => {})
    return () => controller.abort()
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    fetchFranchises(controller.signal)
      .then(setFranchises)
      .catch(() => {})
    return () => controller.abort()
  }, [])

  useEffect(() => {
    if (!game) {
      setTracks([])
      return
    }
    const controller = new AbortController()
    fetchGameTracks(game.id, controller.signal)
      .then(setTracks)
      .catch(() => {})
    return () => controller.abort()
  }, [game])

  // Une fois la franchise confirmée, la case "Jeu" ne propose plus que ses jeux (aide sans donner la réponse).
  const gamesToSearch = confirmedFranchise
    ? games.filter((item) => item.franchiseId === confirmedFranchise.id)
    : games

  function selectGame(selected: Game) {
    setGame(selected)
    setTrack(null)
  }

  function clearGame() {
    setGame(null)
    setTrack(null)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!game) return
    onSubmit(game.id, track ? track.id : null)
  }

  function handleGuessFranchise() {
    if (!franchise) return
    setFranchiseSubmitting(true)
    onGuessFranchise(franchise.id)
      .then(({ correct, revealed }) => {
        // Si la musique vient d'être révélée (essais épuisés), ce composant va disparaître :
        // pas la peine de mettre à jour un état local qui ne sera jamais affiché.
        if (!revealed) {
          setConfirmedFranchise(correct ? franchise : null)
          setFranchise(null)
        }
      })
      .finally(() => setFranchiseSubmitting(false))
  }

  return (
    <form onSubmit={handleSubmit} className="game-guess-form">
      <label>
        Jeu {confirmedFranchise && `(dans ${confirmedFranchise.name})`}
        <NamePicker
          items={gamesToSearch}
          selected={game}
          onSelect={selectGame}
          onClear={clearGame}
          placeholder="Chercher un jeu"
          disabled={disabled}
          renderLabel={(item) => `${item.name} (${item.franchiseName})`}
          getSearchText={(item) => `${item.name} ${item.franchiseName}`}
        />
      </label>
      {game && (
        <label>
          Musique (optionnel, bonus)
          <NamePicker
            items={tracks}
            selected={track}
            onSelect={setTrack}
            onClear={() => setTrack(null)}
            placeholder="Chercher la musique"
            disabled={disabled}
          />
        </label>
      )}
      <div className="game-guess-form-actions">
        <button type="submit" disabled={disabled || !game}>
          Valider
        </button>
        <button type="button" onClick={onPass} disabled={disabled}>
          Je passe
        </button>
      </div>

      {confirmedFranchise ? (
        <p className="franchise-found">Franchise trouvée : {confirmedFranchise.name} !</p>
      ) : (
        <div className="franchise-help">
          {!franchiseHelp ? (
            <button type="button" onClick={() => setFranchiseHelp(true)} disabled={disabled}>
              Je sais juste la franchise
            </button>
          ) : (
            <label>
              Franchise
              <div className="franchise-help-picker">
                <NamePicker
                  items={franchises}
                  selected={franchise}
                  onSelect={setFranchise}
                  onClear={() => setFranchise(null)}
                  placeholder="Chercher une franchise"
                  disabled={disabled || franchiseSubmitting}
                />
                {franchise && (
                  <button type="button" onClick={handleGuessFranchise} disabled={disabled || franchiseSubmitting}>
                    Valider la franchise
                  </button>
                )}
              </div>
            </label>
          )}
        </div>
      )}
    </form>
  )
}
