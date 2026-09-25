import { useEffect, useState, type FormEvent } from 'react'
import { fetchGameTracks, fetchGames, type Game } from '../../api/games'
import type { Track } from '../../api/tracks'
import { NamePicker } from '../NamePicker/NamePicker'
import './GameGuessForm.css'

type Props = {
  onSubmit: (gameId: number, trackId: number | null) => void
  onPass: () => void
  disabled?: boolean
}

// Jeu (affiché avec sa franchise, pour les rares doublons de nom) puis musique (bonus, optionnelle) :
// choisis dans une liste, jamais tapés en texte libre.
export function GameGuessForm({ onSubmit, onPass, disabled = false }: Props) {
  const [games, setGames] = useState<Game[]>([])
  const [game, setGame] = useState<Game | null>(null)
  const [tracks, setTracks] = useState<Track[]>([])
  const [track, setTrack] = useState<Track | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    fetchGames(controller.signal)
      .then(setGames)
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

  return (
    <form onSubmit={handleSubmit} className="game-guess-form">
      <label>
        Jeu
        <NamePicker
          items={games}
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
    </form>
  )
}
