import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { Track } from '../../api/tracks'
import { TrackTable } from '../TrackTable/TrackTable'

const dawn: Track = {
  id: 1,
  name: 'Dawn',
  gameName: 'Stellar Blade',
  franchiseName: 'Stellar Blade',
  khinsiderLink: 'https://downloads.khinsider.com/game-soundtracks/album/stellar-blade-soundtrack-2024/62.%2520Dawn.mp3',
  youtubeLink: null,
}
const raven: Track = { ...dawn, id: 2, name: 'Raven', khinsiderLink: null }

describe('TrackTable', () => {
  it('affiche une ligne par musique', () => {
    const html = renderToStaticMarkup(<TrackTable tracks={[dawn, raven]} />)

    expect(html.match(/<tr/g)).toHaveLength(3) // l'en-tête et deux musiques
    expect(html).toContain('Dawn')
    expect(html).toContain('Raven')
  })

  it('affiche un lien externe sûr quand la musique a une source', () => {
    const html = renderToStaticMarkup(<TrackTable tracks={[dawn]} />)

    expect(html).toContain(`href="${dawn.khinsiderLink}"`)
    expect(html).toContain('target="_blank"')
    expect(html).toContain('rel="noreferrer"')
    expect(html).not.toContain('Aucune')
  })

  it('affiche « Aucune » quand la musique n’a pas de source', () => {
    const html = renderToStaticMarkup(<TrackTable tracks={[raven]} />)

    expect(html).toContain('Aucune')
    expect(html).not.toContain('KHInsider')
  })
})
