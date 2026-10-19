import { useEffect, useEffectEvent } from 'react'

export function useEnterToContinue(active: boolean, onEnter: () => void): void {
  const continueNow = useEffectEvent(onEnter)

  useEffect(() => {
    if (!active) return
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key !== 'Enter' || event.repeat || event.target instanceof HTMLInputElement) return
      event.preventDefault()
      continueNow()
    }
    window.addEventListener('keydown', handleKeyDown, true)
    return () => window.removeEventListener('keydown', handleKeyDown, true)
  }, [active])
}
