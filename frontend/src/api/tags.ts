// Forme du JSON renvoyé par GET/POST /api/tags (voir TagResponse côté backend)
export type Tag = {
    id: number
    name: string
    typeName: string
}

// Forme du JSON renvoyé par GET /api/tag-types (voir TagTypeResponse côté backend)
export type TagType = {
    id: number
    name: string
}

export async function searchTags(search: string, signal?: AbortSignal): Promise<Tag[]> {
    const response = await fetch(`/api/tags?search=${encodeURIComponent(search)}`, { signal })
    if (!response.ok) {
        throw new Error(`Erreur ${response.status}`)
    }
    return response.json()
}

export async function createTag(typeName: string, tagName: string): Promise<Tag> {
    const response = await fetch('/api/tags', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ typeName, tagName }),
    })
    if (!response.ok) {
        const body = await response.json()
        throw new Error(body.message ?? `Erreur ${response.status}`)
    }
    return response.json()
}

export async function fetchTagTypes(signal?: AbortSignal): Promise<TagType[]> {
    const response = await fetch('/api/tag-types', { signal })
    if (!response.ok) {
        throw new Error(`Erreur ${response.status}`)
    }
    return response.json()
}
