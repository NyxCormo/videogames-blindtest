import { AdminSessionExpired } from '../../api/admin'
import { useNotification } from '../../context/NotificationContext'

// Une session expirée renvoie au formulaire de connexion ; toute erreur s'affiche dans le bandeau.
export function useAdminErrorHandler(onSessionExpired: () => void) {
  const { notify } = useNotification()
  return (err: Error) => {
    if (err instanceof AdminSessionExpired) {
      onSessionExpired()
    }
    notify(err.message, 'error')
  }
}
