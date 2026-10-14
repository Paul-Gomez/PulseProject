import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    localStorage.setItem('pulse.accessToken', 'old-access');
    localStorage.setItem('pulse.refreshToken', 'refresh-1');
    TestBed.configureTestingModule({
      providers: [
        // logout() navigates to /login, so the test router needs to accept any URL.
        provideRouter([{ path: '**', children: [] }]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
    localStorage.clear();
  });

  it('adds the access token to API calls', () => {
    http.get('/api/v1/workspaces').subscribe();

    const request = backend.expectOne('/api/v1/workspaces');
    expect(request.request.headers.get('Authorization')).toBe('Bearer old-access');
    request.flush([]);
  });

  it('does not send the token to other hosts', () => {
    http.get('https://example.com/data').subscribe();

    const request = backend.expectOne('https://example.com/data');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });

  it('refreshes the token after a 401 and repeats the request once', () => {
    let result: unknown;
    http.get('/api/v1/users/me').subscribe((body) => (result = body));

    backend.expectOne('/api/v1/users/me').flush({}, { status: 401, statusText: 'Unauthorized' });

    const refresh = backend.expectOne('/api/v1/auth/refresh');
    expect(refresh.request.body).toEqual({ refreshToken: 'refresh-1' });
    refresh.flush({ accessToken: 'new-access', refreshToken: 'refresh-2', tokenType: 'Bearer' });

    const retry = backend.expectOne('/api/v1/users/me');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer new-access');
    retry.flush({ id: '1' });

    expect(result).toEqual({ id: '1' });
    expect(localStorage.getItem('pulse.refreshToken')).toBe('refresh-2');
  });

  it('logs out when the refresh token is also rejected', () => {
    http.get('/api/v1/users/me').subscribe({ error: () => undefined });

    backend.expectOne('/api/v1/users/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/v1/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/v1/auth/logout').flush({});

    expect(localStorage.getItem('pulse.accessToken')).toBeNull();
  });
});
