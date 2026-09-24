import { MemoryRouter } from 'react-router'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { Track } from '../../api/tracks'
import { TrackTable } from './TrackTable'

const dawn: Track = {
  id: 1,
  name: 'Dawn',
  gameName: 'Stellar Blade',
  franchiseName: 'Stellar Blade',
  khinsiderLink: 'https://downloads.khinsider.com/game-soundtracks/album/stellar-blade-soundtrack-2024/62.%2520Dawn.mp3',
  youtubeLink: null,
  audioLink: 'https://jetta.vgmtreasurechest.com/soundtracks/stellar-blade-soundtrack-2024/ybomulwy/62.%20Dawn.mp3',
}
const raven: Track = { ...dawn, id: 2, name: 'Raven', khinsiderLink: null, audioLink: null }

describe('TrackTable', () => {
  it('affiche une ligne par musique', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter>
        <TrackTable tracks={[dawn, raven]} onPlay={() => {}} />
      </MemoryRouter>,
    )

    expect(html.match(/<tr/g)).toHaveLength(3) // l'en-tête et deux musiques
    expect(html).toContain('Dawn')
    expect(html).toContain('Raven')
  })

  it('affiche un lien externe sûr quand la musique a une source', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter>
        <TrackTable tracks={[dawn]} onPlay={() => {}} />
      </MemoryRouter>,
    )

    expect(html).toContain(`href="${dawn.khinsiderLink}"`)
    expect(html).toContain('target="_blank"')
    expect(html).toContain('rel="noreferrer"')
    expect(html).not.toContain('Aucune')
  })

  it('affiche « Aucune » quand la musique n’a pas de source', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter>
        <TrackTable tracks={[raven]} onPlay={() => {}} />
      </MemoryRouter>,
    )

    expect(html).toContain('Aucune')
    expect(html).not.toContain('KHInsider')
  })

  it('affiche un bouton de lecture quand la musique a un lien audio', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter>
        <TrackTable tracks={[dawn]} onPlay={() => {}} />
      </MemoryRouter>,
    )

    expect(html).toContain('aria-label="Écouter Dawn"')
  })

  it('n’affiche pas de bouton de lecture sans lien audio résolu', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter>
        <TrackTable tracks={[raven]} onPlay={() => {}} />
      </MemoryRouter>,
    )

    expect(html).not.toContain('aria-label="Écouter Raven"')
  })

  it('affiche le bouton de pause pour la musique en cours de lecture', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter>
        <TrackTable tracks={[dawn]} onPlay={() => {}} playingTrackId={dawn.id} />
      </MemoryRouter>,
    )

    expect(html).toContain('⏸')
  })
})
