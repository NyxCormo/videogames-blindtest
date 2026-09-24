import { describe, expect, it } from 'vitest'
import type { Tag } from '../../api/tags'
import { filterTags } from './filterTags'

function tag(id: number, name: string): Tag {
    return { id, name, typeName: 'genre' }
}

describe('filterTags', () => {
    it('garde les tags dont le nom contient le texte, sans tenir compte de la casse', () => {
        const action = tag(1, 'Action')
        const aventure = tag(2, 'Aventure')

        expect(filterTags([action, aventure], 'act')).toEqual([action])
    })

    it('renvoie tout quand la recherche est vide', () => {
        const action = tag(1, 'Action')
        const aventure = tag(2, 'Aventure')

        expect(filterTags([action, aventure], '')).toEqual([action, aventure])
    })

    it('ne modifie pas la liste reçue', () => {
        const action = tag(1, 'Action')
        const original = [action]

        filterTags(original, 'a')

        expect(original).toEqual([action])
    })
})
