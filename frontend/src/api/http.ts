export class HttpError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export async function errorMessage(response: Response): Promise<string> {
  const body = await response.json().catch(() => null)
  return body?.message ?? `Erreur ${response.status}`
}

export async function request(url: string, init?: RequestInit): Promise<Response> {
  const response = await fetch(url, init)
  if (!response.ok) {
    throw new HttpError(response.status, await errorMessage(response))
  }
  return response
}
