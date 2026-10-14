import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Notification, Page, SearchResult, User, UserSummary } from '../models';

@Injectable({ providedIn: 'root' })
export class NotificationApi {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/notifications`;

  list(page = 0, unreadOnly = false): Observable<Page<Notification>> {
    return this.http.get<Page<Notification>>(this.api, { params: { page, size: 20, unreadOnly } });
  }

  unreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.api}/unread-count`);
  }

  markAsRead(id: string): Observable<Notification> {
    return this.http.patch<Notification>(`${this.api}/${id}/read`, {});
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.api}/read-all`, {});
  }
}

export interface SearchFilters {
  q?: string;
  workspaceId?: string;
  channelId?: string;
  from?: string;
  to?: string;
  page?: number;
}

@Injectable({ providedIn: 'root' })
export class SearchApi {
  private readonly http = inject(HttpClient);

  messages(filters: SearchFilters): Observable<Page<SearchResult>> {
    let params = new HttpParams().set('size', 20);
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return this.http.get<Page<SearchResult>>(`${environment.apiUrl}/search/messages`, { params });
  }
}

@Injectable({ providedIn: 'root' })
export class UserApi {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/users`;

  get(id: string): Observable<UserSummary> {
    return this.http.get<UserSummary>(`${this.api}/${id}`);
  }

  updateProfile(changes: { displayName?: string; avatarUrl?: string }): Observable<User> {
    return this.http.patch<User>(`${this.api}/me`, changes);
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.patch<void>(`${this.api}/me/password`, { currentPassword, newPassword });
  }

  resendVerification(): Observable<void> {
    return this.http.post<void>(`${this.api}/me/verification-email`, {});
  }
}
