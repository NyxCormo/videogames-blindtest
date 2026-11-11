import type { RoomPhase, RoomVisibility, ScoringMode, SoundMode } from '../../api/rooms'

export const VISIBILITY_LABELS: Record<RoomVisibility, string> = {
  PUBLIC: 'Publique',
  PRIVATE: 'Privée',
}

export const SOUND_LABELS: Record<SoundMode, string> = {
  EVERY_DEVICE: 'Sur chaque appareil',
  MAIN_SCREEN: 'Sur un écran principal',
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
