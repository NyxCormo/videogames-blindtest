import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { deleteKnowledge, fetchKnowledge, setKnowledge } from '../../../api/discover'
import { fetchTracks, type Track } from '../../../api/tracks'
import { LoginPrompt } from '../../../components/LoginPrompt/LoginPrompt'
import { Select, type SelectOption } from '../../../components/Select/Select'
import { useCurrentListener } from '../../../context/CurrentListenerContext'
import { useNotification } from '../../../context/NotificationContext'
import { plural } from '../../../plural'
import { useProgressiveList } from '../../../useProgressiveList'
import { filterKnowledge, type VoteFilter } from './filterKnowledge'
import './VotesTab.css'

const VOTE_OPTIONS: SelectOption<VoteFilter>[] = [
  { value: 'all', label: 'Tous les votes' },
  { value: 'yes', label: 'Connues' },
  { value: 'no', label: 'Pas connues' },
  { value: 'none', label: 'Pas encore votées' },
]

export function VotesTab() {
  const { listener } = useCurrentListener()
  const { notify } = useNotification()
  const [tracks, setTracks] = useState<Track[] | null>(null)
  const [knowledge, setKnowledgeMap] = useState<Map<number, boolean>>(new Map())
  const [error, setError] = useState(false)
  const [query, setQuery] = useState('')
  const [vote, setVote] = useState<VoteFilter>('all')

  useEffect(() => {
    if (!listener) return
    const controller = new AbortController()
    Promise.all([fetchTracks(controller.signal), fetchKnowledge(listener.id, controller.signal)])
      .then(([allTracks, entries]) => {
        setTracks(allTracks)
        setKnowledgeMap(new Map(entries.map((entry) => [entry.trackId, entry.knows])))
      })
      .catch(() => {
        if (!controller.signal.aborted) setError(true)
      })
    return () => controller.abort()
  }, [listener])

  const shown = useMemo(
    () => (tracks ? filterKnowledge(tracks, query, vote, knowledge) : []),
    [tracks, query, vote, knowledge],
  )

  function updateVote(trackId: number, knows: boolean | undefined) {
    setKnowledgeMap((current) => {
      const next = new Map(current)
      if (knows === undefined) {
        next.delete(trackId)
      } else {
        next.set(trackId, knows)
      }
      return next
    })
  }

  // Recliquer sur le vote déjà en place l'annule (retour à "pas encore votée") au lieu de le répéter.
  function answer(track: Track, knows: boolean) {
    if (!listener) return
    const previous = knowledge.get(track.id)
    const cancelling = previous === knows
    updateVote(track.id, cancelling ? undefined : knows)
    const request = cancelling ? deleteKnowledge(listener.id, track.id) : setKnowledge(listener.id, track.id, knows)
    request.catch(() => {
      // Le vote affiché doit rester celui du serveur.
      updateVote(track.id, previous)
      notify(`Ton vote pour « ${track.name} » n'a pas été enregistré.`, 'error')
    })
  }

  const rows = useProgressiveList(shown, `${query}|${vote}`)
  const answerRef = useRef(answer)
  useEffect(() => {
    answerRef.current = answer
  })
  const stableAnswer = useCallback((track: Track, knows: boolean) => answerRef.current(track, knows), [])

  if (!listener) {
    return <LoginPrompt message="Les votes sont enregistrés par pseudo." />
  }
  if (error) {
    return <p role="alert">Impossible de charger les musiques.</p>
  }
  if (tracks === null) {
    return <p>Chargement...</p>
  }

  return (
    <>
      <div className="controls">
        <input
          type="search"
          placeholder="Rechercher une franchise, un jeu ou une musique"
          aria-label="Rechercher"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        <Select ariaLabel="Filtrer par vote" options={VOTE_OPTIONS} value={vote} onChange={setVote} />
      </div>
      <p className="summary">
        {plural(shown.length, 'musique affichée', 'musiques affichées')} sur {tracks.length}.
      </p>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Franchise</th>
              <th>Jeu</th>
              <th>Musique</th>
              <th>Je connais</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((track) => (
              <VoteRow key={track.id} track={track} knows={knowledge.get(track.id)} onAnswer={stableAnswer} />
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

type VoteRowProps = {
  track: Track
  knows: boolean | undefined
  onAnswer: (track: Track, knows: boolean) => void
}

const VoteRow = memo(function VoteRow({ track, knows, onAnswer }: VoteRowProps) {
  return (
    <tr>
      <td>{track.franchiseName}</td>
      <td>{track.gameName}</td>
      <td>{track.name}</td>
      <td>
        <div className="knowledge-actions">
          <button type="button" aria-pressed={knows === true} onClick={() => onAnswer(track, true)}>
            Oui
          </button>
          <button type="button" aria-pressed={knows === false} onClick={() => onAnswer(track, false)}>
            Non
          </button>
        </div>
      </td>
    </tr>
  )
})
