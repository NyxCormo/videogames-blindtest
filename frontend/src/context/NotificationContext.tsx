import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'

export type NotificationKind = 'success' | 'warning' | 'error'

type Notification = {
  message: string
  kind: NotificationKind
}

type NotificationContextValue = {
  notification: Notification | null
  notify: (message: string, kind: NotificationKind) => void
  dismiss: () => void
}

const NotificationContext = createContext<NotificationContextValue | null>(null)

const SUCCESS_DURATION_MS = 4000

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [notification, setNotification] = useState<Notification | null>(null)

  // Seul le succès disparaît tout seul : une erreur ou un avertissement reste jusqu'à ce qu'on le ferme.
  useEffect(() => {
    if (notification?.kind !== 'success') return
    const timeout = setTimeout(() => setNotification(null), SUCCESS_DURATION_MS)
    return () => clearTimeout(timeout)
  }, [notification])

  return (
    <NotificationContext.Provider
      value={{
        notification,
        notify: (message, kind) => setNotification({ message, kind }),
        dismiss: () => setNotification(null),
      }}
    >
      {children}
    </NotificationContext.Provider>
  )
}

export function useNotification() {
  const context = useContext(NotificationContext)
  if (!context) {
    throw new Error('useNotification doit être utilisé dans NotificationProvider')
  }
  return context
}
