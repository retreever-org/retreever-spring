// ==========================
// Retreever Service Worker
// ==========================

const STATIC_CACHE = 'retreever-static-v1';

// List of PWA assets that must NEVER hit network again
const PRECACHE_URLS = [
  '/images/Icon192.png',
  '/images/Icon256.png',
  '/images/Icon512.png',
  '/images/screenshot.png',
  '/images/retreever-icon.png',
  '/images/retreever-logo.svg'
];

// ---- INSTALL: precache everything ----
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE).then((cache) => cache.addAll(PRECACHE_URLS))
  );
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

// ---- FETCH: hard short-circuit for /images/* ----
self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  // Only same-origin image assets
  if (url.origin === self.location.origin && url.pathname.startsWith('/images/')) {
    event.respondWith(serveImageFromCache(event.request));
    return;
  }

  // let everything else pass through
});

// ---- Serve images ONLY from cache (never network) ----
async function serveImageFromCache(request) {
  const cache = await caches.open(STATIC_CACHE);

  const cached = await cache.match(request);
  if (cached) return cached;

  // fallback (shouldn't normally happen)
  try {
    const response = await fetch(request);
    if (response && response.status === 200) {
      cache.put(request, response.clone());
    }
    return response;
  } catch {
    return new Response('', { status: 404 });
  }
}
