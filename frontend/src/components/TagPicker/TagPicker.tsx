import { useEffect, useState } from 'react'
import { createTag, fetchTagTypes, searchTags, type Tag, type TagType } from '../../api/tags'
import { Select } from '../Select/Select'
import './TagPicker.css'

type Props = {
    onPick: (tag: Tag) => void
}

export function TagPicker({ onPick }: Props) {
    const [query, setQuery] = useState('')
    const [results, setResults] = useState<Tag[]>([])
    const [tagTypes, setTagTypes] = useState<TagType[]>([])
    const [typeId, setTypeId] = useState('')
    const [creating, setCreating] = useState(false)
    const [createError, setCreateError] = useState<string | null>(null)

    useEffect(() => {
        const controller = new AbortController()
        fetchTagTypes(controller.signal)
            .then(setTagTypes)
            .catch(() => {})
        return () => controller.abort()
    }, [])

    useEffect(() => {
        const trimmed = query.trim()
        if (trimmed.length < 2) {
            setResults([])
            return
        }
        const controller = new AbortController()
        const timeout = setTimeout(() => {
            searchTags(trimmed, controller.signal)
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

    function select(tag: Tag) {
        onPick(tag)
        setQuery('')
        setResults([])
        setTypeId('')
        setCreateError(null)
    }

    function handleCreate() {
        if (typeId === '') {
            setCreateError('Le type est obligatoire pour créer un tag')
            return
        }
        setCreateError(null)
        setCreating(true)
        createTag(Number(typeId), query.trim())
            .then(select)
            .catch((err: Error) => setCreateError(err.message))
            .finally(() => setCreating(false))
    }

    const selectedTypeName = tagTypes.find((type) => String(type.id) === typeId)?.name
    const alreadyExists = results.some(
        (tag) => tag.name.toLowerCase() === query.trim().toLowerCase() && tag.typeName === selectedTypeName,
    )

    return (
        <div className="tag-picker">
            <input
                type="search"
                placeholder="Chercher un tag"
                aria-label="Chercher un tag"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
            />
            {results.length > 0 && (
                <ul className="results-list">
                    {results.map((tag) => (
                        <li key={tag.id}>
                            <button type="button" onClick={() => select(tag)}>
                                {tag.name} <span className="tag-type">({tag.typeName})</span>
                            </button>
                        </li>
                    ))}
                </ul>
            )}
            {query.trim().length >= 2 && (
                <div className="tag-picker-create">
                    <Select
                        ariaLabel="Type du nouveau tag"
                        options={[
                            { value: '', label: 'Choisir un type' },
                            ...tagTypes.map((type) => ({ value: String(type.id), label: type.name })),
                        ]}
                        value={typeId}
                        onChange={setTypeId}
                    />
                    {!alreadyExists && (
                        <button type="button" onClick={handleCreate} disabled={creating}>
                            Créer « {query.trim()} »
                        </button>
                    )}
                    {createError && <p role="alert">{createError}</p>}
                </div>
            )}
        </div>
    )
}
