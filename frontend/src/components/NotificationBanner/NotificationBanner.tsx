import { useNotification } from '../../context/NotificationContext'
import './NotificationBanner.css'

export function NotificationBanner() {
  const { notification, dismiss } = useNotification()

  if (!notification) {
    return null
  }

  return (
    <div
      className={`notification-banner notification-${notification.kind}`}
      role={notification.kind === 'error' ? 'alert' : 'status'}
    >
      <span>{notification.message}</span>
      <button type="button" onClick={dismiss} aria-label="Fermer le message">
        ×
      </button>
    </div>
  )
}
