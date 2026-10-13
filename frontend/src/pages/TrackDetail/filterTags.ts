import type { Tag } from '../../api/tags'
import { matchesWords, searchWords } from '../../textSearch'

export function filterTags(tags: Tag[], query: string): Tag[] {
    const words = searchWords(query)
    if (words.length === 0) {
        return tags
    }
    return tags.filter((tag) => matchesWords(tag.name, words))
}
