export function playWhenAllowed(audio: HTMLAudioElement): () => void {
  let stopped = false

  function stop() {
    stopped = true
    document.removeEventListener('pointerdown', start)
    document.removeEventListener('keydown', start)
  }

  function start(event: Event) {
    stop()
    if (event.target !== audio) audio.play().catch(() => {})
  }

  audio.play().catch((error: unknown) => {
    if (stopped || !(error instanceof DOMException) || error.name !== 'NotAllowedError') return
    document.addEventListener('pointerdown', start)
    document.addEventListener('keydown', start)
  })
  return stop
}
