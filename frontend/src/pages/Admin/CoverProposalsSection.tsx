import { useEffect, useState } from 'react'
import { acceptCoverProposal, fetchCoverProposals, rejectCoverProposal, type CoverProposal } from '../../api/admin'
import type { Game } from '../../api/games'
import { useNotification } from '../../context/NotificationContext'
import { CoverThumbnail } from './CoverThumbnail'
import { useAdminErrorHandler } from './useAdminErrorHandler'

type Props = {
  token: string
  onSessionExpired: () => void
  onChooseCover: (game: Game) => void
  onCoverChanged: () => void
}

function formatDate(value: string): string {
  return new Date(value).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
}

function sameProposal(a: CoverProposal, b: CoverProposal): boolean {
  return a.game.id === b.game.id && a.listenerId === b.listenerId
}

export function CoverProposalsSection({ token, onSessionExpired, onChooseCover, onCoverChanged }: Props) {
  const { notify } = useNotification()
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [proposals, setProposals] = useState<CoverProposal[] | null>(null)

  function load() {
    fetchCoverProposals(token).then(setProposals).catch(handleError)
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(load, [])

  function accept(proposal: CoverProposal) {
    acceptCoverProposal(token, proposal)
      .then(() => {
        notify(`Nouvelle image de « ${proposal.game.name} » enregistrée.`, 'success')
        onCoverChanged()
        load()
      })
      .catch(handleError)
  }

  function reject(proposal: CoverProposal) {
    rejectCoverProposal(token, proposal)
      .then(() => {
        setProposals((current) => current && current.filter((other) => !sameProposal(other, proposal)))
        notify(`Proposition de ${proposal.listenerName} refusée.`, 'success')
      })
      .catch(handleError)
  }

  return (
    <section className="admin-section">
      <h2>Propositions des joueurs</h2>
      {proposals === null && <p className="muted">Chargement…</p>}
      {proposals !== null && proposals.length === 0 && <p className="muted">Aucune proposition en attente.</p>}
      {proposals !== null && proposals.length > 0 && (
        <ul className="admin-reports">
          {proposals.map((proposal) => (
            <li key={`${proposal.game.id}-${proposal.listenerId}`}>
              <span>
                <strong>{proposal.game.name}</strong> ({proposal.game.franchiseName})
              </span>
              <span className="muted">
                {proposal.imageUrl ? 'Image proposée' : 'Image actuelle signalée'} par {proposal.listenerName} le{' '}
                {formatDate(proposal.createdAt)}
                {!proposal.imageUrl && ', sans nouvelle image'}
              </span>
              <div className="admin-cover-compare">
                <figure>
                  {proposal.currentUrl ? (
                    <img src={proposal.currentUrl} alt={`Image actuelle de ${proposal.game.name}`} />
                  ) : (
                    <span className="admin-cover-empty">Pas d'image</span>
                  )}
                  <figcaption>Avant</figcaption>
                </figure>
                {proposal.imageUrl && (
                  <figure>
                    <CoverThumbnail url={proposal.imageUrl} alt={`Image proposée pour ${proposal.game.name}`} />
                    <figcaption>Après</figcaption>
                  </figure>
                )}
              </div>
              <div className="admin-row">
                {proposal.imageUrl ? (
                  <button type="button" onClick={() => accept(proposal)}>
                    Accepter
                  </button>
                ) : (
                  <button type="button" onClick={() => onChooseCover(proposal.game)}>
                    Choisir une image moi-même
                  </button>
                )}
                <button type="button" onClick={() => reject(proposal)}>
                  Refuser
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
