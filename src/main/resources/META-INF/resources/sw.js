/*
 * Tombstone service worker.
 *
 * Karnak used to be a PWA (@PWA on AppShellConfig), which made Vaadin generate and
 * register a service worker at this origin. The PWA was removed, but browsers that
 * loaded an older build still have that worker registered. The stale worker calls
 * importScripts('/sw-runtime-resources-precache.js'), which no longer exists (404),
 * so it fails to start and breaks navigation/session handling (the user is silently
 * bounced to the login page with nothing in the server log).
 *
 * The browser re-fetches /sw.js on its update check; serving this script makes the
 * stale worker update to one that clears all caches and unregisters itself, so the
 * origin returns to a plain, worker-free state. Once every client has updated this
 * file (and the static sw-* artifacts) can be deleted.
 */
self.addEventListener('install', () => self.skipWaiting());

self.addEventListener('activate', (event) => {
  event.waitUntil(
    (async () => {
      try {
        const keys = await caches.keys();
        await Promise.all(keys.map((key) => caches.delete(key)));
      } catch (e) {
        // Ignore cache-clearing failures; unregistering is what matters.
      }
      await self.registration.unregister();
      const clients = await self.clients.matchAll({ type: 'window' });
      clients.forEach((client) => client.navigate(client.url));
    })(),
  );
});
