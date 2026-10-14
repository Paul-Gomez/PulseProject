import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuditLog, Ban, Member, Page, RoleName, Workspace } from '../models';

@Injectable({ providedIn: 'root' })
export class WorkspaceApi {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/workspaces`;

  list(): Observable<Workspace[]> {
    return this.http.get<Workspace[]>(this.api);
  }

  get(id: string): Observable<Workspace> {
    return this.http.get<Workspace>(`${this.api}/${id}`);
  }

  create(name: string, description: string | null): Observable<Workspace> {
    return this.http.post<Workspace>(this.api, { name, description });
  }

  update(id: string, changes: { name?: string; description?: string }): Observable<Workspace> {
    return this.http.patch<Workspace>(`${this.api}/${id}`, changes);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  members(id: string, page = 0, size = 100): Observable<Page<Member>> {
    return this.http.get<Page<Member>>(`${this.api}/${id}/members`, { params: { page, size } });
  }

  invite(id: string, email: string): Observable<Member> {
    return this.http.post<Member>(`${this.api}/${id}/members`, { email });
  }

  changeRole(id: string, userId: string, roleName: RoleName): Observable<Member> {
    return this.http.patch<Member>(`${this.api}/${id}/members/${userId}/role`, { roleName });
  }

  removeMember(id: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}/members/${userId}`);
  }

  bans(id: string): Observable<Page<Ban>> {
    return this.http.get<Page<Ban>>(`${this.api}/${id}/bans`);
  }

  ban(id: string, userId: string, reason: string | null): Observable<Ban> {
    return this.http.post<Ban>(`${this.api}/${id}/bans/${userId}`, { reason });
  }

  unban(id: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}/bans/${userId}`);
  }

  auditLogs(id: string, page = 0, action?: string): Observable<Page<AuditLog>> {
    let params = new HttpParams().set('page', page).set('size', 20);
    if (action) {
      params = params.set('action', action);
    }
    return this.http.get<Page<AuditLog>>(`${this.api}/${id}/audit-logs`, { params });
  }

  onlineUsers(id: string): Observable<{ onlineUserIds: string[] }> {
    return this.http.get<{ onlineUserIds: string[] }>(`${this.api}/${id}/presence`);
  }
}
