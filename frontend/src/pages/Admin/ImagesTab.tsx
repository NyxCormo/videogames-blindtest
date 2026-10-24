import { useState } from 'react'
import type { Game } from '../../api/games'
import { AlbumsSection } from './AlbumsSection'
import { CoverFillSection } from './CoverFillSection'
import { CoverProposalsSection } from './CoverProposalsSection'
import { GameCoverSection } from './GameCoverSection'
import { GameCoversList } from './GameCoversList'

type Props = {
  token: string
  onSessionExpired: () => void
}

export function ImagesTab({ token, onSessionExpired }: Props) {
  const [selected, setSelected] = useState<Game | null>(null)
  const [proposalsVersion, setProposalsVersion] = useState(0)
  const [coverVersion, setCoverVersion] = useState(0)
  const [listVersion, setListVersion] = useState(0)

  function chooseCover(game: Game) {
    setSelected(game)
    document.getElementById('image-d-un-jeu')?.scrollIntoView({ behavior: 'smooth' })
  }

  return (
    <>
      <CoverProposalsSection
        key={`proposals-${proposalsVersion}-${listVersion}`}
        token={token}
        onSessionExpired={onSessionExpired}
        onChooseCover={chooseCover}
        onCoverChanged={() => setCoverVersion((version) => version + 1)}
      />
      <GameCoversList
        key={`list-${proposalsVersion}-${coverVersion}`}
        token={token}
        onSessionExpired={onSessionExpired}
        onChangeCover={chooseCover}
        onCoverChanged={() => setListVersion((version) => version + 1)}
      />
      <AlbumsSection token={token} onSessionExpired={onSessionExpired} onChooseCover={chooseCover} />
      <GameCoverSection
        key={`cover-${coverVersion}-${listVersion}`}
        token={token}
        onSessionExpired={onSessionExpired}
        selected={selected}
        onSelect={setSelected}
        onCoverChanged={() => setProposalsVersion((version) => version + 1)}
      />
      <CoverFillSection token={token} onSessionExpired={onSessionExpired} />
    </>
  )
}
