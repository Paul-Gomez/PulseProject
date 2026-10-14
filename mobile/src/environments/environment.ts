// `ionic serve` runs on localhost:8100 and calls the backend directly (CORS allows 8100).
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  wsUrl: 'ws://localhost:8080/ws',
};
