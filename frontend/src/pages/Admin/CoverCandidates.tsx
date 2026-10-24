import { useEffect, useState } from 'react'
import { fetchCoverCandidates, type AlbumImages } from '../../api/admin'
import type { Game } from '../../api/games'
import { CoverThumbnail } from '../../components/CoverThumbnail/CoverThumbnail'
import { useAdminErrorHandler } from './useAdminErrorHandler'

type Props = {
  token: string
  onSessionExpired: () => void
  game: Game
  currentUrl: string | null
  onChoose: (image: string) => void
}

// Pochettes des albums d'un jeu, chargées dès que le jeu est choisi
export function CoverCandidates({ token, onSessionExpired, game, currentUrl, onChoose }: Props) {
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [albums, setAlbums] = useState<{ gameId: number; value: AlbumImages[] } | null>(null)
  const [searching, setSearching] = useState(false)
  const found = albums?.gameId === game.id ? albums.value : null

  function search(rescan: boolean) {
    setSearching(true)
    fetchCoverCandidates(token, game.id, rescan)
      .then((value) => setAlbums({ gameId: game.id, value }))
      .catch(handleError)
      .finally(() => setSearching(false))
  }

  useEffect(() => {
    search(false)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [game.id])

  if (!found) {
    return <p className="muted">{searching ? 'Recherche des pochettes…' : ''}</p>
  }
  return (
    <div className="admin-cover-candidates">
      {found.length === 0 && <p className="muted">Aucune musique jouable de ce jeu ne vient de KHInsider.</p>}
      {found.map((album) => (
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
                ? 'La page enregistrée ne contient aucune pochette de cet album.'
                : "Pas de pochette sous les noms habituels : ouvrir l'album pour l'enregistrer avec le script, ou copier l'adresse de la bonne image."}
            </p>
          )}
          <div className="admin-cover-grid">
            {album.images.map((image) => (
              <button key={image} type="button" aria-pressed={image === currentUrl} onClick={() => onChoose(image)}>
                <CoverThumbnail url={image} alt={decodeURIComponent(image.slice(image.lastIndexOf('/') + 1))} />
                <span>{image === currentUrl ? 'Image actuelle' : 'Choisir'}</span>
              </button>
            ))}
          </div>
        </div>
      ))}
      {found.length > 0 && (
        <div className="admin-row">
          <button type="button" onClick={() => search(true)} disabled={searching}>
            {searching ? 'Recherche des pochettes…' : 'Rechercher à nouveau sur le CDN'}
          </button>
        </div>
      )}
    </div>
  )
}
