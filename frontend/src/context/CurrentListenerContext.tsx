import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import type { Listener } from '../api/listeners'

const STORAGE_KEY = 'blindtest.currentListener'

type CurrentListenerContextValue = {
  listener: Listener | null
  connect: (listener: Listener) => void
  disconnect: () => void
}

const CurrentListenerContext = createContext<CurrentListenerContextValue | null>(null)

export function CurrentListenerProvider({ children }: { children: ReactNode }) {
  const [listener, setListener] = useState<Listener | null>(() => {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : null
  })

  useEffect(() => {
    if (listener) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(listener))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }, [listener])

  return (
    <CurrentListenerContext.Provider value={{ listener, connect: setListener, disconnect: () => setListener(null) }}>
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
