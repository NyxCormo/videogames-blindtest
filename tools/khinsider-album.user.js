// ==UserScript==
// @name         Blindtest InsaLan : pochettes KHInsider
// @namespace    blindtest-insalan
// @version      1
// @match        https://downloads.khinsider.com/game-soundtracks/album/*
// @grant        GM_xmlhttpRequest
// @connect      localhost
// ==/UserScript==

const SERVER = 'http://localhost:4673'
const TOKEN = 'test'

function show(text) {
  const box = document.createElement('div')
  box.textContent = text
  Object.assign(box.style, {
    position: 'fixed',
    right: '1rem',
    bottom: '1rem',
    zIndex: 9999,
    maxWidth: '24rem',
    padding: '0.75rem 1rem',
    border: '1px solid #63d1ff',
    borderRadius: '0.5rem',
    background: '#161a2b',
    color: '#ffffff',
    font: '14px sans-serif',
  })
  document.body.append(box)
  setTimeout(() => box.remove(), 8000)
}

function describe(response) {
  if (response.status === 200) {
    const result = JSON.parse(response.responseText)
    const covered = result.coveredGames.length > 0 ? ` Nouvelle image pour : ${result.coveredGames.join(', ')}.` : ''
    return `Blindtest : ${result.images.length} pochette(s) trouvée(s) pour ${result.album}.${covered}`
  }
  try {
    return `Blindtest : ${JSON.parse(response.responseText).message}`
  } catch {
    return `Blindtest : erreur ${response.status}`
  }
}

if (location.pathname.split('/').filter(Boolean).length === 3) {
  GM_xmlhttpRequest({
    method: 'POST',
    url: `${SERVER}/api/khinsider/album-page`,
    headers: {
      'Content-Type': 'text/html; charset=utf-8',
      'X-Album-Url': location.origin + location.pathname,
      'X-Import-Token': TOKEN,
    },
    data: document.documentElement.outerHTML,
    onload: (response) => show(describe(response)),
    onerror: () => show(`Blindtest : serveur injoignable (${SERVER})`),
  })
}
