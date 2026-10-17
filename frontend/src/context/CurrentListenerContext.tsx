import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { HttpError } from '../api/http'
import { openSession, type Listener } from '../api/listeners'
import { forget, loadRecent, remember, rename, storeRecent } from '../recentListeners'
import { useNotification } from './NotificationContext'

const STORAGE_KEY = 'blindtest.currentListener'

type CurrentListenerContextValue = {
  listener: Listener | null
  recent: Listener[]
  forgetRecent: (id: number) => void
  connect: (listener: Listener) => void
  disconnect: () => void
  loginOpen: boolean
  openLogin: () => void
  closeLogin: () => void
}

const CurrentListenerContext = createContext<CurrentListenerContextValue | null>(null)

export function CurrentListenerProvider({ children }: { children: ReactNode }) {
  const [listener, setListener] = useState<Listener | null>(() => {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : null
  })

  const [loginOpen, setLoginOpen] = useState(false)
  const [recent, setRecent] = useState<Listener[]>(loadRecent)
  const { notify } = useNotification()
  const listenerId = listener?.id

  useEffect(() => {
    if (listenerId === undefined) return
    const controller = new AbortController()
    openSession(listenerId, controller.signal)
      .then((fresh) => {
        setListener((current) => (current?.id === fresh.id && current.name !== fresh.name ? fresh : current))
        setRecent((current) => remember(rename(current, fresh), fresh))
      })
      .catch((err) => {
        if (controller.signal.aborted || !(err instanceof HttpError) || err.status !== 404) return
        setListener(null)
        setRecent((current) => forget(current, listenerId))
        notify("Le pseudo enregistré sur cet appareil n'existe plus : il a été fusionné ou supprimé. Il faut se reconnecter.", 'warning')
      })
    return () => controller.abort()
  }, [listenerId, notify])

  useEffect(() => {
    storeRecent(recent)
  }, [recent])

  useEffect(() => {
    if (listener) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(listener))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }, [listener])

  return (
    <CurrentListenerContext.Provider
      value={{
        listener,
        recent,
        forgetRecent: (id) => setRecent((current) => forget(current, id)),
        connect: setListener,
        disconnect: () => setListener(null),
        loginOpen,
        openLogin: () => setLoginOpen(true),
        closeLogin: () => setLoginOpen(false),
      }}
    >
      {children}
    </CurrentListenerContext.Provider>
  )
}

export function useCurrentListener() {
  const context = useContext(CurrentListenerContext)
  if (!context) {
    throw new Error('useCurrentListener doit être utilisé dans CurrentListenerProvider')
  }
  return context
}
