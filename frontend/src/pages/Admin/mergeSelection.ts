import { createContext, useContext, useState } from 'react'
import type { AdminKind } from '../../api/admin'

export type MergeSlot = 'a' | 'b'
export type MergeItem = { id: number; name: string }
export type MergeSelection = { kind: AdminKind; a: MergeItem | null; b: MergeItem | null; keep: MergeSlot }
export type LastMerge = { kind: AdminKind; sourceId: number; targetId: number }

type MergeSelectionValue = {
  selection: MergeSelection | null
  slotOf: (kind: AdminKind, id: number) => MergeSlot | null
  canSelect: (kind: AdminKind, id: number) => boolean
  toggle: (kind: AdminKind, item: MergeItem) => void
  remove: (slot: MergeSlot) => void
  switchKeep: () => void
  clear: () => void
  // Change après chaque fusion : les onglets rechargent leurs listes.
  lastMerge: LastMerge | null
  merged: (merge: LastMerge) => void
}

export const MergeSelectionContext = createContext<MergeSelectionValue | null>(null)

export function useMergeSelection() {
  const context = useContext(MergeSelectionContext)
  if (!context) {
    throw new Error('useMergeSelection doit être utilisé dans MergeSelectionContext.Provider')
  }
  return context
}

// Deux éléments au plus, du même type, jamais deux fois le même.
export function useMergeSelectionState(): MergeSelectionValue {
  const [selection, setSelection] = useState<MergeSelection | null>(null)
  const [lastMerge, setLastMerge] = useState<LastMerge | null>(null)

  function slotOf(kind: AdminKind, id: number): MergeSlot | null {
    if (selection === null || selection.kind !== kind) return null
    if (selection.a?.id === id) return 'a'
    if (selection.b?.id === id) return 'b'
    return null
  }

  function canSelect(kind: AdminKind, id: number): boolean {
    if (slotOf(kind, id) !== null) return true
    if (selection === null) return true
    return selection.kind === kind && (selection.a === null || selection.b === null)
  }

  function remove(slot: MergeSlot) {
    setSelection((current) => {
      if (current === null) return null
      const next = { ...current, [slot]: null }
      if (next.a === null && next.b === null) return null
      return { ...next, keep: next[next.keep] === null ? (next.keep === 'a' ? 'b' : 'a') : next.keep }
    })
  }

  function toggle(kind: AdminKind, item: MergeItem) {
    const slot = slotOf(kind, item.id)
    if (slot !== null) {
      remove(slot)
      return
    }
    if (!canSelect(kind, item.id)) return
    setSelection((current) => {
      if (current === null) return { kind, a: item, b: null, keep: 'a' }
      return current.a === null ? { ...current, a: item } : { ...current, b: item }
    })
  }

  return {
    selection,
    slotOf,
    canSelect,
    toggle,
    remove,
    switchKeep: () => setSelection((current) => current && { ...current, keep: current.keep === 'a' ? 'b' : 'a' }),
    clear: () => setSelection(null),
    lastMerge,
    merged: (merge) => {
      setSelection(null)
      setLastMerge(merge)
    },
  }
}
