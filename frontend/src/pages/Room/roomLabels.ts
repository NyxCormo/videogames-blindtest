import type { DrawMode, Room, RoomMode, RoomPhase, RoomVisibility, ScoringMode } from '../../api/rooms'

export const VISIBILITY_LABELS: Record<RoomVisibility, string> = {
  PUBLIC: 'Publique',
  PRIVATE: 'Privée',
}

export const MODE_LABELS: Record<RoomMode, string> = {
  MULTI: 'Multi',
  PRESENTATION: 'Présentation',
}

export const PLAYBACK_LABELS = {
  FREE: 'Libre',
  SYNCED: 'Synchronisée',
}

export const DRAW_LABELS: Record<DrawMode, string> = {
  TRACK: 'Musique au hasard',
  FRANCHISE: 'Franchise, puis jeu, puis musique',
}

export function trackProgress(room: Room, position: number): string {
  return room.infinite ? `Musique ${position + 1}` : `Musique ${position + 1} sur ${room.trackCount}`
}

export const SCORING_LABELS: Record<ScoringMode, string> = {
  SPEED: 'Rapidité',
  FIRST_COME: 'Premiers servis',
}

export const PHASE_LABELS: Record<RoomPhase, string> = {
  WAITING: 'En attente',
  PLAYING: 'En cours',
  REVEAL: 'En cours',
  FINISHED: 'Terminée',
}
