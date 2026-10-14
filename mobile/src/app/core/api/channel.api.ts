import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Attachment, Channel, ChannelType, Message, Page } from '../models';

@Injectable({ providedIn: 'root' })
export class ChannelApi {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  list(workspaceId: string): Observable<Channel[]> {
    return this.http.get<Channel[]>(`${this.api}/workspaces/${workspaceId}/channels`);
  }

  get(channelId: string): Observable<Channel> {
    return this.http.get<Channel>(`${this.api}/channels/${channelId}`);
  }

  create(workspaceId: string, name: string, type: ChannelType, isPrivate: boolean): Observable<Channel> {
    return this.http.post<Channel>(`${this.api}/workspaces/${workspaceId}/channels`, { name, type, isPrivate });
  }

  archive(channelId: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/channels/${channelId}`);
  }

  addMember(channelId: string, userId: string): Observable<void> {
    return this.http.post<void>(`${this.api}/channels/${channelId}/members/${userId}`, {});
  }

  messages(channelId: string, page = 0, size = 30): Observable<Page<Message>> {
    return this.http.get<Page<Message>>(`${this.api}/channels/${channelId}/messages`, { params: { page, size } });
  }

  send(channelId: string, content: string, parentMessageId?: string | null, attachmentIds: string[] = []): Observable<Message> {
    return this.http.post<Message>(`${this.api}/channels/${channelId}/messages`, { content, parentMessageId, attachmentIds });
  }

  edit(messageId: string, content: string): Observable<Message> {
    return this.http.patch<Message>(`${this.api}/messages/${messageId}`, { content });
  }

  delete(messageId: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/messages/${messageId}`);
  }

  react(messageId: string, emoji: string): Observable<void> {
    return this.http.post<void>(`${this.api}/messages/${messageId}/reactions`, { emoji });
  }

  unreact(messageId: string, emoji: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/messages/${messageId}/reactions/${encodeURIComponent(emoji)}`);
  }

  upload(channelId: string, file: File): Observable<Attachment> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Attachment>(`${this.api}/channels/${channelId}/attachments`, form);
  }

  /** Downloads go through HttpClient (not a plain link) so the access token is sent and checked. */
  download(attachmentId: string): Observable<Blob> {
    return this.http.get(`${this.api}/attachments/${attachmentId}/download`, { responseType: 'blob' });
  }
}
