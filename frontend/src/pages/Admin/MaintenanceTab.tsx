import { AudioLinksSection } from './AudioLinksSection'
import { TrackLinksSection } from './TrackLinksSection'

type Props = {
  token: string
  onSessionExpired: () => void
}

export function MaintenanceTab({ token, onSessionExpired }: Props) {
  return (
    <>
      <TrackLinksSection token={token} onSessionExpired={onSessionExpired} />
      <AudioLinksSection token={token} onSessionExpired={onSessionExpired} />
    </>
  )
}
