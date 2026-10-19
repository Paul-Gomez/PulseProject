import type { ApiError } from "./types";

const MESSAGES: Record<string, string> = {
  INVALID_CREDENTIALS: "Email o contraseña incorrectos.",
  EMAIL_ALREADY_IN_USE: "Ese email ya está registrado.",
  USERNAME_ALREADY_IN_USE: "Ese nombre de usuario ya existe.",
  TOO_MANY_LOGIN_ATTEMPTS: "Demasiados intentos fallidos. Prueba en unos minutos.",
  RATE_LIMITED: "Demasiadas peticiones. Espera un momento.",
  ACCESS_DENIED: "No tienes permiso para hacer esto.",
  CHANNEL_NAME_TAKEN: "Ya hay un canal con ese nombre.",
  CHANNEL_ARCHIVED: "Este canal está archivado.",
};

/** Error thrown by the API client, carrying the backend's error body. */
export class HttpError extends Error {
  constructor(
    readonly status: number,
    readonly body: Partial<ApiError> | null,
  ) {
    super(body?.message ?? `HTTP ${status}`);
  }
}

/** Turns any error into a sentence the user can understand. */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpError) {
    const code = error.body?.code;
    if (code && MESSAGES[code]) return MESSAGES[code];
    if (code === "VALIDATION_ERROR" && error.body?.details?.length) {
      return "Revisa los datos: " + error.body.details.join(", ");
    }
    if (error.body?.message) return error.body.message;
  }
  if (error instanceof TypeError) return "No se puede conectar con el servidor.";
  return "Algo ha ido mal. Inténtalo de nuevo.";
}
