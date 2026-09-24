import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { applyTagToGame, fetchGameTracks } from '../../api/games'
import { fetchAllTags, fetchMostUsedTags, type Tag, type TagUsage } from '../../api/tags'
import { addTrackTag, fetchTrack, fetchTrackTags, removeTrackTag, type Track } from '../../api/tracks'
import { TagPicker } from '../../components/TagPicker/TagPicker'
import { filterTags } from './filterTags'
import './TrackDetailPage.css'

// Tags inhérents au jeu (pas à la musique elle-même) : seuls ceux-là peuvent s'appliquer à tout le jeu d'un coup.
const GAME_LEVEL_TYPES = ['genre', 'plateforme']

export function TrackDetailPage() {
    const { id } = useParams()
    const trackId = Number(id)

    const [track, setTrack] = useState<Track | null>(null)
    const [tags, setTags] = useState<Tag[] | null>(null)
    const [error, setError] = useState(false)
    const [mostUsedTags, setMostUsedTags] = useState<TagUsage[]>([])
    const [showAllTags, setShowAllTags] = useState(false)
    const [allTags, setAllTags] = useState<Tag[] | null>(null)
    const [allTagsFilter, setAllTagsFilter] = useState('')
    const [gameTracks, setGameTracks] = useState<Track[] | null>(null)
    const [selectedTrackIds, setSelectedTrackIds] = useState<Set<number>>(new Set())
    const [gameTagMessage, setGameTagMessage] = useState<string | null>(null)

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

    useEffect(() => {
        const controller = new AbortController()
        fetchMostUsedTags(controller.signal)
            .then(setMostUsedTags)
            .catch(() => {})
        return () => controller.abort()
    }, [])

    useEffect(() => {
        if (track === null) return
        const controller = new AbortController()
        fetchGameTracks(track.gameId, controller.signal)
            .then((tracks) => setGameTracks(tracks.filter((other) => other.id !== track.id)))
            .catch(() => {})
        return () => controller.abort()
    }, [track])

    function handleShowAllTags() {
        setShowAllTags(true)
        if (allTags === null) {
            fetchAllTags().then(setAllTags)
        }
    }

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

    const alreadyTaggedIds = new Set(tags.map((tag) => tag.id))
    const filteredAllTags = allTags ? filterTags(allTags, allTagsFilter) : []
    const gameId = track.gameId

    function handleApplyToGame(tag: Tag) {
        if (!window.confirm(`Ajouter le tag « ${tag.name} » à toutes les musiques de ce jeu ?`)) {
            return
        }
        applyTagToGame(gameId, tag.id).then(() => {
            setGameTagMessage(`« ${tag.name} » ajouté à toutes les musiques du jeu.`)
        })
    }

    function toggleTrackSelection(id: number) {
        setSelectedTrackIds((current) => {
            const next = new Set(current)
            if (next.has(id)) {
                next.delete(id)
            } else {
                next.add(id)
            }
            return next
        })
    }

    function handlePushTag(tag: Tag) {
        if (selectedTrackIds.size === 0) return
        Promise.all([...selectedTrackIds].map((selectedId) => addTrackTag(selectedId, tag.id)))
    }

    function toggleSelectAll() {
        if (gameTracks === null) return
        setSelectedTrackIds((current) =>
            current.size === gameTracks.length ? new Set() : new Set(gameTracks.map((gameTrack) => gameTrack.id)),
        )
    }

    return (
        <div className="track-detail">
        <div className="track-detail-main">
            <h1>{track.name}</h1>
            <p className="track-info">
                {track.gameName} — {track.franchiseName}
            </p>

            <h2>Tags</h2>
            {tags.length === 0 && <p>Aucun tag pour l'instant.</p>}
            {gameTagMessage && <p>{gameTagMessage}</p>}
            <ul className="tag-list">
                {tags.map((tag) => (
                    <li key={tag.id}>
            <span>
              {tag.name} <span className="tag-type">({tag.typeName})</span>
            </span>
                        <div className="tag-list-actions">
                            {GAME_LEVEL_TYPES.includes(tag.typeName) && (
                                <button type="button" onClick={() => handleApplyToGame(tag)}>
                                    Appliquer au jeu
                                </button>
                            )}
                            <button type="button" onClick={() => handleRemove(tag.id)}>
                                Retirer
                            </button>
                        </div>
                    </li>
                ))}
            </ul>

            <TagPicker onPick={handleAdd} />

            {mostUsedTags.length > 0 && (
                <>
                    <h3>Tags populaires</h3>
                    <ul className="tag-suggestions">
                        {mostUsedTags.map((tag) => (
                            <li key={tag.id}>
                                <button type="button" onClick={() => handleAdd(tag)} disabled={alreadyTaggedIds.has(tag.id)}>
                                    {tag.name} <span className="tag-type">({tag.typeName})</span> · {tag.count}
                                </button>
                            </li>
                        ))}
                    </ul>
                </>
            )}

            {!showAllTags ? (
                <button type="button" onClick={handleShowAllTags}>
                    + Voir tous les tags
                </button>
            ) : (
                <div className="all-tags">
                    <input
                        type="search"
                        placeholder="Filtrer les tags"
                        aria-label="Filtrer les tags"
                        value={allTagsFilter}
                        onChange={(event) => setAllTagsFilter(event.target.value)}
                    />
                    {allTags === null && <p>Chargement...</p>}
                    {allTags !== null && (
                        <ul className="tag-suggestions">
                            {filteredAllTags.map((tag) => (
                                <li key={tag.id}>
                                    <button type="button" onClick={() => handleAdd(tag)} disabled={alreadyTaggedIds.has(tag.id)}>
                                        {tag.name} <span className="tag-type">({tag.typeName})</span>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            )}
        </div>

        <aside className="track-detail-sidebar">
            <h2>Autres musiques du jeu</h2>
            {gameTracks === null && <p>Chargement...</p>}
            {gameTracks !== null && gameTracks.length === 0 && <p>Aucune autre musique de ce jeu.</p>}
            {gameTracks !== null && gameTracks.length > 0 && (
                <>
                    <button type="button" onClick={toggleSelectAll}>
                        {selectedTrackIds.size === gameTracks.length ? 'Tout désélectionner' : 'Tout sélectionner'}
                    </button>
                    <ul className="game-track-list">
                        {gameTracks.map((gameTrack) => (
                            <li key={gameTrack.id}>
                                <label>
                                    <input
                                        type="checkbox"
                                        checked={selectedTrackIds.has(gameTrack.id)}
                                        onChange={() => toggleTrackSelection(gameTrack.id)}
                                    />
                                    {gameTrack.name}
                                </label>
                            </li>
                        ))}
                    </ul>

                    {tags.length > 0 && (
                        <>
                            <p className="sidebar-hint">
                                {selectedTrackIds.size} musique{selectedTrackIds.size > 1 ? 's' : ''} sélectionnée
                                {selectedTrackIds.size > 1 ? 's' : ''}
                            </p>
                            <ul className="tag-suggestions">
                                {tags.map((tag) => (
                                    <li key={tag.id}>
                                        <button
                                            type="button"
                                            onClick={() => handlePushTag(tag)}
                                            disabled={selectedTrackIds.size === 0}
                                        >
                                            + {tag.name} aux sélectionnées
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        </>
                    )}
                </>
            )}
        </aside>
        </div>
    )
}
