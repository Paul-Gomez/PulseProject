import { HttpErrorResponse } from '@angular/common/http';
import { errorMessage } from './errors';

describe('errorMessage', () => {
  it('translates known API error codes', () => {
    const error = new HttpErrorResponse({ status: 401, error: { code: 'INVALID_CREDENTIALS', message: 'x' } });
    expect(errorMessage(error)).toBe('Email o contraseña incorrectos.');
  });

  it('lists validation details', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { code: 'VALIDATION_ERROR', message: 'Validation failed', details: ['email: must be a well-formed email'] },
    });
    expect(errorMessage(error)).toContain('email: must be a well-formed email');
  });

  it('explains when the server is unreachable', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toBe('No se puede conectar con el servidor.');
  });

  it('falls back to a generic message', () => {
    expect(errorMessage(new Error('boom'))).toBe('Algo ha ido mal. Inténtalo de nuevo.');
  });
});
