import { useEffect, useState, type FormEvent } from 'react'
import { checkAdmin, loginAdmin, logoutAdmin } from '../../api/admin'
import { useNotification } from '../../context/NotificationContext'
import { loadAdminToken, storeAdminToken } from './adminToken'
import { EditSection } from './EditSection'
import { TrackLinksSection } from './TrackLinksSection'
import './AdminPage.css'

export function AdminPage() {
  const { notify } = useNotification()
  const [token, setToken] = useState<string | null>(loadAdminToken)
  const [verified, setVerified] = useState(false)
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function saveToken(next: string | null) {
    setToken(next)
    storeAdminToken(next)
  }

  useEffect(() => {
    const stored = loadAdminToken()
    if (stored === null) return
    const controller = new AbortController()
    checkAdmin(stored, controller.signal)
      .then((valid) => {
        if (valid) {
          setVerified(true)
        } else {
          saveToken(null)
        }
      })
      .catch(() => {
        if (controller.signal.aborted) return
        saveToken(null)
        notify('Impossible de vérifier la connexion admin.', 'error')
      })
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function handleLogin(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    loginAdmin(password)
      .then((newToken) => {
        saveToken(newToken)
        setVerified(true)
        setPassword('')
        notify('Connecté au panneau admin.', 'success')
      })
      .catch((err: Error) => notify(err.message, 'error'))
      .finally(() => setSubmitting(false))
  }

  function handleLogout() {
    if (token === null) return
    logoutAdmin(token)
      .then(() => notify('Déconnecté du panneau admin.', 'success'))
      .catch(() => notify('La déconnexion a échoué côté serveur, le token expirera tout seul.', 'warning'))
    saveToken(null)
    setVerified(false)
  }

  function handleSessionExpired() {
    saveToken(null)
    setVerified(false)
  }

  if (token === null) {
    return (
      <>
        <h1>Administration</h1>
        <form className="admin-login" onSubmit={handleLogin}>
          <label>
            Mot de passe admin
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoFocus
            />
          </label>
          <button type="submit" disabled={submitting || password === ''}>
            Se connecter
          </button>
        </form>
      </>
    )
  }

  if (!verified) {
    return <p>Chargement...</p>
  }

  return (
    <>
      <h1>Administration</h1>
      <div className="admin-session">
        <span>Connecté en tant qu'admin.</span>
        <button type="button" onClick={handleLogout}>
          Se déconnecter
        </button>
      </div>
      <TrackLinksSection token={token} onSessionExpired={handleSessionExpired} />
      <EditSection token={token} onSessionExpired={handleSessionExpired} />
    </>
  )
}
