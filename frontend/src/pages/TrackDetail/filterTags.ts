import type { Tag } from '../../api/tags'

export function filterTags(tags: Tag[], query: string): Tag[] {
    const trimmed = query.trim().toLowerCase()
    if (trimmed.length === 0) {
        return tags
    }
    return tags.filter((tag) => tag.name.toLowerCase().includes(trimmed))
}
