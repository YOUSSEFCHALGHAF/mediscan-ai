/*
 * Service worker de Firebase Cloud Messaging (MediScan AI).
 * Il reçoit les notifications quand l'onglet est fermé ou en arrière-plan,
 * et les affiche via le système (centre de notifications Windows, macOS, Android...).
 *
 * Les scripts Firebase sont copiés depuis node_modules par angular.json (dossier /firebase) :
 * même version que l'application, aucun CDN externe.
 * La configuration Firebase est transmise par l'application dans l'URL (?config=...),
 * pour n'avoir qu'une seule source : le fichier environment.ts.
 */

/*
 * Clic sur une notification. Ce gestionnaire est enregistré AVANT celui de Firebase, qui interrompt
 * la propagation de l'événement. En production (HTTPS), le serveur fournit un lien "fcmOptions.link"
 * que Firebase ouvre lui-même ; en développement (http://localhost), Firebase refuse ce lien :
 * on ouvre alors nous-mêmes la page indiquée dans data.lien.
 */
self.addEventListener('notificationclick', (evenement) => {
  const message = evenement.notification.data && evenement.notification.data.FCM_MSG;
  if (!message || (message.fcmOptions && message.fcmOptions.link)) {
    return; // Firebase s'en charge
  }
  const chemin = message.data && message.data.lien;
  if (!chemin) {
    return;
  }
  evenement.stopImmediatePropagation();
  evenement.notification.close();
  evenement.waitUntil(ouvrirPage(chemin));
});

/** Réutilise un onglet MediScan déjà ouvert, sinon en ouvre un nouveau. */
async function ouvrirPage(chemin) {
  const url = new URL(chemin, self.location.origin).href;
  const fenetres = await clients.matchAll({ type: 'window', includeUncontrolled: true });
  const onglet = fenetres.find((f) => f.url.startsWith(self.location.origin));
  if (onglet) {
    await onglet.focus();
    return onglet.navigate(url);
  }
  return clients.openWindow(url);
}

importScripts('/firebase/firebase-app-compat.js', '/firebase/firebase-messaging-compat.js');

const parametres = new URL(self.location.href).searchParams;
const config = JSON.parse(parametres.get('config') || '{}');

if (config.apiKey) {
  firebase.initializeApp(config);
  // Les messages envoyés par le serveur contiennent une partie "notification" : le SDK les affiche lui-même.
  firebase.messaging();
}