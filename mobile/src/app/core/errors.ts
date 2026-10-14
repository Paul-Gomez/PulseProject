import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './models';

const MESSAGES: Record<string, string> = {
  INVALID_CREDENTIALS: 'Email o contraseña incorrectos.',
  EMAIL_ALREADY_IN_USE: 'Ese email ya está registrado.',
  USERNAME_ALREADY_IN_USE: 'Ese nombre de usuario ya existe.',
  TOO_MANY_LOGIN_ATTEMPTS: 'Demasiados intentos fallidos. Prueba de nuevo en unos minutos.',
  RATE_LIMITED: 'Demasiadas peticiones. Espera un momento.',
  ACCESS_DENIED: 'No tienes permiso para hacer esto.',
  USER_NOT_FOUND: 'No existe ningún usuario con ese email.',
  MEMBER_ALREADY_EXISTS: 'Esa persona ya es miembro.',
  USER_BANNED: 'Ese usuario está baneado de este workspace.',
  CHANNEL_NAME_TAKEN: 'Ya hay un canal con ese nombre.',
  CHANNEL_ARCHIVED: 'Este canal está archivado.',
  INVALID_TOKEN: 'El enlace no es válido o ha caducado.',
  INVALID_PASSWORD: 'La contraseña actual no es correcta.',
  PAYLOAD_TOO_LARGE: 'El archivo es demasiado grande (máximo 10 MB).',
  UNSUPPORTED_MEDIA_TYPE: 'Tipo de archivo no permitido.',
};

/** Turns an API error into a message the user can understand. */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'No se puede conectar con el servidor.';
    }
    const body = error.error as Partial<ApiError> | null;
    if (body?.code && MESSAGES[body.code]) {
      return MESSAGES[body.code];
    }
    if (body?.code === 'VALIDATION_ERROR' && body.details?.length) {
      return 'Revisa los datos: ' + body.details.join(', ');
    }
    if (body?.message) {
      return body.message;
    }
  }
  return 'Algo ha ido mal. Inténtalo de nuevo.';
}
