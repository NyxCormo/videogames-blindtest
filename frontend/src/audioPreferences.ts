const VOLUME_KEY = 'blindtest.audioVolume'

export function loadStoredVolume(): number {
  try {
    const stored = localStorage.getItem(VOLUME_KEY)
    const volume = stored === null ? 1 : Number(stored)
    return Number.isFinite(volume) && volume >= 0 && volume <= 1 ? volume : 1
  } catch {
    return 1
  }
}

export function storeVolume(volume: number): void {
  try {
    localStorage.setItem(VOLUME_KEY, String(volume))
  } catch {
    // stockage indisponible : tant pis, le volume ne sera juste pas mémorisé
  }
}
