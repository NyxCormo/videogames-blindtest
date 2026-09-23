import { useEffect, useState } from 'react'
import { createListener, searchListeners, type Listener } from '../../api/listeners'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import './ListenerBanner.css'

export function ListenerBanner() {
  const { listener, connect, disconnect } = useCurrentListener()
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Listener[]>([])
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    const trimmed = query.trim()
    if (trimmed.length < 2) {
      setResults([])
      return
    }
    const controller = new AbortController()
    const timeout = setTimeout(() => {
      searchListeners(trimmed, controller.signal)
        .then(setResults)
        .catch(() => {
          if (!controller.signal.aborted) setResults([])
        })
    }, 300)
    return () => {
      clearTimeout(timeout)
      controller.abort()
    }
  }, [query])

  function select(selected: Listener) {
    connect(selected)
    setOpen(false)
    setQuery('')
    setResults([])
  }

  function handleCreate() {
    setCreating(true)
    createListener(query.trim())
      .then(select)
      .finally(() => setCreating(false))
  }

  if (listener) {
    return (
      <div className="listener-banner">
        <span>Connecté : {listener.name}</span>
        <button type="button" onClick={disconnect}>Se déconnecter</button>
      </div>
    )
  }

  if (!open) {
    return (
      <div className="listener-banner">
        <button type="button" onClick={() => setOpen(true)}>Se connecter</button>
      </div>
    )
  }

  return (
    <div className="listener-banner">
      <input
        type="search"
        placeholder="Ton pseudo"
        aria-label="Rechercher son pseudo"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        autoFocus
      />
      <button type="button" onClick={() => setOpen(false)}>Annuler</button>
      {results.length > 0 && (
        <ul className="listener-results">
          {results.map((result) => (
            <li key={result.id}>
              <button type="button" onClick={() => select(result)}>{result.name}</button>
            </li>
          ))}
        </ul>
      )}
      {query.trim().length >= 2 && (
        <button type="button" onClick={handleCreate} disabled={creating}>
          Créer « {query.trim()} »
        </button>
      )}
    </div>
  )
}
