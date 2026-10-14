import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, finalize, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenPair, User } from '../models';

const ACCESS_KEY = 'pulse.accessToken';
const REFRESH_KEY = 'pulse.refreshToken';

/**
 * Keeps the session. Tokens live in localStorage so a reload keeps you logged in.
 * Trade-off: anything running on the page could read them (XSS), which is why Angular's
 * escaping matters and why the access token is short-lived (15 min).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly api = `${environment.apiUrl}/auth`;

  private readonly currentUserSignal = signal<User | null>(null);
  readonly currentUser = this.currentUserSignal.asReadonly();

  private refreshInProgress$: Observable<TokenPair> | null = null;

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  hasSession(): boolean {
    return this.accessToken !== null;
  }

  login(email: string, password: string): Observable<TokenPair> {
    return this.http.post<TokenPair>(`${this.api}/login`, { email, password }).pipe(tap((t) => this.store(t)));
  }

  register(data: { email: string; username: string; password: string; displayName: string }): Observable<TokenPair> {
    return this.http.post<TokenPair>(`${this.api}/register`, data).pipe(tap((t) => this.store(t)));
  }

  /** Several requests can fail with 401 at once; they all wait for the same refresh call. */
  refresh(): Observable<TokenPair> {
    if (!this.refreshInProgress$) {
      this.refreshInProgress$ = this.http
        .post<TokenPair>(`${this.api}/refresh`, { refreshToken: this.refreshToken })
        .pipe(
          tap((t) => this.store(t)),
          finalize(() => (this.refreshInProgress$ = null)),
          shareReplay(1),
        );
    }
    return this.refreshInProgress$;
  }

  loadCurrentUser(): Observable<User> {
    return this.http.get<User>(`${environment.apiUrl}/users/me`).pipe(tap((u) => this.currentUserSignal.set(u)));
  }

  setCurrentUser(user: User): void {
    this.currentUserSignal.set(user);
  }

  logout(): void {
    const refreshToken = this.refreshToken;
    if (refreshToken) {
      this.http.post(`${this.api}/logout`, { refreshToken }).subscribe({ error: () => undefined });
    }
    this.clearSession();
    this.router.navigate(['/login']);
  }

  clearSession(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    this.currentUserSignal.set(null);
  }

  verifyEmail(token: string): Observable<void> {
    return this.http.post<void>(`${this.api}/verify-email`, { token });
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${this.api}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.api}/reset-password`, { token, newPassword });
  }

  private store(tokens: TokenPair): void {
    localStorage.setItem(ACCESS_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
  }
}
