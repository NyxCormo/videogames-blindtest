import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import type { Tag } from '../../api/tags'
import { addTrackTag, fetchTrack, fetchTrackTags, removeTrackTag, type Track } from '../../api/tracks'
import { TagPicker } from '../../components/TagPicker/TagPicker'
import './TrackDetailPage.css'

export function TrackDetailPage() {
    const { id } = useParams()
    const trackId = Number(id)

    const [track, setTrack] = useState<Track | null>(null)
    const [tags, setTags] = useState<Tag[] | null>(null)
    const [error, setError] = useState(false)

    useEffect(() => {
        const controller = new AbortController()
        Promise.all([fetchTrack(trackId, controller.signal), fetchTrackTags(trackId, controller.signal)])
            .then(([fetchedTrack, fetchedTags]) => {
                setTrack(fetchedTrack)
                setTags(fetchedTags)
            })
            .catch(() => {
                // Un rechargement annule la requête en cours (ce n'est pas une erreur)
                if (!controller.signal.aborted) {
                    setError(true)
                }
            })
        return () => controller.abort()
    }, [trackId])

    function handleAdd(tag: Tag) {
        addTrackTag(trackId, tag.id).then(() => {
            setTags((current) => (current?.some((existing) => existing.id === tag.id) ? current : [...(current ?? []), tag]))
        })
    }

    function handleRemove(tagId: number) {
        removeTrackTag(trackId, tagId).then(() => {
            setTags((current) => current?.filter((tag) => tag.id !== tagId) ?? null)
        })
    }

    if (error) {
        return <p role="alert">Impossible de charger cette musique.</p>
    }
    if (track === null || tags === null) {
        return <p>Chargement...</p>
    }

    return (
        <>
            <h1>{track.name}</h1>
            <p className="track-info">
                {track.gameName} — {track.franchiseName}
            </p>

            <h2>Tags</h2>
            {tags.length === 0 && <p>Aucun tag pour l'instant.</p>}
            <ul className="tag-list">
                {tags.map((tag) => (
                    <li key={tag.id}>
            <span>
              {tag.name} <span className="tag-type">({tag.typeName})</span>
            </span>
                        <button type="button" onClick={() => handleRemove(tag.id)}>
                            Retirer
                        </button>
                    </li>
                ))}
            </ul>

            <TagPicker onPick={handleAdd} />
        </>
    )
}
