import { request } from './http'

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
    const response = await request(`/api/tags?search=${encodeURIComponent(search)}`, { signal })
    return response.json()
}

export async function createTag(typeId: number, tagName: string): Promise<Tag> {
    const response = await request('/api/tags', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ typeId, tagName }),
    })
    return response.json()
}

export async function fetchTagTypes(signal?: AbortSignal): Promise<TagType[]> {
    const response = await request('/api/tag-types', { signal })
    return response.json()
}

// Forme du JSON renvoyé par GET /api/tags/most-used (voir TagUsageResponse côté backend)
export type TagUsage = Tag & { count: number }

export async function fetchMostUsedTags(signal?: AbortSignal): Promise<TagUsage[]> {
    const response = await request('/api/tags/most-used', { signal })
    return response.json()
}

export async function fetchAllTags(signal?: AbortSignal): Promise<Tag[]> {
    const response = await request('/api/tags/all', { signal })
    return response.json()
}
