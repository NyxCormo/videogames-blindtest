import { useEffect, useRef, useState, type FormEvent } from 'react'
import { fetchFranchises, type Franchise } from '../../api/franchises'
import { fetchGameTracks, fetchGames, type Game } from '../../api/games'
import type { Track } from '../../api/tracks'
import { useNotification } from '../../context/NotificationContext'
import { CheckIcon } from '../icons'
import { NamePicker } from '../NamePicker/NamePicker'
import { Tabs } from '../Tabs/Tabs'
import './GameGuessForm.css'

type Mode = 'game' | 'franchise'

type Props = {
  foundFranchiseId: number | null
  onSubmit: (game: Game, track: Track | null) => void
  onGuessFranchise: (franchise: Franchise) => void
  disabled?: boolean
}

export function GameGuessForm({ foundFranchiseId, onSubmit, onGuessFranchise, disabled = false }: Props) {
  const { notify } = useNotification()
  const [mode, setMode] = useState<Mode>('game')
  const [games, setGames] = useState<Game[]>([])
  const [game, setGame] = useState<Game | null>(null)
  const [tracks, setTracks] = useState<Track[]>([])
  const [track, setTrack] = useState<Track | null>(null)
  const [franchises, setFranchises] = useState<Franchise[]>([])
  const [franchise, setFranchise] = useState<Franchise | null>(null)
  const submitRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    const controller = new AbortController()
    fetchGames(controller.signal)
      .then(setGames)
      .catch(() => {
        if (!controller.signal.aborted) notify('Impossible de charger la liste des jeux.', 'error')
      })
    return () => controller.abort()
  }, [notify])

  useEffect(() => {
    const controller = new AbortController()
    fetchFranchises(controller.signal)
      .then(setFranchises)
      .catch(() => {
        if (!controller.signal.aborted) notify('Impossible de charger la liste des franchises.', 'error')
      })
    return () => controller.abort()
  }, [notify])

  useEffect(() => {
    if (!game) {
      setTracks([])
      return
    }
    const controller = new AbortController()
    fetchGameTracks(game.id, controller.signal)
      .then(setTracks)
      .catch(() => {
        if (!controller.signal.aborted) notify(`Impossible de charger les musiques de « ${game.name} ».`, 'error')
      })
    return () => controller.abort()
  }, [game, notify])

  useEffect(() => {
    if (franchise) submitRef.current?.focus()
  }, [franchise])

  // Une fois la franchise trouvée, la case "Jeu" ne propose plus que ses jeux (aide sans donner la réponse).
  const foundFranchise = franchises.find((item) => item.id === foundFranchiseId) ?? null
  const gamesToSearch = foundFranchise ? games.filter((item) => item.franchiseId === foundFranchise.id) : games
  const ready = mode === 'game' ? game !== null : franchise !== null

  function selectGame(selected: Game) {
    setGame(selected)
    setTrack(null)
  }

  function selectTrack(selected: Track) {
    setTrack(selected)
    submitRef.current?.focus()
  }

  function clearGame() {
    setGame(null)
    setTrack(null)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (mode === 'game' && game) onSubmit(game, track)
    if (mode === 'franchise' && franchise) onGuessFranchise(franchise)
  }

  return (
    <form onSubmit={handleSubmit} className="game-guess-form">
      <Tabs
        tabs={[
          { id: 'game', label: 'Jeu' },
          { id: 'franchise', label: 'Franchise' },
        ]}
        active={mode}
        onChange={(id) => setMode(id === 'franchise' ? 'franchise' : 'game')}
      />
      {foundFranchise && (
        <p className="franchise-found">
          <CheckIcon /> Franchise trouvée : {foundFranchise.name}
        </p>
      )}
      {mode === 'game' ? (
        <>
          <NamePicker
            items={gamesToSearch}
            selected={game}
            onSelect={selectGame}
            onClear={clearGame}
            placeholder="Chercher un jeu"
            disabled={disabled}
            autoFocus
            renderLabel={(item) => `${item.name} (${item.franchiseName})`}
            getSearchText={(item) => `${item.name} ${item.franchiseName}`}
          />
          {game && (
            <label>
              Musique (optionnel, bonus)
              <NamePicker
                items={tracks}
                selected={track}
                onSelect={selectTrack}
                onClear={() => setTrack(null)}
                placeholder="Chercher la musique"
                disabled={disabled}
                autoFocus
              />
            </label>
          )}
        </>
      ) : (
        !foundFranchise && (
          <NamePicker
            items={franchises}
            selected={franchise}
            onSelect={setFranchise}
            onClear={() => setFranchise(null)}
            placeholder="Chercher une franchise"
            disabled={disabled}
            autoFocus
          />
        )
      )}
      <div className="game-guess-form-actions">
        <button ref={submitRef} type="submit" className="button-primary" disabled={disabled || !ready}>
          Valider
        </button>
      </div>
    </form>
  )
}
