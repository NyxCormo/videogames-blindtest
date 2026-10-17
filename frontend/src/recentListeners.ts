import type { Listener } from './api/listeners'

const STORAGE_KEY = 'blindtest.recentListeners'
const MAX_RECENT = 5

export function loadRecent(): Listener[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : []
  } catch {
    return []
  }
}

export function storeRecent(recent: Listener[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(recent))
  } catch {
    // stockage indisponible : la liste ne sera pas mémorisée
  }
}

export function remember(recent: Listener[], listener: Listener): Listener[] {
  return [listener, ...recent.filter((other) => other.id !== listener.id)].slice(0, MAX_RECENT)
}

export function forget(recent: Listener[], id: number): Listener[] {
  return recent.filter((other) => other.id !== id)
}

export function rename(recent: Listener[], listener: Listener): Listener[] {
  return recent.map((other) => (other.id === listener.id ? listener : other))
}
