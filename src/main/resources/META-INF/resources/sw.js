// ==========================
// Retreever Service Worker
// ==========================

const STATIC_CACHE = 'retreever-static-v3';

// App icons are resolved from environment-backed asset paths now.
self.addEventListener('install', () => {
  self.skipWaiting();
});

// ---- ACTIVATE: clean old caches ----
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((k) => k !== STATIC_CACHE)
          .map((k) => caches.delete(k))
      )
    ).then(() => self.clients.claim())
  );
});
