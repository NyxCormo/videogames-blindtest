const TOKEN_KEY = 'blindtest.adminToken'

export function loadAdminToken(): string | null {
  try {
    return sessionStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function storeAdminToken(token: string | null): void {
  try {
    if (token === null) {
      sessionStorage.removeItem(TOKEN_KEY)
    } else {
      sessionStorage.setItem(TOKEN_KEY, token)
    }
  } catch {
    // stockage indisponible : il faudra se reconnecter après un rechargement
  }
}
