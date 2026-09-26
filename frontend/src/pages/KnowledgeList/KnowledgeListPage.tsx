import { useEffect, useMemo, useState } from 'react'
import { deleteKnowledge, fetchKnowledge, setKnowledge } from '../../api/discover'
import { fetchTracks, type Track } from '../../api/tracks'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import { filterKnowledge, type VoteFilter } from './filterKnowledge'
import './KnowledgeListPage.css'

export function KnowledgeListPage() {
  const { listener } = useCurrentListener()
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

  // Recliquer sur le vote déjà en place l'annule (retour à "pas encore votée") au lieu de le répéter.
  function answer(trackId: number, knows: boolean) {
    if (!listener) return
    if (knowledge.get(trackId) === knows) {
      setKnowledgeMap((previous) => {
        const next = new Map(previous)
        next.delete(trackId)
        return next
      })
      deleteKnowledge(listener.id, trackId).catch(() => {})
    } else {
      setKnowledgeMap((previous) => new Map(previous).set(trackId, knows))
      setKnowledge(listener.id, trackId, knows).catch(() => {})
    }
  }

  if (!listener) {
    return <p>Connecte-toi pour voir et modifier tes connaissances.</p>
  }
  if (error) {
    return <p role="alert">Impossible de charger les musiques.</p>
  }
  if (tracks === null) {
    return <p>Chargement...</p>
  }

  return (
    <>
      <h1>Musiques connues</h1>
      <div className="controls">
        <input
          type="search"
          placeholder="Rechercher une franchise, un jeu ou une musique"
          aria-label="Rechercher"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        <select aria-label="Filtrer par vote" value={vote} onChange={(event) => setVote(event.target.value as VoteFilter)}>
          <option value="all">Tous les votes</option>
          <option value="yes">Connues</option>
          <option value="no">Pas connues</option>
          <option value="none">Pas encore votées</option>
        </select>
      </div>
      <p className="summary">
        {shown.length} {shown.length > 1 ? 'musiques affichées' : 'musique affichée'} sur {tracks.length}.
      </p>
      <div className="knowledge-wrap">
        <table className="knowledge-table">
          <thead>
            <tr>
              <th>Franchise</th>
              <th>Jeu</th>
              <th>Musique</th>
              <th>Je connais</th>
            </tr>
          </thead>
          <tbody>
            {shown.map((track) => {
              const knows = knowledge.get(track.id)
              return (
                <tr key={track.id}>
                  <td>{track.franchiseName}</td>
                  <td>{track.gameName}</td>
                  <td>{track.name}</td>
                  <td>
                    <div className="knowledge-actions">
                      <button type="button" aria-pressed={knows === true} onClick={() => answer(track.id, true)}>
                        Oui
                      </button>
                      <button type="button" aria-pressed={knows === false} onClick={() => answer(track.id, false)}>
                        Non
                      </button>
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </>
  )
}
