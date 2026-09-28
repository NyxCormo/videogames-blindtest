import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { createFranchise, fetchFranchises, type Franchise } from '../../api/franchises'
import { createGame, fetchGames, type Game } from '../../api/games'
import { addKhinsiderLink, addYoutubeLink, createTrack, fetchTracks, type Track } from '../../api/tracks'
import { filterEnrichment } from './filterEnrichment'
import { TrackLinkControl } from './TrackLinkControl'
import './EnrichmentPage.css'

const MAX_TRACKS_PER_GAME = 25

export function EnrichmentPage() {
  const [franchises, setFranchises] = useState<Franchise[] | null>(null)
  const [games, setGames] = useState<Game[]>([])
  const [tracks, setTracks] = useState<Track[]>([])
  const [error, setError] = useState(false)
  const [query, setQuery] = useState('')
  const [onlyFranchisesWithoutGames, setOnlyFranchisesWithoutGames] = useState(false)
  const [onlyGamesWithoutTracks, setOnlyGamesWithoutTracks] = useState(false)
  const [maxTracksPerGame, setMaxTracksPerGame] = useState(MAX_TRACKS_PER_GAME)
  const [onlyTracksWithoutLinks, setOnlyTracksWithoutLinks] = useState(false)

  const [addingFranchise, setAddingFranchise] = useState(false)
  const [franchiseDraft, setFranchiseDraft] = useState('')
  const [addingGameFor, setAddingGameFor] = useState<number | null>(null)
  const [gameDraft, setGameDraft] = useState('')
  const [addingTrackFor, setAddingTrackFor] = useState<number | null>(null)
  const [trackDraft, setTrackDraft] = useState('')
  const [addingKhinsiderFor, setAddingKhinsiderFor] = useState<number | null>(null)
  const [khinsiderDraft, setKhinsiderDraft] = useState('')
  const [addingYoutubeFor, setAddingYoutubeFor] = useState<number | null>(null)
  const [youtubeDraft, setYoutubeDraft] = useState('')
  const [createError, setCreateError] = useState<string | null>(null)
  const [khinsiderFeedback, setKhinsiderFeedback] = useState<string | null>(null)

  function loadAll(signal?: AbortSignal) {
    return Promise.all([fetchFranchises(signal), fetchGames(signal), fetchTracks(signal)]).then(
      ([allFranchises, allGames, allTracks]) => {
        setFranchises(allFranchises)
        setGames(allGames)
        setTracks(allTracks)
      },
    )
  }

  useEffect(() => {
    const controller = new AbortController()
    loadAll(controller.signal).catch(() => {
      if (!controller.signal.aborted) setError(true)
    })
    return () => controller.abort()
  }, [])

  const shown = useMemo(
    () =>
      franchises
        ? filterEnrichment(
            franchises,
            games,
            tracks,
            query,
            onlyFranchisesWithoutGames,
            onlyGamesWithoutTracks,
            maxTracksPerGame,
            onlyTracksWithoutLinks,
          )
        : [],
    [
      franchises,
      games,
      tracks,
      query,
      onlyFranchisesWithoutGames,
      onlyGamesWithoutTracks,
      maxTracksPerGame,
      onlyTracksWithoutLinks,
    ],
  )

  function submitNewFranchise(event: FormEvent) {
    event.preventDefault()
    const name = franchiseDraft.trim()
    if (!name) return
    setCreateError(null)
    createFranchise(name)
      .then(() => {
        setFranchiseDraft('')
        setAddingFranchise(false)
        return loadAll()
      })
      .catch((err: Error) => setCreateError(err.message))
  }

  function submitNewGame(event: FormEvent, franchiseId: number) {
    event.preventDefault()
    const name = gameDraft.trim()
    if (!name) return
    setCreateError(null)
    createGame(name, franchiseId)
      .then(() => {
        setGameDraft('')
        setAddingGameFor(null)
        return loadAll()
      })
      .catch((err: Error) => setCreateError(err.message))
  }

  function submitNewTrack(event: FormEvent, gameId: number) {
    event.preventDefault()
    const name = trackDraft.trim()
    if (!name) return
    setCreateError(null)
    createTrack(name, gameId)
      .then(() => {
        setTrackDraft('')
        setAddingTrackFor(null)
        return loadAll()
      })
      .catch((err: Error) => setCreateError(err.message))
  }

  function submitKhinsiderLink(event: FormEvent, trackId: number) {
    event.preventDefault()
    const link = khinsiderDraft.trim()
    if (!link) return
    setCreateError(null)
    setKhinsiderFeedback(null)
    addKhinsiderLink(trackId, link)
      .then((track) => {
        setKhinsiderDraft('')
        setAddingKhinsiderFor(null)
        if (!track.audioLink) {
          setKhinsiderFeedback(
            "Lien ajouté, mais la musique n'est pas encore jouable : nouvelle tentative à la prochaine passe planifiée.",
          )
        }
        return loadAll()
      })
      .catch((err: Error) => setCreateError(err.message))
  }

  function submitYoutubeLink(event: FormEvent, trackId: number) {
    event.preventDefault()
    const link = youtubeDraft.trim()
    if (!link) return
    setCreateError(null)
    addYoutubeLink(trackId, link)
      .then(() => {
        setYoutubeDraft('')
        setAddingYoutubeFor(null)
        return loadAll()
      })
      .catch((err: Error) => setCreateError(err.message))
  }

  if (error) {
    return <p role="alert">Impossible de charger les données.</p>
  }
  if (franchises === null) {
    return <p>Chargement...</p>
  }

  return (
    <>
      <h1>Enrichissement</h1>

      {addingFranchise ? (
        <form className="enrichment-add-form" onSubmit={submitNewFranchise}>
          <input
            type="text"
            placeholder="Nom de la franchise"
            aria-label="Nom de la franchise"
            value={franchiseDraft}
            onChange={(event) => setFranchiseDraft(event.target.value)}
            autoFocus
          />
          <button type="submit">Créer</button>
          <button
            type="button"
            onClick={() => {
              setAddingFranchise(false)
              setFranchiseDraft('')
            }}
          >
            Annuler
          </button>
        </form>
      ) : (
        <button
          type="button"
          className="enrichment-add-franchise"
          onClick={() => {
            setAddingFranchise(true)
            setCreateError(null)
          }}
        >
          Ajouter une franchise
        </button>
      )}
      {createError && <p role="alert">{createError}</p>}
      {khinsiderFeedback && <p>{khinsiderFeedback}</p>}

      <div className="controls">
        <input
          type="search"
          placeholder="Rechercher une franchise ou un jeu"
          aria-label="Rechercher"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        <label>
          <input
            type="checkbox"
            checked={onlyFranchisesWithoutGames}
            onChange={(event) => setOnlyFranchisesWithoutGames(event.target.checked)}
          />
          Franchises sans jeu uniquement
        </label>
        <label>
          <input
            type="checkbox"
            checked={onlyGamesWithoutTracks}
            onChange={(event) => setOnlyGamesWithoutTracks(event.target.checked)}
          />
          Jeux sans musique uniquement
        </label>
        <label>
          <input
            type="checkbox"
            checked={onlyTracksWithoutLinks}
            onChange={(event) => setOnlyTracksWithoutLinks(event.target.checked)}
          />
          Musiques sans lien uniquement
        </label>
        <label className="enrichment-slider">
          Jeux avec au plus {maxTracksPerGame} musique{maxTracksPerGame > 1 ? 's' : ''}
          <input
            type="range"
            min={0}
            max={MAX_TRACKS_PER_GAME}
            value={maxTracksPerGame}
            onChange={(event) => setMaxTracksPerGame(Number(event.target.value))}
          />
        </label>
      </div>
      {shown.length === 0 ? (
        <p>Aucune franchise ne correspond.</p>
      ) : (
      <div className="enrichment-tree">
        {shown.map(({ franchise, games: franchiseGames }) => {
          const totalGames = games.filter((game) => game.franchiseId === franchise.id).length
          const totalTracks = games
            .filter((game) => game.franchiseId === franchise.id)
            .reduce((total, game) => total + tracks.filter((track) => track.gameId === game.id).length, 0)
          return (
            <details key={franchise.id} className="enrichment-franchise" open>
              <summary>
                <span>
                  {franchise.name} ({totalGames}) ({totalTracks})
                </span>
                <button
                  type="button"
                  onClick={(event) => {
                    event.stopPropagation()
                    event.currentTarget.closest('details')!.open = true
                    setAddingGameFor(franchise.id)
                    setCreateError(null)
                  }}
                >
                  Ajouter un jeu
                </button>
              </summary>
              <div className="enrichment-games">
                {addingGameFor === franchise.id && (
                  <form className="enrichment-add-form" onSubmit={(event) => submitNewGame(event, franchise.id)}>
                    <input
                      type="text"
                      placeholder="Nom du jeu"
                      aria-label="Nom du jeu"
                      value={gameDraft}
                      onChange={(event) => setGameDraft(event.target.value)}
                      autoFocus
                    />
                    <button type="submit">Créer</button>
                    <button
                      type="button"
                      onClick={() => {
                        setAddingGameFor(null)
                        setGameDraft('')
                      }}
                    >
                      Annuler
                    </button>
                  </form>
                )}
                {franchiseGames.map((game) => {
                  const gameTracks = tracks.filter((track) => track.gameId === game.id)
                  return (
                    <details key={game.id} className="enrichment-game">
                      <summary>
                        <span>
                          {game.name} ({gameTracks.length})
                        </span>
                        <button
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation()
                            event.currentTarget.closest('details')!.open = true
                            setAddingTrackFor(game.id)
                            setCreateError(null)
                          }}
                        >
                          Ajouter une musique
                        </button>
                      </summary>
                      <ul className="enrichment-tracks">
                        {addingTrackFor === game.id && (
                          <li>
                            <form className="enrichment-add-form" onSubmit={(event) => submitNewTrack(event, game.id)}>
                              <input
                                type="text"
                                placeholder="Nom de la musique"
                                aria-label="Nom de la musique"
                                value={trackDraft}
                                onChange={(event) => setTrackDraft(event.target.value)}
                                autoFocus
                              />
                              <button type="submit">Créer</button>
                              <button
                                type="button"
                                onClick={() => {
                                  setAddingTrackFor(null)
                                  setTrackDraft('')
                                }}
                              >
                                Annuler
                              </button>
                            </form>
                          </li>
                        )}
                        {gameTracks.map((track) => (
                          <li key={track.id} className="enrichment-track">
                            <span>{track.name}</span>
                            <span className="enrichment-track-links">
                              <TrackLinkControl
                                track={track}
                                kind="khinsider"
                                addingFor={addingKhinsiderFor}
                                draft={khinsiderDraft}
                                onStartAdding={() => {
                                  setAddingKhinsiderFor(track.id)
                                  setCreateError(null)
                                  setKhinsiderFeedback(null)
                                }}
                                onDraftChange={setKhinsiderDraft}
                                onCancel={() => {
                                  setAddingKhinsiderFor(null)
                                  setKhinsiderDraft('')
                                }}
                                onSubmit={(event) => submitKhinsiderLink(event, track.id)}
                              />
                              <TrackLinkControl
                                track={track}
                                kind="youtube"
                                addingFor={addingYoutubeFor}
                                draft={youtubeDraft}
                                onStartAdding={() => {
                                  setAddingYoutubeFor(track.id)
                                  setCreateError(null)
                                }}
                                onDraftChange={setYoutubeDraft}
                                onCancel={() => {
                                  setAddingYoutubeFor(null)
                                  setYoutubeDraft('')
                                }}
                                onSubmit={(event) => submitYoutubeLink(event, track.id)}
                              />
                            </span>
                          </li>
                        ))}
                      </ul>
                    </details>
                  )
                })}
              </div>
            </details>
          )
        })}
      </div>
      )}
    </>
  )
}
