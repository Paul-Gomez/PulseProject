import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Pages that need a session. The real check is always the backend; this only avoids showing empty screens. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.hasSession() ? true : inject(Router).createUrlTree(['/login']);
};

/** Login/register: if you are already logged in, go straight to the app. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.hasSession() ? inject(Router).createUrlTree(['/']) : true;
};
