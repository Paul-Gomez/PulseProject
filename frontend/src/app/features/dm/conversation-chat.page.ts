import { DatePipe } from '@angular/common';
import { Component, effect, ElementRef, inject, input, OnDestroy, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { ConversationApi } from '../../core/api/conversation.api';
import { UserApi } from '../../core/api/misc.api';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/errors';
import { DirectMessage } from '../../core/models';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { IconComponent } from '../../shared/icon.component';
import { ComposerComponent, ComposerSubmit } from '../chat/composer.component';

@Component({
  selector: 'app-conversation-chat',
  imports: [ComposerComponent, AvatarComponent, DatePipe, RouterLink, IconComponent],
  template: `
    <header class="chat-header">
      <a class="btn btn-ghost" routerLink="/dm" aria-label="Volver a las conversaciones"><app-icon name="arrow-left" [size]="20" /></a>
      <h2>Conversación</h2>
    </header>

    @if (error()) {
      <div class="alert alert-error banner" role="alert">{{ error() }}</div>
    }

    <div class="scroller" #scroller>
      @for (message of messages(); track message.id) {
        <div class="message" [class.mine]="message.authorId === myId()">
          <app-avatar [name]="name(message.authorId)" [size]="32" />
          <div class="body">
            <header>
              <strong>{{ name(message.authorId) }}</strong>
              <time class="muted">{{ message.createdAt | date: 'short' }}</time>
            </header>
            <p>{{ message.content }}</p>
          </div>
          @if (message.authorId === myId()) {
            <button class="btn btn-ghost delete" (click)="remove(message)" title="Borrar" aria-label="Borrar mensaje"><app-icon name="trash" [size]="16" /></button>
          }
        </div>
      } @empty {
        <div class="empty-state"><div class="icon"><app-icon name="message" [size]="40" /></div><p>Saluda para empezar la conversación.</p></div>
      }
    </div>

    <app-composer placeholder="Escribe un mensaje privado..." [allowFiles]="false" (send)="send($event)" />
  `,
  styles: `
    :host { display: flex; flex-direction: column; height: 100%; min-height: 0; }
    .chat-header { display: flex; align-items: center; gap: 0.5rem; padding: 0.6rem 1rem;
      border-bottom: 1px solid var(--border); background: var(--surface); }
    .chat-header h2 { margin: 0; font-size: 1.05rem; }
    .banner { margin: 0.5rem 1rem 0; }
    .scroller { flex: 1; min-height: 0; overflow-y: auto; padding: 0.5rem 0; }
    .message { display: flex; gap: 0.75rem; padding: 0.5rem 1rem; }
    .message:hover { background: var(--surface-alt); }
    .message:hover .delete, .message:focus-within .delete { visibility: visible; }
    .body { flex: 1; min-width: 0; }
    .body header { display: flex; gap: 0.5rem; align-items: baseline; }
    time { font-size: 0.75rem; }
    p { margin: 0.15rem 0 0; white-space: pre-wrap; word-break: break-word; }
    .delete { visibility: hidden; }
  `,
})
export class ConversationChatPage implements OnDestroy {
  private readonly api = inject(ConversationApi);
  private readonly userApi = inject(UserApi);
  private readonly realtime = inject(RealtimeService);
  private readonly auth = inject(AuthService);

  readonly conversationId = input.required<string>();
  private readonly scroller = viewChild<ElementRef<HTMLElement>>('scroller');
  private subscription: Subscription | null = null;

  protected readonly messages = signal<DirectMessage[]>([]);
  protected readonly error = signal<string | null>(null);
  private readonly names = signal<Map<string, string>>(new Map());

  protected myId = () => this.auth.currentUser()?.id ?? '';

  constructor() {
    effect(() => this.load(this.conversationId()));
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  name(userId: string): string {
    return this.names().get(userId) ?? '...';
  }

  send({ content }: ComposerSubmit): void {
    this.api.send(this.conversationId(), content).subscribe({
      next: (message) => this.upsert(message),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  remove(message: DirectMessage): void {
    if (confirm('¿Borrar este mensaje?')) {
      this.api.deleteMessage(message.id).subscribe(() =>
        this.messages.update((list) => list.filter((m) => m.id !== message.id)),
      );
    }
  }

  private load(conversationId: string): void {
    this.subscription?.unsubscribe();
    this.api.messages(conversationId).subscribe({
      next: (page) => {
        this.messages.set([...page.content].reverse());
        page.content.forEach((m) => this.loadName(m.authorId));
        this.scrollToBottom();
        this.api.markAsRead(conversationId).subscribe();
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
    this.subscription = this.realtime.watch<unknown>(`/topic/conversation.${conversationId}`).subscribe((event) => {
      if (event.type === 'DIRECT_MESSAGE_CREATED') {
        const message = event.payload as DirectMessage;
        this.loadName(message.authorId);
        this.upsert(message);
        this.api.markAsRead(conversationId).subscribe();
      } else if (event.type === 'DIRECT_MESSAGE_DELETED') {
        const { messageId } = event.payload as { messageId: string };
        this.messages.update((list) => list.filter((m) => m.id !== messageId));
      }
    });
  }

  private upsert(message: DirectMessage): void {
    this.messages.update((list) => (list.some((m) => m.id === message.id) ? list : [...list, message]));
    this.scrollToBottom();
  }

  private loadName(userId: string): void {
    if (!this.names().has(userId)) {
      this.names.update((names) => new Map(names).set(userId, '...'));
      this.userApi
        .get(userId)
        .subscribe((user) => this.names.update((names) => new Map(names).set(user.id, user.displayName)));
    }
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const element = this.scroller()?.nativeElement;
      if (element) element.scrollTop = element.scrollHeight;
    });
  }
}
