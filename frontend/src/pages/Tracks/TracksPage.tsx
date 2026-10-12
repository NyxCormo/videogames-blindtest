import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router'
import { fetchTracks, type Track } from '../../api/tracks'
import { AudioPlayer } from '../../components/AudioPlayer/AudioPlayer'
import { Tabs } from '../../components/Tabs/Tabs'
import { EnrichTab } from './Enrich/EnrichTab'
import { ListenTab } from './Listen/ListenTab'
import { sortTracks } from './Listen/sortTracks'
import { usePlayer } from './usePlayer'
import { VotesTab } from './Votes/VotesTab'
import './TracksPage.css'

const TABS = [
  { id: 'ecouter', label: 'Écouter' },
  { id: 'enrichir', label: 'Enrichir' },
  { id: 'votes', label: 'Mes votes' },
]

export function TracksPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const tab = TABS.some((item) => item.id === searchParams.get('tab')) ? searchParams.get('tab')! : TABS[0].id
  const [tracks, setTracks] = useState<Track[] | null>(null)
  const [error, setError] = useState(false)
  const audioRef = useRef<HTMLAudioElement>(null)
  const player = usePlayer(tracks, audioRef)

  // Rechargée à chaque retour sur l'onglet : une musique ajoutée dans « Enrichir » y apparaît.
  useEffect(() => {
    if (tab !== 'ecouter') return
    const controller = new AbortController()
    fetchTracks(controller.signal)
      .then((list) => {
        setTracks(sortTracks(list))
        setError(false)
      })
      .catch(() => {
        // Un rechargement annule la requête en cours (ce n'est pas une erreur)
        if (!controller.signal.aborted) {
          setError(true)
        }
      })
    return () => controller.abort()
  }, [tab])

  return (
    <>
      <h1>Musiques</h1>
      <Tabs tabs={TABS} active={tab} onChange={(id) => setSearchParams(id === TABS[0].id ? {} : { tab: id })} />
      {tab === 'ecouter' && <ListenTab tracks={tracks} error={error} player={player} />}
      {tab === 'enrichir' && <EnrichTab />}
      {tab === 'votes' && <VotesTab />}
      <AudioPlayer
        ref={audioRef}
        track={player.currentTrack}
        onPlayingChange={player.setIsPlaying}
        onNext={player.next}
        onPrevious={player.previous}
        canGoBack={player.canGoBack}
      />
    </>
  )
}
