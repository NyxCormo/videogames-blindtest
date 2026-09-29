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
  const [franchiseWrong, setFranchiseWrong] = useState(false)

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
    setFranchiseWrong(false)
    onGuessFranchise(franchise.id)
      .then(({ correct, revealed }) => {
        if (!revealed) {
          setConfirmedFranchise(correct ? franchise : null)
          setFranchiseWrong(!correct)
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
              Je ne connais que la franchise
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
              {franchiseWrong && <p role="alert">Ce n'est pas la bonne franchise.</p>}
            </label>
          )}
        </div>
      )}
    </form>
  )
}
