import { DatePipe } from '@angular/common';
import { Component, inject, input, OnDestroy, signal, viewChild } from '@angular/core';
import {
  IonBackButton, IonButton, IonButtons, IonContent, IonFooter, IonHeader, IonIcon, IonItem, IonLabel, IonList, IonTextarea,
  IonTitle, IonToolbar, ViewWillEnter,
} from '@ionic/angular';
import { addIcons } from 'ionicons';
import { send } from 'ionicons/icons';
import { Subscription } from 'rxjs';
import { ConversationApi } from '../core/api/conversation.api';
import { UserApi } from '../core/api/misc.api';
import { AuthService } from '../core/auth/auth.service';
import { errorMessage } from '../core/errors';
import { DirectMessage } from '../core/models';
import { RealtimeService } from '../core/realtime/realtime.service';

@Component({
  selector: 'app-conversation-chat',
  imports: [DatePipe, IonHeader, IonToolbar, IonButtons, IonBackButton, IonTitle, IonContent, IonList, IonItem, IonLabel,
    IonFooter, IonTextarea, IonButton, IonIcon],
  template: `
    <ion-header>
      <ion-toolbar color="primary">
        <ion-buttons slot="start"><ion-back-button defaultHref="/tabs/dm" /></ion-buttons>
        <ion-title>Conversación</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content>
      <ion-list lines="none">
        @for (message of messages(); track message.id) {
          <ion-item [class.mine]="message.authorId === myId()">
            <ion-label class="ion-text-wrap">
              <h3>{{ names().get(message.authorId) ?? '...' }} <small>{{ message.createdAt | date: 'shortTime' }}</small></h3>
              <p class="text">{{ message.content }}</p>
            </ion-label>
          </ion-item>
        }
      </ion-list>
    </ion-content>
    <ion-footer>
      @if (error()) {
        <div class="error">{{ error() }}</div>
      }
      <ion-toolbar>
        <ion-textarea [value]="text()" (ionInput)="text.set($event.detail.value ?? '')" placeholder="Mensaje privado"
                      autoGrow rows="1" maxlength="4000" />
        <ion-buttons slot="end">
          <ion-button (click)="sendMessage()" [disabled]="!text().trim()"><ion-icon slot="icon-only" name="send" /></ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-footer>
  `,
  styles: `
    .text { white-space: pre-wrap; color: var(--ion-text-color); }
    small { color: var(--ion-color-medium); font-weight: normal; }
    .mine { --background: rgba(var(--ion-color-primary-rgb), 0.08); }
    .error { color: var(--ion-color-danger); font-size: 0.8rem; padding: 2px 16px; }
    ion-textarea { padding-left: 12px; }
  `,
})
export class ConversationChatPage implements ViewWillEnter, OnDestroy {
  private readonly api = inject(ConversationApi);
  private readonly userApi = inject(UserApi);
  private readonly realtime = inject(RealtimeService);
  private readonly auth = inject(AuthService);
  private readonly content = viewChild(IonContent);
  private subscription: Subscription | null = null;

  readonly conversationId = input.required<string>();
  protected readonly messages = signal<DirectMessage[]>([]);
  protected readonly names = signal<Map<string, string>>(new Map());
  protected readonly text = signal('');
  protected readonly error = signal<string | null>(null);

  constructor() {
    addIcons({ send });
  }

  protected myId = () => this.auth.currentUser()?.id;

  ionViewWillEnter(): void {
    this.api.messages(this.conversationId()).subscribe((page) => {
      this.messages.set([...page.content].reverse());
      page.content.forEach((m) => this.loadName(m.authorId));
      this.scrollToBottom();
      this.api.markAsRead(this.conversationId()).subscribe();
    });
    this.subscription?.unsubscribe();
    this.subscription = this.realtime
      .watch<DirectMessage>(`/topic/conversation.${this.conversationId()}`)
      .subscribe((event) => {
        if (event.type === 'DIRECT_MESSAGE_CREATED') {
          this.loadName(event.payload.authorId);
          this.upsert(event.payload);
          this.api.markAsRead(this.conversationId()).subscribe();
        }
      });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  sendMessage(): void {
    const content = this.text().trim();
    if (!content) return;
    this.text.set('');
    this.api.send(this.conversationId(), content).subscribe({
      next: (message) => this.upsert(message),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  private upsert(message: DirectMessage): void {
    this.messages.update((list) => (list.some((m) => m.id === message.id) ? list : [...list, message]));
    this.scrollToBottom();
  }

  private loadName(userId: string): void {
    if (this.names().has(userId)) return;
    this.names.update((names) => new Map(names).set(userId, '...'));
    this.userApi.get(userId).subscribe((user) => this.names.update((names) => new Map(names).set(user.id, user.displayName)));
  }

  private scrollToBottom(): void {
    setTimeout(() => this.content()?.scrollToBottom(200));
  }
}
