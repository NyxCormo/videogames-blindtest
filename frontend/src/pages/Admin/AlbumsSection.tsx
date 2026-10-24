import { useEffect, useState } from 'react'
import { fetchAlbums, type AlbumOverview } from '../../api/admin'
import type { Game } from '../../api/games'
import { EntityList } from '../../components/EntityList/EntityList'
import { Select } from '../../components/Select/Select'
import { plural } from '../../plural'
import { CoverThumbnail } from '../../components/CoverThumbnail/CoverThumbnail'
import { useAdminErrorHandler } from './useAdminErrorHandler'

type AlbumFilter = 'all' | 'without' | 'never' | 'with' | 'page'

const FILTERS: { value: AlbumFilter; label: string }[] = [
  { value: 'all', label: 'Tous les albums' },
  { value: 'without', label: 'Albums sans pochette trouvée' },
  { value: 'never', label: 'Albums jamais recherchés' },
  { value: 'with', label: 'Albums avec pochette' },
  { value: 'page', label: 'Albums dont la page est enregistrée' },
]

type Row = AlbumOverview & { id: number }

type Props = {
  token: string
  onSessionExpired: () => void
  onChooseCover: (game: Game) => void
}

function formatDate(value: string): string {
  return new Date(value).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
}

function searched(album: AlbumOverview): boolean {
  return album.scannedAt !== null || album.pageSavedAt !== null
}

function state(album: AlbumOverview): string {
  if (!searched(album)) return 'jamais recherché'
  const found = album.images.length === 0 ? 'sans pochette' : plural(album.images.length, 'pochette', 'pochettes')
  return album.pageSavedAt ? `${found} · page` : found
}

function matches(album: AlbumOverview, filter: AlbumFilter): boolean {
  if (filter === 'never') return !searched(album)
  if (filter === 'without') return searched(album) && album.images.length === 0
  if (filter === 'with') return album.images.length > 0
  if (filter === 'page') return album.pageSavedAt !== null
  return true
}

export function AlbumsSection({ token, onSessionExpired, onChooseCover }: Props) {
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [albums, setAlbums] = useState<Row[] | null>(null)
  const [filter, setFilter] = useState<AlbumFilter>('all')
  const [albumId, setAlbumId] = useState<number | null>(null)

  function load() {
    fetchAlbums(token)
      .then((loaded) => setAlbums(loaded.map((album, index) => ({ ...album, id: index }))))
      .catch(handleError)
  }

  useEffect(() => {
    load()
    window.addEventListener('focus', load)
    return () => window.removeEventListener('focus', load)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const shown = albums && albums.filter((album) => matches(album, filter))
  const album = shown?.find((item) => item.id === albumId)

  return (
    <section className="admin-section">
      <h2>Albums</h2>
      <p className="muted">
        Chaque album dont viennent des musiques jouables. Ouvrir une page d'album avec le script
        tools/khinsider-album.user.js envoie ses vraies pochettes au serveur ; la liste se met à jour en revenant ici.
      </p>
      <div className="admin-row">
        <Select ariaLabel="Filtrer les albums" options={FILTERS} value={filter} onChange={setFilter} />
      </div>
      <div className="entity-columns">
        <EntityList
          key={`albums-${filter}`}
          title="Albums"
          items={shown}
          label={(item) => item.name}
          detail={state}
          activeId={albumId}
          onOpen={(item) => setAlbumId(item.id)}
          sorted={false}
          emptyText="Aucun album pour ce filtre."
        />
        {!album ? (
          <p className="muted entity-column-hint">Choisir un album pour voir ses jeux et ses pochettes.</p>
        ) : (
          <div className="entity-list admin-album-panel">
            <h3>{album.name}</h3>
            {album.pageUrl ? (
              <a href={album.pageUrl} target="_blank" rel="noreferrer">
                Ouvrir la page de l'album sur KHInsider
              </a>
            ) : (
              <span className="muted">Pas de lien KHInsider pour cet album.</span>
            )}
            <span className="muted">
              {album.pageSavedAt
                ? `Page KHInsider enregistrée le ${formatDate(album.pageSavedAt)}`
                : album.scannedAt
                  ? `Recherché sur le CDN le ${formatDate(album.scannedAt)}`
                  : 'Jamais recherché'}{' '}
              ·{' '}
              {plural(album.tracks.length, 'musique', 'musiques')} : {album.tracks.join(', ')}
            </span>
            <div className="admin-row">
              {album.games.map((game) => (
                <button key={game.id} type="button" onClick={() => onChooseCover(game)}>
                  Image de {game.name}
                </button>
              ))}
            </div>
            {album.images.length > 0 && (
              <div className="admin-cover-grid">
                {album.images.map((image) => (
                  <CoverThumbnail key={image} url={image} alt={decodeURIComponent(image.slice(image.lastIndexOf('/') + 1))} />
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  )
}
