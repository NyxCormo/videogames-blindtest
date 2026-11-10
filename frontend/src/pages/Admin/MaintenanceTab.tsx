import { useState } from 'react'
import type { Track } from '../../api/tracks'
import { AudioLinksSection } from './AudioLinksSection'
import { ReportsSection } from './ReportsSection'
import { TrackLinksSection } from './TrackLinksSection'
import { YoutubeAudioSection } from './YoutubeAudioSection'

type Props = {
  token: string
  onSessionExpired: () => void
}

export function MaintenanceTab({ token, onSessionExpired }: Props) {
  const [selected, setSelected] = useState<Track | null>(null)
  const [reportsVersion, setReportsVersion] = useState(0)

  return (
    <>
      <ReportsSection key={reportsVersion} token={token} onSessionExpired={onSessionExpired} onFixLink={setSelected} />
      <YoutubeAudioSection
        key={`youtube-${reportsVersion}`}
        token={token}
        onSessionExpired={onSessionExpired}
        onFixLink={setSelected}
      />
      <TrackLinksSection
        token={token}
        onSessionExpired={onSessionExpired}
        selected={selected}
        onSelect={setSelected}
        onLinkChanged={() => setReportsVersion((version) => version + 1)}
      />
      <AudioLinksSection token={token} onSessionExpired={onSessionExpired} />
    </>
  )
}
