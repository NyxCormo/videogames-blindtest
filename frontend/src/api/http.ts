export async function errorMessage(response: Response): Promise<string> {
  const body = await response.json().catch(() => null)
  return body?.message ?? `Erreur ${response.status}`
}

export async function request(url: string, init?: RequestInit): Promise<Response> {
  const response = await fetch(url, init)
  if (!response.ok) {
    throw new Error(await errorMessage(response))
  }
  return response
}
