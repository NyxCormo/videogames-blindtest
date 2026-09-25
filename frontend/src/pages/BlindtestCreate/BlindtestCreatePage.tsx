import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { createBlindtest, type DifficultyBand } from '../../api/blindtests'
import { fetchAllTags, type Tag } from '../../api/tags'
import { DifficultyRangeSlider } from '../../components/DifficultyRangeSlider/DifficultyRangeSlider'
import './BlindtestCreatePage.css'

const STRATEGIES = [
  { value: '', label: 'Automatique (aléatoire, puis jeux rares, puis franchises rares)' },
  { value: 'random', label: 'Aléatoire' },
  { value: 'rareGames', label: 'Favoriser les jeux rares' },
  { value: 'rareFranchises', label: 'Favoriser les franchises rares' },
]

export function BlindtestCreatePage() {
  const [name, setName] = useState('')
  const [trackCount, setTrackCount] = useState(10)
  const [bands, setBands] = useState<DifficultyBand[]>([{ minDifficulty: 0, maxDifficulty: 100, proportion: 100 }])
  const [maxPerGame, setMaxPerGame] = useState<number | ''>('')
  const [maxPerFranchise, setMaxPerFranchise] = useState<number | ''>('')
  const [strategy, setStrategy] = useState('')
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

  function addBand() {
    setBands((current) => [...current, { minDifficulty: 0, maxDifficulty: 100, proportion: 0 }])
  }

  function removeBand(index: number) {
    setBands((current) => current.filter((_, i) => i !== index))
  }

  function updateBand(index: number, field: keyof DifficultyBand, value: number) {
    setBands((current) => current.map((band, i) => (i === index ? { ...band, [field]: value } : band)))
  }

  function updateBandRange(index: number, minDifficulty: number, maxDifficulty: number) {
    setBands((current) =>
      current.map((band, i) => (i === index ? { ...band, minDifficulty, maxDifficulty } : band)),
    )
  }

  const proportionTotal = bands.reduce((total, band) => total + band.proportion, 0)

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    createBlindtest(
      name,
      trackCount,
      bands,
      [...selectedTagIds],
      matchAllTags,
      maxPerGame === '' ? null : maxPerGame,
      maxPerFranchise === '' ? null : maxPerFranchise,
      strategy === '' ? null : strategy,
    )
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

        <fieldset className="blindtest-bands">
          <legend>Paliers de difficulté</legend>
          <p className="bands-hint">
            0 = musiques les plus connues, 100 = les moins connues. Chaque palier prend une part du nombre de musiques.
          </p>
          {bands.map((band, index) => (
            <div key={index} className="band-row">
              <div className="band-slider-wrapper">
                <span className="band-slider-value">{band.minDifficulty}</span>
                <DifficultyRangeSlider
                  min={band.minDifficulty}
                  max={band.maxDifficulty}
                  onChange={(minDifficulty, maxDifficulty) => updateBandRange(index, minDifficulty, maxDifficulty)}
                />
                <span className="band-slider-value">{band.maxDifficulty}</span>
              </div>
              <label>
                Proportion (%)
                <input
                  type="number"
                  min={0}
                  max={100}
                  value={band.proportion}
                  onChange={(event) => updateBand(index, 'proportion', Number(event.target.value))}
                />
              </label>
              {bands.length > 1 && (
                <button type="button" onClick={() => removeBand(index)}>
                  Retirer
                </button>
              )}
            </div>
          ))}
          <button type="button" onClick={addBand}>
            + Ajouter un palier
          </button>
          <p className={proportionTotal === 100 ? 'band-total-ok' : 'band-total-error'}>
            Total : {proportionTotal} % {proportionTotal !== 100 && '(doit faire 100)'}
          </p>
        </fieldset>

        <label>
          Max de musiques par jeu (optionnel)
          <input
            type="number"
            min={1}
            value={maxPerGame}
            onChange={(event) => setMaxPerGame(event.target.value === '' ? '' : Number(event.target.value))}
          />
        </label>
        <label>
          Max de musiques par franchise (optionnel)
          <input
            type="number"
            min={1}
            value={maxPerFranchise}
            onChange={(event) => setMaxPerFranchise(event.target.value === '' ? '' : Number(event.target.value))}
          />
        </label>
        <label>
          Stratégie de sélection
          <select value={strategy} onChange={(event) => setStrategy(event.target.value)}>
            {STRATEGIES.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
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
        <button type="submit" disabled={submitting || proportionTotal !== 100}>
          {submitting ? 'Création...' : 'Créer'}
        </button>
      </form>
    </>
  )
}
