import { useEffect, useState, type FormEvent } from 'react'
import {
  fetchCoverCandidates,
  fetchGameCover,
  removeGameCover,
  saveGameCover,
  type AlbumImages,
  type GameCover,
} from '../../api/admin'
import { fetchGames, type Game } from '../../api/games'
import { NamePicker } from '../../components/NamePicker/NamePicker'
import { useNotification } from '../../context/NotificationContext'
import { CoverThumbnail } from './CoverThumbnail'
import { useAdminErrorHandler } from './useAdminErrorHandler'

type Props = {
  token: string
  onSessionExpired: () => void
  selected: Game | null
  onSelect: (game: Game | null) => void
  onCoverChanged: () => void
}

export function GameCoverSection({ token, onSessionExpired, selected, onSelect, onCoverChanged }: Props) {
  const { notify } = useNotification()
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [games, setGames] = useState<Game[]>([])
  const [cover, setCover] = useState<{ gameId: number; value: GameCover } | null>(null)
  const [albums, setAlbums] = useState<{ gameId: number; value: AlbumImages[] } | null>(null)
  const [searching, setSearching] = useState(false)
  const current = selected && cover?.gameId === selected.id ? cover.value : null
  const found = selected && albums?.gameId === selected.id ? albums.value : null

  useEffect(() => {
    const controller = new AbortController()
    fetchGames(controller.signal)
      .then(setGames)
      .catch(() => {
        if (!controller.signal.aborted) notify('Impossible de charger les jeux.', 'error')
      })
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!selected) return
    fetchGameCover(token, selected.id)
      .then((value) => setCover({ gameId: selected.id, value }))
      .catch(handleError)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected])

  function search(game: Game, rescan: boolean) {
    setSearching(true)
    fetchCoverCandidates(token, game.id, rescan)
      .then((value) => setAlbums({ gameId: game.id, value }))
      .catch(handleError)
      .finally(() => setSearching(false))
  }

  function applyUpdate(game: Game, value: GameCover, message: string) {
    setCover({ gameId: game.id, value })
    onCoverChanged()
    notify(message, 'success')
  }

  function save(game: Game, coverUrl: string) {
    saveGameCover(token, game.id, coverUrl)
      .then((value) =>
        applyUpdate(
          game,
          value,
          value.downloaded
            ? `Image de « ${game.name} » enregistrée et téléchargée.`
            : `Image de « ${game.name} » enregistrée, mais pas téléchargée : l'adresse d'origine sera utilisée.`,
        ),
      )
      .catch(handleError)
  }

  function remove(game: Game) {
    if (!window.confirm(`Retirer l'image de « ${game.name} » ?`)) return
    removeGameCover(token, game.id)
      .then((value) => applyUpdate(game, value, `Image de « ${game.name} » retirée.`))
      .catch(handleError)
  }

  return (
    <section className="admin-section" id="image-d-un-jeu">
      <h2>Image d'un jeu</h2>
      <NamePicker
        items={games}
        selected={selected}
        onSelect={onSelect}
        onClear={() => onSelect(null)}
        placeholder="Chercher un jeu"
        renderLabel={(game) => `${game.name} (${game.franchiseName})`}
        getSearchText={(game) => `${game.name} ${game.franchiseName}`}
      />
      {selected && current && (
        <>
          <div className="admin-row">
            {current.displayUrl ? (
              <img className="admin-cover-preview" src={current.displayUrl} alt={`Image actuelle de ${selected.name}`} />
            ) : (
              <span className="muted">Pas encore d'image.</span>
            )}
            {current.coverUrl && (
              <span className="muted">
                {current.downloaded ? 'Téléchargée sur le serveur.' : "Pas téléchargée : l'adresse d'origine est utilisée."}
              </span>
            )}
          </div>
          <CoverUrlForm
            key={`${selected.id}-${current.coverUrl ?? ''}`}
            current={current.coverUrl}
            onSave={(url) => save(selected, url)}
            onRemove={() => remove(selected)}
          />
          <div className="admin-row">
            <button type="button" onClick={() => search(selected, false)} disabled={searching}>
              {searching ? 'Recherche des pochettes…' : 'Voir les pochettes de ses albums'}
            </button>
            {found && found.length > 0 && (
              <button type="button" onClick={() => search(selected, true)} disabled={searching}>
                Rechercher à nouveau sur le CDN
              </button>
            )}
          </div>
          {found && found.length === 0 && <p className="muted">Aucune musique jouable de ce jeu ne vient de KHInsider.</p>}
          {found?.map((album) => (
            <div key={album.albumUrl} className="admin-cover-album">
              <a href={album.albumUrl} target="_blank" rel="noreferrer">
                {album.albumUrl.slice(album.albumUrl.lastIndexOf('/') + 1)}
              </a>
              <span className="muted">
                {album.fromPage ? 'Pochettes de la page KHInsider enregistrée' : 'Pochettes trouvées sur le CDN'} · Musiques :{' '}
                {album.tracks.join(', ')}
              </span>
              {album.images.length === 0 && (
                <p className="muted">
                  {album.fromPage
                    ? "La page enregistrée ne contient aucune pochette de cet album."
                    : "Pas de pochette sous les noms habituels : ouvrir l'album pour l'enregistrer avec le script, ou copier l'adresse de la bonne image."}
                </p>
              )}
              <div className="admin-cover-grid">
                {album.images.map((image) => (
                  <button
                    key={image}
                    type="button"
                    aria-pressed={image === current.coverUrl}
                    onClick={() => save(selected, image)}
                  >
                    <CoverThumbnail url={image} alt={decodeURIComponent(image.slice(image.lastIndexOf('/') + 1))} />
                    <span>{image === current.coverUrl ? 'Image actuelle' : 'Choisir'}</span>
                  </button>
                ))}
              </div>
            </div>
          ))}
        </>
      )}
    </section>
  )
}

type CoverUrlFormProps = {
  current: string | null
  onSave: (url: string) => void
  onRemove: () => void
}

function CoverUrlForm({ current, onSave, onRemove }: CoverUrlFormProps) {
  const [draft, setDraft] = useState(current ?? '')
  const trimmed = draft.trim()

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSave(trimmed)
  }

  return (
    <form className="admin-row" onSubmit={handleSubmit}>
      <span className="admin-link-label">Adresse</span>
      <input
        type="url"
        aria-label="Adresse de l'image"
        placeholder="Aucune image"
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
      />
      <button type="submit" disabled={trimmed === '' || trimmed === current}>
        Enregistrer
      </button>
      <button type="button" onClick={onRemove} disabled={current === null}>
        Retirer
      </button>
    </form>
  )
}
