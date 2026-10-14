// Relative URLs: nginx (Docker) or the ng serve proxy forward /api and /ws to the backend,
// so the browser always talks to the same origin that served the app.
const wsProtocol = typeof location !== 'undefined' && location.protocol === 'https:' ? 'wss' : 'ws';
const host = typeof location !== 'undefined' ? location.host : 'localhost:4200';

export const environment = {
  apiUrl: '/api/v1',
  wsUrl: `${wsProtocol}://${host}/ws`,
};
