import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { createBlindtest } from '../../api/blindtests'
import { fetchAllTags, type Tag } from '../../api/tags'
import './BlindtestCreatePage.css'

export function BlindtestCreatePage() {
  const [name, setName] = useState('')
  const [trackCount, setTrackCount] = useState(10)
  const [difficulty, setDifficulty] = useState(50)
  const [allTags, setAllTags] = useState<Tag[]>([])
  const [selectedTagIds, setSelectedTagIds] = useState<Set<number>>(new Set())
  const [matchAllTags, setMatchAllTags] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    const controller = new AbortController()
    fetchAllTags(controller.signal)
      .then(setAllTags)
      .catch(() => {})
    return () => controller.abort()
  }, [])

  function toggleTag(id: number) {
    setSelectedTagIds((current) => {
      const next = new Set(current)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    createBlindtest(name, trackCount, difficulty, [...selectedTagIds], matchAllTags)
      .then(() => navigate('/blindtests'))
      .catch((err: Error) => setError(err.message))
      .finally(() => setSubmitting(false))
  }

  const tagsByType = new Map<string, Tag[]>()
  for (const tag of allTags) {
    const group = tagsByType.get(tag.typeName) ?? []
    group.push(tag)
    tagsByType.set(tag.typeName, group)
  }

  return (
    <>
      <h1>Créer un blindtest</h1>
      <form onSubmit={handleSubmit} className="blindtest-create">
        <label>
          Nom
          <input
            type="text"
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
          />
        </label>
        <label>
          Nombre de musiques
          <input
            type="number"
            min={1}
            value={trackCount}
            onChange={(event) => setTrackCount(Number(event.target.value))}
            required
          />
        </label>
        <label>
          Difficulté ({difficulty})
          <input
            type="range"
            min={0}
            max={100}
            value={difficulty}
            onChange={(event) => setDifficulty(Number(event.target.value))}
          />
        </label>

        {tagsByType.size > 0 && (
          <fieldset className="blindtest-tags">
            <legend>Tags (optionnel)</legend>
            {[...tagsByType.entries()].map(([typeName, tagsOfType]) => (
              <div key={typeName} className="tag-type-group">
                <h3>{typeName}</h3>
                {tagsOfType.map((tag) => (
                  <label key={tag.id} className="tag-checkbox">
                    <input
                      type="checkbox"
                      checked={selectedTagIds.has(tag.id)}
                      onChange={() => toggleTag(tag.id)}
                    />
                    {tag.name}
                  </label>
                ))}
              </div>
            ))}
            {selectedTagIds.size > 0 && (
              <button type="button" onClick={() => setMatchAllTags((current) => !current)}>
                Combinaison : {matchAllTags ? 'ET (toutes les tags)' : 'OU (au moins une)'}
              </button>
            )}
          </fieldset>
        )}

        {error && <p role="alert">{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Création...' : 'Créer'}
        </button>
      </form>
    </>
  )
}
