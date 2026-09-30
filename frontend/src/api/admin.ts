const ADMIN_USERNAME = 'admin'

export async function loginAdmin(password: string): Promise<string> {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: ADMIN_USERNAME, password }),
  })
  if (!response.ok) {
    const body = await response.json()
    throw new Error(body.message ?? `Erreur ${response.status}`)
  }
  const body: { token: string } = await response.json()
  return body.token
}

export async function logoutAdmin(token: string): Promise<void> {
  const response = await fetch('/api/auth/logout', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
}

export async function checkAdmin(token: string, signal?: AbortSignal): Promise<boolean> {
  const response = await fetch('/api/admin/check', {
    headers: { Authorization: `Bearer ${token}` },
    signal,
  })
  if (response.status === 401) {
    return false
  }
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return true
}
