// On a device or Android emulator "localhost" is the phone itself, not your PC.
// The Android emulator reaches the host machine at 10.0.2.2; on a real phone use your PC's LAN IP
// (and add that origin to CORS_ALLOWED_ORIGINS in the backend).
export const environment = {
  production: true,
  apiUrl: 'http://10.0.2.2:8080/api/v1',
  wsUrl: 'ws://10.0.2.2:8080/ws',
};
