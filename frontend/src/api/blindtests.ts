// Forme du JSON renvoyé par GET /api/blindtests (voir BlindtestResponse côté backend)
export type Blindtest = {
  id: number
  name: string
  difficulty: number
  createdAt: string
}

export async function fetchBlindtests(signal?: AbortSignal): Promise<Blindtest[]> {
  const response = await fetch('/api/blindtests', { signal })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

// Un palier de difficulté envoyé à la création (voir DifficultyBandRequest côté backend)
export type DifficultyBand = {
  minDifficulty: number
  maxDifficulty: number
  proportion: number
}

export async function createBlindtest(
    name: string,
    trackCount: number,
    difficultyBands: DifficultyBand[],
    tagIds: number[],
    matchAllTags: boolean,
    maxPerGame: number | null,
    maxPerFranchise: number | null,
    strategy: string | null,
    maxAttempts: number | null
): Promise<Blindtest> {
    const response = await fetch(`/api/blindtests`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        name,
        trackCount,
        difficultyBands,
        tagIds,
        matchAllTags,
        maxPerGame,
        maxPerFranchise,
        strategy,
        maxAttempts,
    }),
    })
    if (!response.ok) {
        const body = await response.json()
        throw new Error(body.message ?? `Erreur ${response.status}`)
    }
    return response.json()
}

// Forme du JSON renvoyé par GET /api/blindtests/{id}/session (voir BlindtestSessionResponse côté backend)
export type BlindtestSession = {
  trackId: number | null
  audioLink: string | null
  finished: boolean
  tracksHeard: number
  totalTracks: number
  goodAnswers: number
  attemptsRemaining: number
}

// Forme du JSON renvoyé quand une musique est révélée (bonne réponse ou passe)
export type Reveal = {
  trackId: number
  franchiseName: string
  gameName: string
  trackName: string
}

export type GuessResult = {
  correct: boolean
  bonusCorrect: boolean
  reveal: Reveal | null
}

export async function fetchSession(blindtestId: number, listenerId: number, signal?: AbortSignal): Promise<BlindtestSession> {
  const response = await fetch(`/api/blindtests/${blindtestId}/session?listenerId=${listenerId}`, { signal })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

export async function submitGuess(
  blindtestId: number,
  listenerId: number,
  gameId: number,
  trackId: number | null,
): Promise<GuessResult> {
  const response = await fetch(`/api/blindtests/${blindtestId}/guess?listenerId=${listenerId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ gameId, trackId }),
  })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

export type FranchiseGuessResult = {
  correct: boolean
  reveal: Reveal | null
}

export async function submitGuessFranchise(
  blindtestId: number,
  listenerId: number,
  franchiseId: number,
): Promise<FranchiseGuessResult> {
  const response = await fetch(`/api/blindtests/${blindtestId}/guess-franchise?listenerId=${listenerId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ franchiseId }),
  })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

export async function submitPass(blindtestId: number, listenerId: number): Promise<Reveal> {
  const response = await fetch(`/api/blindtests/${blindtestId}/pass?listenerId=${listenerId}`, { method: 'POST' })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

export async function submitKnowAnyway(blindtestId: number, listenerId: number, trackId: number): Promise<void> {
  const response = await fetch(
    `/api/blindtests/${blindtestId}/know-anyway?listenerId=${listenerId}&trackId=${trackId}`,
    { method: 'POST' },
  )
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
}

// Forme du JSON renvoyé par GET /api/blindtests/{id}/leaderboard (voir LeaderboardEntryResponse côté backend)
export type LeaderboardEntry = {
  listenerName: string
  goodAnswers: number
  bonusAnswers: number
  tracksHeard: number
}

export async function fetchLeaderboard(blindtestId: number, signal?: AbortSignal): Promise<LeaderboardEntry[]> {
  const response = await fetch(`/api/blindtests/${blindtestId}/leaderboard`, { signal })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}
