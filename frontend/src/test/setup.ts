import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'

// Point d'entree unique des setupFiles Vitest (Phase 10 - tache #88) :
// ajoute les matchers jest-dom (toBeInTheDocument, etc.) a `expect`. Tenu
// volontairement minimal - pas de mock global ici, chaque test mock ce
// dont il a besoin pour rester lisible isolement.

// `globals: false` (vitest.config.ts) signifie que testing-library ne peut
// pas detecter tout seul un `afterEach` global pour son nettoyage
// automatique du DOM entre les tests - on l'enregistre donc explicitement
// ici, une fois, plutot que dans chaque fichier de test.
afterEach(() => {
  cleanup()
})

// jsdom (25.x) implemente Blob mais PAS Blob.prototype.text()/arrayBuffer()
// (limitation connue de jsdom, pas du navigateur reel) - or
// lib/api-error.ts (extractBlobApiErrorMessage) en depend pour lire le
// corps d'une reponse d'erreur `responseType: 'blob'`. On comble ce trou
// via FileReader (que jsdom implemente) pour que les tests reflentent le
// comportement d'un vrai navigateur plutot que la lacune de jsdom.
if (typeof Blob !== 'undefined' && typeof Blob.prototype.text !== 'function') {
  Blob.prototype.text = function (this: Blob) {
    return new Promise<string>((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(String(reader.result))
      reader.onerror = () => reject(reader.error)
      reader.readAsText(this)
    })
  }
}
