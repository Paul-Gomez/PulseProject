import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Conversation, DirectMessage, Page } from '../models';

@Injectable({ providedIn: 'root' })
export class ConversationApi {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  list(): Observable<Page<Conversation>> {
    return this.http.get<Page<Conversation>>(`${this.api}/conversations`, { params: { size: 50 } });
  }

  start(participantIds: string[], name?: string): Observable<Conversation> {
    return this.http.post<Conversation>(`${this.api}/conversations`, { participantIds, name });
  }

  messages(conversationId: string, page = 0, size = 30): Observable<Page<DirectMessage>> {
    return this.http.get<Page<DirectMessage>>(`${this.api}/conversations/${conversationId}/messages`, {
      params: { page, size },
    });
  }

  send(conversationId: string, content: string): Observable<DirectMessage> {
    return this.http.post<DirectMessage>(`${this.api}/conversations/${conversationId}/messages`, { content });
  }

  markAsRead(conversationId: string): Observable<void> {
    return this.http.patch<void>(`${this.api}/conversations/${conversationId}/read`, {});
  }

  deleteMessage(messageId: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/direct-messages/${messageId}`);
  }
}
