import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { createFranchise, fetchFranchises, type Franchise } from '../../api/franchises'
import { createGame, fetchGames, type Game } from '../../api/games'
import { addKhinsiderLink, addYoutubeLink, createTrack, fetchTracks, hasSource, type Track } from '../../api/tracks'
import { EntityList } from '../../components/EntityList/EntityList'
import { plural } from '../../components/EntityList/listText'
import { useNotification } from '../../context/NotificationContext'
import { filterEnrichment } from './filterEnrichment'
import { TrackLinkControl } from './TrackLinkControl'
import './EnrichmentPage.css'

const MAX_TRACKS_PER_GAME = 25

export function EnrichmentPage() {
  const { notify } = useNotification()
  const [franchises, setFranchises] = useState<Franchise[] | null>(null)
  const [games, setGames] = useState<Game[]>([])
  const [tracks, setTracks] = useState<Track[]>([])
  const [error, setError] = useState(false)
  const [query, setQuery] = useState('')
  const [onlyFranchisesWithoutGames, setOnlyFranchisesWithoutGames] = useState(false)
  const [onlyGamesWithoutTracks, setOnlyGamesWithoutTracks] = useState(false)
  const [maxTracksPerGame, setMaxTracksPerGame] = useState(MAX_TRACKS_PER_GAME)
  const [onlyTracksWithoutLinks, setOnlyTracksWithoutLinks] = useState(false)

  const [franchiseId, setFranchiseId] = useState<number | null>(null)
  const [gameId, setGameId] = useState<number | null>(null)
  const [addingKhinsiderFor, setAddingKhinsiderFor] = useState<number | null>(null)
  const [khinsiderDraft, setKhinsiderDraft] = useState('')
  const [addingYoutubeFor, setAddingYoutubeFor] = useState<number | null>(null)
  const [youtubeDraft, setYoutubeDraft] = useState('')

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

  // La promesse rejetée laisse le champ d'ajout ouvert pour corriger le nom.
  function addFranchise(name: string) {
    return createFranchise(name)
      .then((created) => {
        notify(`Franchise « ${created.name} » ajoutée.`, 'success')
        setFranchiseId(created.id)
        setGameId(null)
        return loadAll()
      })
      .catch((err: Error) => {
        notify(err.message, 'error')
        throw err
      })
  }

  function addGame(name: string, toFranchiseId: number) {
    return createGame(name, toFranchiseId)
      .then((created) => {
        notify(`Jeu « ${created.name} » ajouté.`, 'success')
        setGameId(created.id)
        return loadAll()
      })
      .catch((err: Error) => {
        notify(err.message, 'error')
        throw err
      })
  }

  function addTrack(name: string, toGameId: number) {
    return createTrack(name, toGameId)
      .then((created) => {
        notify(`Musique « ${created.name} » ajoutée.`, 'success')
        return loadAll()
      })
      .catch((err: Error) => {
        notify(err.message, 'error')
        throw err
      })
  }

  function submitKhinsiderLink(event: FormEvent, trackId: number) {
    event.preventDefault()
    const link = khinsiderDraft.trim()
    if (!link) return
    addKhinsiderLink(trackId, link)
      .then((track) => {
        setKhinsiderDraft('')
        setAddingKhinsiderFor(null)
        if (track.audioLink) {
          notify(`Lien KHInsider ajouté à « ${track.name} ».`, 'success')
        } else {
          notify(
            `Lien KHInsider ajouté à « ${track.name} », mais la musique n'est pas encore jouable : nouvelle tentative à la prochaine passe planifiée.`,
            'warning',
          )
        }
        return loadAll()
      })
      .catch((err: Error) => notify(err.message, 'error'))
  }

  function submitYoutubeLink(event: FormEvent, trackId: number) {
    event.preventDefault()
    const link = youtubeDraft.trim()
    if (!link) return
    addYoutubeLink(trackId, link)
      .then((track) => {
        setYoutubeDraft('')
        setAddingYoutubeFor(null)
        notify(`Lien YouTube ajouté à « ${track.name} ».`, 'success')
        return loadAll()
      })
      .catch((err: Error) => notify(err.message, 'error'))
  }

  if (error) {
    return <p role="alert">Impossible de charger les données.</p>
  }

  // Les compteurs portent sur toute la base, pas sur ce que les filtres laissent passer.
  function franchiseDetail(franchise: Franchise): string {
    const franchiseGames = games.filter((game) => game.franchiseId === franchise.id)
    const trackCount = tracks.filter((track) => franchiseGames.some((game) => game.id === track.gameId)).length
    return `${plural(franchiseGames.length, 'jeu', 'jeux')} · ${plural(trackCount, 'musique', 'musiques')}`
  }

  const selected = shown.find((entry) => entry.franchise.id === franchiseId)
  const game = selected?.games.find((item) => item.id === gameId)
  const gameTracks = game
    ? tracks.filter((track) => track.gameId === game.id && (!onlyTracksWithoutLinks || !hasSource(track)))
    : null

  return (
    <>
      <h1>Enrichissement</h1>

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

      <div className="entity-columns enrichment-columns">
        <EntityList
          title="Franchises"
          items={franchises === null ? null : shown.map((entry) => entry.franchise)}
          label={(item) => item.name}
          detail={franchiseDetail}
          activeId={selected?.franchise.id}
          onOpen={(item) => {
            setFranchiseId(item.id)
            setGameId(null)
          }}
          addLabel="Ajouter une franchise"
          onAdd={addFranchise}
          emptyText="Aucune franchise ne correspond."
          filterable={false}
        />
        {selected ? (
          <EntityList
            key={`jeux-${selected.franchise.id}`}
            title={`Jeux de ${selected.franchise.name}`}
            items={selected.games}
            label={(item) => item.name}
            detail={(item) => plural(tracks.filter((track) => track.gameId === item.id).length, 'musique', 'musiques')}
            activeId={game?.id}
            onOpen={(item) => setGameId(item.id)}
            addLabel="Ajouter un jeu"
            onAdd={(name) => addGame(name, selected.franchise.id)}
            emptyText="Aucun jeu ne correspond dans cette franchise."
            filterable={false}
          />
        ) : (
          <p className="entity-muted entity-column-hint">Choisir une franchise pour voir ses jeux.</p>
        )}
        {game && gameTracks ? (
          <EntityList
            key={`musiques-${game.id}`}
            title={`Musiques de ${game.name}`}
            items={gameTracks}
            label={(item) => item.name}
            extra={(track) => (
              <>
                <TrackLinkControl
                  track={track}
                  kind="khinsider"
                  addingFor={addingKhinsiderFor}
                  draft={khinsiderDraft}
                  onStartAdding={() => setAddingKhinsiderFor(track.id)}
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
                  onStartAdding={() => setAddingYoutubeFor(track.id)}
                  onDraftChange={setYoutubeDraft}
                  onCancel={() => {
                    setAddingYoutubeFor(null)
                    setYoutubeDraft('')
                  }}
                  onSubmit={(event) => submitYoutubeLink(event, track.id)}
                />
              </>
            )}
            addLabel="Ajouter une musique"
            onAdd={(name) => addTrack(name, game.id)}
            emptyText={onlyTracksWithoutLinks ? 'Toutes les musiques de ce jeu ont un lien.' : 'Aucune musique dans ce jeu.'}
            filterable={false}
          />
        ) : (
          <p className="entity-muted entity-column-hint">Choisir un jeu pour voir ses musiques.</p>
        )}
      </div>
    </>
  )
}
